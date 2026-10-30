package com.amine.postbox.core

import android.content.Context
import android.os.SystemClock
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlinx.serialization.encodeToString

internal data class ReceiverOptions(
    val bindHost: String? = null,
    val advertisedHost: String? = null,
    val port: Int? = null,
    val windowMs: Long = 300_000,
)

class CaptureReceiver
internal constructor(
    context: Context,
    store: PostboxStore,
    onReceived: () -> Unit,
    private val options: ReceiverOptions,
) {
    constructor(
        context: Context,
        store: PostboxStore,
        onReceived: () -> Unit,
    ) : this(context, store, onReceived, ReceiverOptions())

    private val preferences =
        context.applicationContext.getSharedPreferences("postbox-receiver", Context.MODE_PRIVATE)
    private val identity = TlsIdentity(context)
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    private var server: SSLServerSocket? = null
    private var workers: ThreadPoolExecutor? = null
    private var timers: java.util.concurrent.ScheduledExecutorService? = null
    private val protocol =
        TransferProtocol(
            store,
            { SystemClock.elapsedRealtime() },
            { token ->
                if (
                    !preferences
                        .edit()
                        .apply {
                            if (token == null) remove("phone-token")
                            else putString("phone-token", token)
                        }
                        .commit()
                )
                    throw IOException("Receiver storage unavailable.")
            },
            onReceived,
            preferences.getString("phone-token", null),
        )

    @Synchronized
    fun start(): ReceiverSession {
        close()
        try {
            val host = options.advertisedHost ?: lanHost()
            if (!PairingLink.validLocalHost(host)) throw IOException()
            val preferredPort = options.port ?: preferences.getInt("port", 18765)
            val listener = identity.serverFactory().createServerSocket() as SSLServerSocket
            server = listener
            listener.reuseAddress = true
            listener.bind(java.net.InetSocketAddress(options.bindHost ?: host, preferredPort), 2)
            if (
                options.port == null &&
                    !preferences.edit().putInt("port", listener.localPort).commit()
            )
                throw IOException()
            val pool =
                ThreadPoolExecutor(
                    2,
                    2,
                    0,
                    TimeUnit.MILLISECONDS,
                    ArrayBlockingQueue(2),
                    ThreadPoolExecutor.AbortPolicy(),
                )
            workers = pool
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            timers = scheduler
            val token = protocol.open(options.windowMs)
            val link = PairingLink(host, listener.localPort, identity.pin, token).encode()
            scheduler.schedule(
                { closeIfCurrent(listener) },
                options.windowMs,
                TimeUnit.MILLISECONDS,
            )
            Thread({ acceptLoop(listener, pool, scheduler) }, "Postbox-receiver").apply {
                isDaemon = true
                start()
            }
            return ReceiverSession(link, System.currentTimeMillis() + options.windowMs)
        } catch (_: Exception) {
            close()
            throw IOException("Receiver unavailable. Check the local network and try again.")
        }
    }

    @Synchronized
    fun close() {
        val listener = server
        server = null
        try {
            listener?.close()
        } catch (_: Exception) {}
        sockets.toList().forEach {
            try {
                it.close()
            } catch (_: Exception) {}
        }
        sockets.clear()
        workers?.shutdownNow()
        workers = null
        timers?.shutdownNow()
        timers = null
        protocol.close()
    }

    fun forgetPhone() {
        protocol.forget()
    }

    @Synchronized
    private fun closeIfCurrent(listener: SSLServerSocket) {
        if (server === listener) close()
    }

    private fun acceptLoop(
        listener: SSLServerSocket,
        pool: ThreadPoolExecutor,
        scheduler: java.util.concurrent.ScheduledExecutorService,
    ) {
        try {
            while (!listener.isClosed) {
                val socket = listener.accept() as SSLSocket
                sockets.add(socket)
                if (listener.isClosed) {
                    sockets.remove(socket)
                    socket.close()
                    continue
                }
                val deadline =
                    try {
                        scheduler.schedule(
                            {
                                try {
                                    socket.close()
                                } catch (_: Exception) {}
                            },
                            30,
                            TimeUnit.SECONDS,
                        )
                    } catch (_: RejectedExecutionException) {
                        sockets.remove(socket)
                        socket.close()
                        continue
                    }
                try {
                    pool.execute {
                        try {
                            serve(socket)
                        } finally {
                            deadline.cancel(false)
                            sockets.remove(socket)
                            try {
                                socket.close()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    deadline.cancel(false)
                    sockets.remove(socket)
                    socket.close()
                }
            }
        } catch (_: Exception) {
            closeIfCurrent(listener)
        }
    }

    private fun serve(socket: SSLSocket) {
        try {
            socket.soTimeout = 5000
            socket.startHandshake()
            val request = BoundedHttp.readRequest(socket.inputStream)
            if (request.path == "/pair") {
                if (request.authorization != null) throw ProtocolFailure()
                val pair =
                    try {
                        postboxJson.decodeFromString<PairBody>(
                            request.body.toString(Charsets.UTF_8)
                        )
                    } catch (_: Exception) {
                        throw ProtocolFailure()
                    }
                val token = protocol.pair(pair.token)
                BoundedHttp.writeResponse(
                    socket.outputStream,
                    200,
                    postboxJson.encodeToString(PairBody(token)).toByteArray(),
                )
            } else {
                val header = request.authorization ?: throw ProtocolFailure(403)
                if (!header.startsWith("Bearer ")) throw ProtocolFailure(403)
                val result = protocol.save(header.removePrefix("Bearer "), request.body)
                val status =
                    when (result) {
                        SaveResult.ADDED -> 201
                        SaveResult.ALREADY_PRESENT -> 200
                        SaveResult.CONFLICT -> 409
                        SaveResult.FULL -> 507
                    }
                BoundedHttp.writeResponse(
                    socket.outputStream,
                    status,
                    postboxJson.encodeToString(ResultBody(result.name)).toByteArray(),
                )
            }
        } catch (failure: ProtocolFailure) {
            try {
                BoundedHttp.writeResponse(socket.outputStream, failure.status, "{}".toByteArray())
            } catch (_: Exception) {}
        } catch (_: IOException) {
            // Timeout, closure, and incomplete transport never acknowledge a capture.
        } catch (_: Exception) {
            try {
                BoundedHttp.writeResponse(socket.outputStream, 503, "{}".toByteArray())
            } catch (_: Exception) {}
        }
    }

    private fun lanHost(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: throw IOException()
        return Collections.list(interfaces)
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .mapNotNull(InetAddress::getHostAddress)
            .firstOrNull { PairingLink.validLocalHost(it) } ?: throw IOException()
    }
}
