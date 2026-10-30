package com.amine.asktv.phoneauth

import android.content.Context
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/** One listener owns exactly one protocol attempt. Start and close on a background thread. */
class PhoneRelayServer internal constructor(
    private val context: Context,
    private val protocol: PhoneRelayProtocol,
    private val timeouts: RelayTimeouts,
    private val bindHostOverride: String?,
    private val durationMs: Long = 600_000,
    private val monotonicMs: () -> Long = { System.nanoTime() / 1_000_000 },
) : AutoCloseable {
    constructor(context: Context, protocol: PhoneRelayProtocol, timeouts: RelayTimeouts = RelayTimeouts()) :
        this(context.applicationContext, protocol, timeouts, null)
    private val stateLock = Any()
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val timer = relayTimer("AIChat-relay-lifetime")
    private val handshakeTimer = relayTimer("AIChat-relay-handshake")
    @Volatile private var listener: SSLServerSocket? = null
    @Volatile private var workers: RelaySocketWorkers? = null
    @Volatile private var claimedToken: String? = null
    init {
        require(bindHostOverride == null || bindHostOverride == "127.0.0.1")
        require(durationMs in 1..600_000)
    }

    fun start(): PhonePairingLink {
        check(started.compareAndSet(false, true) && !closed.get()) { "Relay cannot be restarted." }
        var candidate: SSLServerSocket? = null
        try {
            // The test-only bind override never changes the production link validation.
            val host = if (bindHostOverride != null) "192.168.1.2" else lanHost()
            val identity = RelayTlsIdentity(context)
            val socket = identity.serverFactory().createServerSocket() as SSLServerSocket
            candidate = socket
            socket.reuseAddress = false
            socket.bind(InetSocketAddress(InetAddress.getByName(bindHostOverride ?: host), 0), 2)
            val pool = RelaySocketWorkers(timeouts) { serve(it as SSLSocket) }
            synchronized(stateLock) {
                if (closed.get()) { pool.close(); throw IOException("Relay stopped.") }
                listener = socket; workers = pool
            }
            val began = monotonicMs()
            // OAuth begin is outside the transport lock. The protocol protects a close/start race.
            val bootstrap = protocol.open(durationMs)
            val link = PhonePairingLink(host, socket.localPort, identity.pin, bootstrap)
            timer.scheduleAtFixedRate({
                val expired = monotonicMs() - began >= durationMs
                val graceEnded = claimedToken?.let { token ->
                    try { protocol.status(token); false } catch (failure: RelayRejected) { failure.status == 410 }
                } ?: false
                if (expired || graceEnded) close()
            }, 10, 10, TimeUnit.MILLISECONDS)
            if (closed.get()) throw IOException("Relay stopped.")
            Thread({ acceptLoop(socket, pool) }, "AIChat-relay-listener").apply { isDaemon = true; start() }
            return link
        } catch (_: Exception) {
            runCatching { candidate?.close() }; close()
            throw IOException("Phone sign-in is unavailable. Check the local network and try again.")
        }
    }

    private fun acceptLoop(socket: SSLServerSocket, pool: RelaySocketWorkers) {
        try { while (!closed.get()) pool.accept(socket.accept()) }
        catch (_: Exception) { if (!closed.get()) close() }
    }
    private fun serve(socket: SSLSocket) {
        try {
            val handshake = handshakeTimer.schedule({ runCatching { socket.close() } }, timeouts.handshakeMs, TimeUnit.MILLISECONDS)
            try { socket.startHandshake() } finally { handshake.cancel(false) }
            if (socket.isClosed || closed.get()) return
            val request = RelayHttp.readRequest(socket.inputStream)
            val body: ByteArray
            val status: Int
            if (request.path == "/claim") {
                if (request.authorization != null) throw RelayRejected(400)
                val claim = protocol.claim(RelayHttp.form(request, "token"))
                synchronized(stateLock) {
                    if (closed.get()) throw RelayRejected(410)
                    claimedToken = claim.relayToken
                }
                body = RelayHttp.claimBody(claim); status = 200
            } else {
                val token = bearer(request.authorization)
                when (request.path) {
                    "/callback" -> {
                        protocol.callback(token, RelayHttp.form(request, "target"))
                        body = RelayHttp.receiptBody(); status = 202
                    }
                    "/status" -> { body = RelayHttp.stateBody(protocol.status(token)); status = 200 }
                    "/cancel" -> {
                        protocol.cancel(token)
                        body = RelayHttp.stateBody(protocol.status(token)); status = 200
                    }
                    else -> throw RelayRejected(400)
                }
            }
            RelayHttp.writeResponse(socket.outputStream, status, body)
        } catch (failure: RelayRejected) {
            runCatching { RelayHttp.writeResponse(socket.outputStream, failure.status, "{}".toByteArray()) }
        } catch (_: SocketTimeoutException) {
            // Browser/socket preconnections and incomplete requests close quietly.
        } catch (_: EOFException) {
        } catch (_: IOException) {
        } catch (_: Exception) {
            runCatching { RelayHttp.writeResponse(socket.outputStream, 400, "{}".toByteArray()) }
        }
    }
    private fun bearer(value: String?): String {
        if (value == null || !value.matches(Regex("Bearer [A-Za-z0-9_-]{43}"))) throw RelayRejected(403)
        return value.removePrefix("Bearer ")
    }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        val owned = synchronized(stateLock) {
            val resources = listener to workers
            listener = null; workers = null; claimedToken = null
            resources
        }
        runCatching { owned.first?.close() }; owned.second?.close()
        timer.shutdownNow(); handshakeTimer.shutdownNow()
        // Never wait for OAuth while holding stateLock.
        protocol.close()
    }
    private fun lanHost(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: throw IOException()
        return Collections.list(interfaces).asSequence().filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .mapNotNull { it.hostAddress }.firstOrNull { PhonePairingLink.validLocalHost(it) } ?: throw IOException()
    }
}
