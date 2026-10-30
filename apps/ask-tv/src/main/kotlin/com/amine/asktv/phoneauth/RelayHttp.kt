package com.amine.asktv.phoneauth

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Blocking transports must be constructed/started/called by a background owner. */
data class RelayTimeouts(
    val connectMs: Int = 5_000,
    val handshakeMs: Long = 5_000,
    val readMs: Int = 5_000,
    val totalMs: Long = 30_000,
) {
    init { require(connectMs > 0 && handshakeMs > 0 && readMs > 0 && totalMs > 0) }
}

internal data class RelayRequest(val path: String, val authorization: String?, val body: ByteArray)
internal data class RelayResponse(val status: Int, val body: ByteArray)

/** All byte limits are applied before decoding, allocating the body, or entering OAuth. */
internal object RelayHttp {
    const val MAX_BODY = 16_384
    private const val MAX_LINE = 8_192
    private const val MAX_HEADERS = 16_384

    fun readRequest(input: InputStream): RelayRequest {
        val fields = line(input, MAX_LINE).split(' ')
        if (fields.size != 3 || fields[2] != "HTTP/1.1") reject()
        if (fields[0] != "POST") reject(405)
        if (fields[1] !in setOf("/claim", "/callback", "/status", "/cancel")) reject()
        val headers = headers(input)
        val size = contentLength(headers, required = true)
        if (fields[1] in setOf("/status", "/cancel") && size != 0) reject()
        val body = body(input, size)
        noPipeline(input)
        return RelayRequest(fields[1], headers["authorization"], body)
    }

    fun readCallback(input: InputStream): String {
        val fields = line(input, MAX_LINE).split(' ')
        if (fields.size != 3 || fields[2] != "HTTP/1.1") reject()
        if (fields[0] != "GET") reject(405)
        if (fields[1].length > AuthCallbackParser.MAX_TARGET_BYTES) reject()
        val headers = headers(input)
        if (contentLength(headers, required = false) != 0) reject()
        noPipeline(input)
        return fields[1]
    }

    fun form(request: RelayRequest, name: String): String = safe {
        val fields = strictForm(ascii(request.body))
        require(fields.keys == setOf(name))
        fields.getValue(name)
    }

    fun readResponse(input: InputStream): RelayResponse {
        val fields = line(input, MAX_LINE).split(' ')
        if (fields.size < 3 || fields[0] != "HTTP/1.1" || !fields[1].matches(Regex("[1-5][0-9]{2}"))) reject()
        val bytes = body(input, contentLength(headers(input), required = true))
        noPipeline(input)
        return RelayResponse(fields[1].toInt(), bytes)
    }

