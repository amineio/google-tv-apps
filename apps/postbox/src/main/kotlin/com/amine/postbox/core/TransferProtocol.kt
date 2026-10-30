package com.amine.postbox.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal class ProtocolFailure(val status: Int = 400) : Exception("Transfer rejected.")

internal data class HttpRequest(val path: String, val authorization: String?, val body: ByteArray)

internal data class HttpResponse(val status: Int, val body: ByteArray)

internal object BoundedHttp {
    fun readRequest(input: InputStream): HttpRequest {
        val line = line(input, 2048)
        val fields = line.split(' ')
        if (
            fields.size != 3 ||
                fields[0] != "POST" ||
                fields[1] !in listOf("/pair", "/captures") ||
                fields[2] != "HTTP/1.1"
        )
            throw ProtocolFailure()
        val headers = headers(input)
        val limit = if (fields[1] == "/pair") 4096 else MAX_CAPTURE_BYTES
        val size = contentLength(headers, limit)
        return HttpRequest(fields[1], headers["authorization"], body(input, size))
    }

    fun readResponse(input: InputStream): HttpResponse {
        val fields = line(input, 2048).split(' ')
        if (fields.size < 2 || fields[0] != "HTTP/1.1") throw ProtocolFailure()
        val status = fields[1].toIntOrNull() ?: throw ProtocolFailure()
        return HttpResponse(status, body(input, contentLength(headers(input), 4096)))
    }

    fun writeResponse(output: OutputStream, status: Int, body: ByteArray) {
        output.write(
            "HTTP/1.1 $status Result\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                .toByteArray(Charsets.US_ASCII)
        )
        output.write(body)
        output.flush()
    }

    private fun headers(input: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var total = 0
        while (true) {
            val line = line(input, 8192 - total)
            total += line.length + 2
            if (total > 8192) throw ProtocolFailure()
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon <= 0) throw ProtocolFailure()
            val key = line.substring(0, colon).lowercase()
            if (!key.matches(Regex("[a-z0-9-]+"))) throw ProtocolFailure()
            val value = line.substring(colon + 1).trim()
            if (key in listOf("content-length", "authorization") && result.containsKey(key))
                throw ProtocolFailure()
            if (key in listOf("transfer-encoding", "expect")) throw ProtocolFailure()
            result[key] = value
        }
        return result
    }

    private fun contentLength(headers: Map<String, String>, maximum: Int): Int {
        val value = headers["content-length"] ?: throw ProtocolFailure()
        if (!value.matches(Regex("[0-9]{1,10}"))) throw ProtocolFailure()
        val size = value.toLongOrNull() ?: throw ProtocolFailure()
        if (size > maximum) throw ProtocolFailure(413)
        return size.toInt()
    }

    private fun body(input: InputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(bytes, offset, size - offset)
            if (read <= 0) throw ProtocolFailure()
            offset += read
        }
        return bytes
    }

    private fun line(input: InputStream, maximum: Int): String {
        val bytes = ByteArrayOutputStream()
        while (true) {
            val next = input.read()
            if (next == 13) {
                if (input.read() != 10) throw ProtocolFailure()
                break
            }
            if (next !in 32..126 || bytes.size() >= maximum) throw ProtocolFailure()
            bytes.write(next)
        }
        return bytes.toString("US-ASCII")
    }
}

internal class TransferProtocol(
    private val store: PostboxStore,
    private val monotonicMs: () -> Long,
    private val persistPhoneToken: (String?) -> Unit,
    private val onReceived: () -> Unit,
    initialPhoneToken: String? = null,
) {
    private var phoneToken: String? = initialPhoneToken
    private var temporaryToken: String? = null
    private var deadline = Long.MIN_VALUE

    @Synchronized
    fun open(durationMs: Long): String {
        deadline = monotonicMs() + durationMs
        return randomToken().also { temporaryToken = it }
    }

    @Synchronized
    fun close() {
        temporaryToken = null
        deadline = Long.MIN_VALUE
    }

    @Synchronized
    fun pair(token: String): String {
        ensureOpen()
        if (!matches(token, temporaryToken)) throw ProtocolFailure(403)
        val replacement = randomToken()
        persistPhoneToken(replacement)
        phoneToken = replacement
        temporaryToken = null
        return replacement
    }

    @Synchronized
    fun authorized(token: String): Boolean = monotonicMs() < deadline && matches(token, phoneToken)

    @Synchronized
    fun forget() {
        persistPhoneToken(null)
        phoneToken = null
    }

    @Synchronized
    fun save(token: String, body: ByteArray): SaveResult {
        ensureOpen()
        if (!authorized(token)) throw ProtocolFailure(403)
        val capture =
            try {
                CaptureCodec().decode(body)
            } catch (_: IllegalArgumentException) {
                throw ProtocolFailure()
            }
        ensureOpen()
        val result = store.save(capture)
        // The durable result remains idempotent if expiry closes the response socket.
        if (result == SaveResult.ADDED) onReceived()
        return result
    }

    private fun ensureOpen() {
        if (monotonicMs() >= deadline) throw ProtocolFailure(503)
    }

    private fun matches(candidate: String, expected: String?): Boolean {
        if (expected == null || !candidate.matches(Regex("[A-Za-z0-9_-]{43}"))) return false
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.US_ASCII),
            expected.toByteArray(Charsets.US_ASCII),
        )
    }

    companion object {
        fun randomToken(): String =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
    }
}

internal data class TransferTimeouts(
    val connectMs: Int = 5000,
    val handshakeMs: Long = 5000,
    val readMs: Int = 5000,
    val totalMs: Long = 30000,
) {
    init {
        require(connectMs > 0 && handshakeMs > 0 && readMs > 0 && totalMs > 0)
    }
}

/** Owns one socket's elapsed budget, including registration after the timer has expired. */
internal class SocketDeadline(timeoutMs: Long) : AutoCloseable {
    private val started = System.nanoTime()
    private val budgetNanos = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMs)
    private var expired = false
    private var socket: java.net.Socket? = null
    private val scheduler =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "Postbox-deadline").apply { isDaemon = true }
        }
    private val timer =
        scheduler.schedule({ expire() }, timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)

    init {
        require(timeoutMs > 0)
    }

    @Synchronized
    fun register(candidate: java.net.Socket) {
        if (expired || elapsed()) {
            try {
                candidate.close()
            } catch (_: Exception) {}
            expire()
            throw java.net.SocketTimeoutException("Transfer deadline expired.")
        }
        check(socket == null || socket === candidate)
        socket = candidate
    }

    @Synchronized
    fun check() {
        if (expired || elapsed()) {
            expire()
            throw java.net.SocketTimeoutException("Transfer deadline expired.")
        }
    }

    private fun elapsed() = System.nanoTime() - started >= budgetNanos

    @Synchronized
    private fun expire() {
        expired = true
        try {
            socket?.close()
        } catch (_: Exception) {}
    }

    override fun close() {
        timer.cancel(false)
        scheduler.shutdownNow()
    }
}
