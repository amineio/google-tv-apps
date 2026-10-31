package com.amine.asktv.phoneauth

import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

interface PhoneLoopbackListener : AutoCloseable {
    fun start()
    fun expectState(value: String)
}

/** Phone-owned HTTP loopback only. The owner binds this before claiming the TV attempt. */
class PhoneLoopbackServer internal constructor(
    private val onCallback: (String) -> Unit,
    private val onFailure: (String) -> Unit,
    private val timeouts: RelayTimeouts,
    private val portOverride: Int?,
) : PhoneLoopbackListener {
    constructor(onCallback: (String) -> Unit, onFailure: (String) -> Unit, timeouts: RelayTimeouts = RelayTimeouts()) :
        this(onCallback, onFailure, timeouts, null)
    // Allows local JVM/native tests to choose an ephemeral port; no address override exists.
    internal constructor(onCallback: (String) -> Unit, onFailure: (String) -> Unit, portOverride: Int) :
        this(onCallback, onFailure, RelayTimeouts(), portOverride)
    private val closed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val stateLock = Any()
    @Volatile private var listener: ServerSocket? = null
    @Volatile private var expectedState: String? = null
    private val workers = RelaySocketWorkers(timeouts, ::handle)
    private val lifetime = relayTimer("AIChat-loopback-lifetime")
    internal val localPort: Int get() = listener?.localPort ?: -1
    init { require(portOverride == null || portOverride in 0..65535) }
    override fun start() {
        check(started.compareAndSet(false, true) && !closed.get()) { "Phone listener cannot be restarted." }
        val socket = ServerSocket()
        try {
            socket.reuseAddress = false
            socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), portOverride ?: 1455), 2)
            synchronized(stateLock) {
                if (closed.get()) throw IOException("Phone listener stopped.")
                listener = socket
            }
            lifetime.schedule({
                if (!closed.get()) {
                    close()
                    onFailure("Sign-in expired. Continue with ChatGPT to try again.")
                }
            }, 600_000, TimeUnit.MILLISECONDS)
            Thread({
                try { while (!closed.get()) workers.accept(socket.accept()) }
                catch (_: Exception) {
                    if (!closed.get()) { close(); onFailure("The phone listener stopped. Please try again.") }
                }
            }, "AIChat-phone-loopback").apply { isDaemon = true; start() }
        } catch (_: Exception) {
            runCatching { socket.close() }; close()
            throw IOException("The phone callback port is unavailable. Close other sign-in helpers and try again.")
        }
    }
    override fun expectState(value: String) {
        require(value.isNotEmpty() && value.length <= 128 && value.all { it in '!'..'~' })
        synchronized(stateLock) {
            check(!closed.get()) { "Phone listener stopped." }
            expectedState = value
        }
    }
    private fun handle(socket: Socket) {
        try {
            val target = RelayHttp.readCallback(socket.inputStream)
            val values = try { AuthCallbackParser.parseTarget(target) } catch (_: IllegalArgumentException) { throw RelayRejected(400) }
            val expected = expectedState ?: throw RelayRejected(400)
            val actual = values["state"] ?: throw RelayRejected(400)
            if (!MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))) throw RelayRejected(400)
            if (closed.get()) return
            // The owner's relay delivery is independent of a subsequent browser write reset.
            try { onCallback(target) }
            catch (_: Exception) {
                onFailure("Authorization could not be sent. Check AI Chat on your TV.")
                runCatching { page(socket, 400, "Authorization could not be sent. Check AI Chat on your TV.") }
                return
            }
            runCatching { page(socket, 200, "Authorization received on this phone. Return to AI Chat to check delivery.") }
        } catch (failure: RelayRejected) {
            runCatching { page(socket, failure.status, "This callback could not be accepted. Check AI Chat.") }
        } catch (_: SocketTimeoutException) {
            // Quiet preconnection or incomplete-request timeout; keep the attempt active.
        } catch (_: EOFException) {
        } catch (_: IOException) {
        }
    }
    private fun page(socket: Socket, status: Int, text: String) =
        RelayHttp.writeResponse(socket.outputStream, status, text.toByteArray(Charsets.UTF_8), "text/plain; charset=utf-8")
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        val socket = synchronized(stateLock) { listener.also { listener = null; expectedState = null } }
        runCatching { socket?.close() }; workers.close(); lifetime.shutdownNow()
    }
}
