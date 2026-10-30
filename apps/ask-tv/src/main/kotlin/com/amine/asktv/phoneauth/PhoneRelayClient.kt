package com.amine.asktv.phoneauth

import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

interface PhoneRelayTransport : AutoCloseable {
    fun claim(): RelayClaim
    fun callback(relayToken: String, target: String): RelayReceipt
    fun status(relayToken: String): RelayState
    fun cancel(relayToken: String)
}

/** Blocking, serialized client. The owning service calls it off the main thread. */
class PhoneRelayClient internal constructor(
    private val link: PhonePairingLink,
    private val timeouts: RelayTimeouts,
    private val connectHostOverride: String?,
) : PhoneRelayTransport {
    constructor(link: PhonePairingLink, timeouts: RelayTimeouts = RelayTimeouts()) : this(link, timeouts, null)
    private val requestLock = Any()
    private val closed = AtomicBoolean(false)
    private val active = AtomicReference<SSLSocket?>()
    private val timer = relayTimer("AIChat-client-deadline")
    private var totalTimer: ScheduledFuture<*>? = null
    init {
        PhonePairingLink.parse(link.encode())
        require(connectHostOverride == null || connectHostOverride == "127.0.0.1")
    }
    override fun claim(): RelayClaim = RelayHttp.decodeClaim(request("/claim", null, form("token", link.token), 200))
    override fun callback(relayToken: String, target: String): RelayReceipt {
        if (target.length > AuthCallbackParser.MAX_TARGET_BYTES || target.toByteArray(Charsets.UTF_8).size > AuthCallbackParser.MAX_TARGET_BYTES) throw RelayRejected(400)
        return RelayHttp.decodeReceipt(request("/callback", relayToken, form("target", target), 202))
    }
    override fun status(relayToken: String): RelayState = RelayHttp.decodeState(request("/status", relayToken, ByteArray(0), 200))
    override fun cancel(relayToken: String) { RelayHttp.decodeState(request("/cancel", relayToken, ByteArray(0), 200)) }

    private fun request(path: String, token: String?, body: ByteArray, expected: Int): ByteArray = synchronized(requestLock) {
        if (token != null && !token.matches(Regex("[A-Za-z0-9_-]{43}"))) throw RelayRejected(403)
        if (body.size > RelayHttp.MAX_BODY) throw RelayRejected(413)
        try {
            openPinned().use { socket ->
                // openPinned has completed the certificate check before these application bytes.
                RelayHttp.writeRequest(socket.outputStream, path, token, body)
                val response = RelayHttp.readResponse(socket.inputStream)
                if (response.status != expected) throw RelayRejected(if (response.status in setOf(400, 403, 405, 409, 410, 413)) response.status else 400)
                response.body
            }
        } catch (rejected: RelayRejected) { throw rejected }
        catch (_: Exception) { throw IOException("The TV connection could not be completed.") }
        finally {
            totalTimer?.cancel(false); totalTimer = null
            active.getAndSet(null)?.let { runCatching { it.close() } }
        }
    }

    /** Internal real-TLS fixture entry point. Does not bypass certificate validation. */
    internal fun openPinned(): SSLSocket {
        if (closed.get()) throw IOException("The TV connection is closed.")
        val expected = link.pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) { throw CertificateException("Peer rejected.") }
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (chain.isEmpty() || !MessageDigest.isEqual(expected, MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)))
                    throw CertificateException("Peer rejected.")
                chain[0].checkValidity()
            }
        }
        val factory = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }.socketFactory
        val socket = factory.createSocket() as SSLSocket
        if (!active.compareAndSet(null, socket)) { socket.close(); throw IOException("The TV connection is busy.") }
        try {
            if (closed.get()) throw IOException("The TV connection is closed.")
            totalTimer = timer.schedule({ runCatching { socket.close() } }, timeouts.totalMs, TimeUnit.MILLISECONDS)
            socket.soTimeout = timeouts.readMs
            socket.connect(InetSocketAddress(connectHostOverride ?: link.host, link.port), timeouts.connectMs)
            val handshake = timer.schedule({ runCatching { socket.close() } }, timeouts.handshakeMs, TimeUnit.MILLISECONDS)
            try { socket.startHandshake() } finally { handshake.cancel(false) }
            if (closed.get() || socket.isClosed) throw SocketTimeoutException("The TV connection ended.")
            return socket
        } catch (failure: Exception) {
            totalTimer?.cancel(false); totalTimer = null
            active.compareAndSet(socket, null); runCatching { socket.close() }
            throw failure
        }
    }
    private fun form(name: String, value: String): ByteArray = "$name=${URLEncoder.encode(value, "UTF-8")}".toByteArray(Charsets.US_ASCII)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            active.getAndSet(null)?.let { runCatching { it.close() } }
            timer.shutdownNow()
        }
    }
}