    fun writeResponse(output: OutputStream, status: Int, body: ByteArray, type: String = "application/json") {
        if (body.size > MAX_BODY) reject(413)
        output.write(("HTTP/1.1 $status Result\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    fun writeRequest(output: OutputStream, path: String, authorization: String?, body: ByteArray) {
        if (body.size > MAX_BODY) reject(413)
        val auth = authorization?.let { "Authorization: Bearer $it\r\n" }.orEmpty()
        output.write(("POST $path HTTP/1.1\r\nHost: relay\r\nContent-Type: application/x-www-form-urlencoded\r\n" +
            "Content-Length: ${body.size}\r\n${auth}Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    fun claimBody(claim: RelayClaim): ByteArray =
        ("{\"relay_token\":${quote(claim.relayToken)},\"authorization_url\":${quote(claim.authorizationUrl)},\"expires_in_ms\":${claim.expiresInMs}}").toByteArray()
            .also { if (it.size > MAX_BODY) reject(413) }
    fun stateBody(state: RelayState): ByteArray = "{\"state\":\"${state.wireValue}\"}".toByteArray()
    fun receiptBody(): ByteArray = "{\"receipt\":\"accepted\"}".toByteArray()
    fun decodeClaim(body: ByteArray): RelayClaim = safe {
        val values = jsonObject(body)
        require(values.keys == setOf("relay_token", "authorization_url", "expires_in_ms"))
        val token = string(values.getValue("relay_token"))
        require(token.matches(Regex("[A-Za-z0-9_-]{43}")))
        val url = string(values.getValue("authorization_url"))
        require(url.length <= MAX_BODY)
        val lifetime = values.getValue("expires_in_ms").also { require(it.matches(Regex("[1-9][0-9]{0,5}"))) }.toLong()
        require(lifetime in 1..600_000)
        RelayClaim(token, url, lifetime)
    }
    fun decodeState(body: ByteArray): RelayState = safe {
        val values = jsonObject(body)
        require(values.keys == setOf("state"))
        RelayState.entries.single { it.wireValue == string(values.getValue("state")) }
    }
    fun decodeReceipt(body: ByteArray): RelayReceipt = safe {
        val values = jsonObject(body)
        require(values.keys == setOf("receipt") && string(values.getValue("receipt")) == "accepted")
        RelayReceipt("accepted")
    }

    private fun headers(input: InputStream): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var total = 0
        while (true) {
            val value = line(input, MAX_HEADERS - total)
            total += value.length + 2
            if (total > MAX_HEADERS) reject()
            if (value.isEmpty()) return result
            val colon = value.indexOf(':')
            if (colon <= 0) reject()
            val name = value.substring(0, colon).lowercase(java.util.Locale.ROOT)
            if (!name.matches(Regex("[a-z0-9-]+")) || result.containsKey(name) || name in setOf("transfer-encoding", "expect")) reject()
            result[name] = value.substring(colon + 1).trim(' ')
        }
    }
    private fun contentLength(headers: Map<String, String>, required: Boolean): Int {
        val value = headers["content-length"] ?: if (required) reject() else return 0
        if (!value.matches(Regex("[0-9]{1,10}"))) reject()
        val size = value.toLongOrNull() ?: reject()
        if (size > MAX_BODY) reject(413)
        return size.toInt()
    }
    private fun body(input: InputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val count = input.read(bytes, offset, size - offset)
            if (count <= 0) throw EOFException("Incomplete relay request.")
            offset += count
        }
        return bytes
    }
    private fun noPipeline(input: InputStream) { if (input.available() > 0) reject() }
    private fun line(input: InputStream, maximum: Int): String {
        val bytes = ByteArrayOutputStream(minOf(128, maximum.coerceAtLeast(0)))
        while (true) {
            val value = input.read()
            if (value == -1) throw EOFException("Incomplete relay request.")
            if (value == 13) {
                if (input.read() != 10) reject()
                return bytes.toString("US-ASCII")
            }
            if (value !in 32..126 || bytes.size() >= maximum) reject()
            bytes.write(value)
        }
    }
    private fun ascii(bytes: ByteArray): String {
        if (bytes.any { (it.toInt() and 255) !in 32..126 }) reject()
        return bytes.toString(Charsets.US_ASCII)
    }
    private fun quote(value: String) = Json.encodeToString(value)
    private fun string(value: String): String { require(value.startsWith('"')); return Json.decodeFromString<String>(value) }

    /** Scan only a flat fixed-schema JSON object. Detect duplicate keys before JSON decoding. */
    private fun jsonObject(body: ByteArray): Map<String, String> {
        require(body.size <= MAX_BODY)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body)).toString()
        var position = 0
        fun whitespace() { while (position < text.length && text[position] in " \t\r\n") position++ }
        fun take(char: Char) { whitespace(); require(position < text.length && text[position++] == char) }
        fun quoted(): String {
            whitespace()
            val start = position
            require(position < text.length && text[position++] == '"')
            while (position < text.length) {
                when (text[position++]) {
                    '\\' -> { require(position < text.length); position++ }
                    '"' -> return text.substring(start, position)
                }
            }
            throw IllegalArgumentException()
        }
        take('{')
        val values = linkedMapOf<String, String>()
        whitespace()
        if (position < text.length && text[position] != '}') while (true) {
            val key = string(quoted())
            require(!values.containsKey(key))
            take(':'); whitespace()
            val value = if (position < text.length && text[position] == '"') quoted() else {
                val start = position
                while (position < text.length && text[position] in '0'..'9') position++
                require(position > start)
                text.substring(start, position)
            }
            values[key] = value
            whitespace()
            require(position < text.length)
            if (text[position] != ',') break
            position++
        }
        take('}'); whitespace(); require(position == text.length)
        return values
    }
    private inline fun <T> safe(action: () -> T): T = try { action() } catch (_: Exception) { reject() }
    private fun reject(status: Int = 400): Nothing = throw RelayRejected(status)
}

internal fun relayTimer(name: String) = ScheduledThreadPoolExecutor(1) { task ->
    Thread(task, name).apply { isDaemon = true }
}.apply { removeOnCancelPolicy = true; executeExistingDelayedTasksAfterShutdownPolicy = false }

/** At most four accepted sockets, including two queued sockets. The timer bounds output too. */
internal class RelaySocketWorkers(private val timeouts: RelayTimeouts, private val handle: (Socket) -> Unit) : AutoCloseable {
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    private val timer = relayTimer("AIChat-socket-deadline")
    private val pool = ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(2),
        { task -> Thread(task, "AIChat-relay-worker").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
    private var closed = false

    @Synchronized fun accept(socket: Socket) {
        if (closed || sockets.size >= 4) { runCatching { socket.close() }; return }
        sockets.add(socket)
        try {
            val deadline = timer.schedule({ runCatching { socket.close() } }, timeouts.totalMs, TimeUnit.MILLISECONDS)
            try {
                pool.execute {
                    try { if (!socket.isClosed) { socket.soTimeout = timeouts.readMs; handle(socket) } }
                    catch (_: Exception) { /* A broken peer cannot stop the listener. */ }
                    finally { deadline.cancel(false); sockets.remove(socket); runCatching { socket.close() } }
                }
            } catch (_: RejectedExecutionException) {
                deadline.cancel(false); sockets.remove(socket); runCatching { socket.close() }
            }
        } catch (_: RejectedExecutionException) { sockets.remove(socket); runCatching { socket.close() } }
    }
    @Synchronized override fun close() {
        if (closed) return
        closed = true
        sockets.forEach { runCatching { it.close() } }; sockets.clear()
        pool.shutdownNow(); timer.shutdownNow()
    }
}
