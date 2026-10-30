package com.amine.asktv

import android.content.Context
import android.os.SystemClock
import com.amine.asktv.phoneauth.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal interface PhoneRelayHost { fun start(): PhonePairingLink; fun close() }
data class PhonePairingUi(val link: String?, val expiresAtElapsedMs: Long, val state: String) {
    override fun toString() = "PhonePairingUi(redacted, state=$state)"
}

/** One instance owns one attempt. Closing never waits on the UI thread. */
class PhoneSignInCoordinator internal constructor(
    private val onGrant: (OAuthProtocol, AuthGrant, Long) -> Unit,
    private val onUi: (Long, PhonePairingUi) -> Unit,
    private val hostFactory: (PhoneRelayProtocol) -> PhoneRelayHost,
    private val worker: Executor,
    private val monotonicMs: () -> Long,
    private val startTicker: (() -> Unit) -> AutoCloseable,
) : AutoCloseable {
    constructor(context: Context, onGrant: (OAuthProtocol, AuthGrant, Long) -> Unit,
        onUi: (Long, PhonePairingUi) -> Unit) : this(onGrant, onUi, { relay ->
        val server = PhoneRelayServer(context.applicationContext, relay)
        object : PhoneRelayHost { override fun start() = server.start(); override fun close() = server.close() }
    }, workers, SystemClock::elapsedRealtime, { tick ->
        val future = timer.scheduleAtFixedRate(tick, 250, 250, TimeUnit.MILLISECONDS)
        AutoCloseable { future.cancel(false) }
    })
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val startDone = CompletableFuture<Unit>()
    internal val released = CompletableFuture<Unit>()
    @Volatile private var host: PhoneRelayHost? = null
    @Volatile private var relay: PhoneRelayProtocol? = null
    @Volatile private var ticker: AutoCloseable? = null
    @Volatile private var token = 0L
    private var deadline = 0L
    private var link: String? = null
    private var terminalState: String? = null
    private var lastUi: PhonePairingUi? = null

    fun start(record: CredentialRecord, requestConsent: Boolean, operationToken: Long) {
        check(started.compareAndSet(false, true) && !closed.get())
        token = operationToken
        worker.execute {
            try {
                if (closed.get()) return@execute
                val oauth = OAuthProtocol(record.hostId, record.clientId ?: record.pendingClientId, requestConsent)
                val protocol = PhoneRelayProtocol(oauth, monotonicMs) { grant ->
                    if (closed.get()) throw AuthFailure()
                    onGrant(oauth, grant, operationToken)
                }
                relay = protocol
                val created = hostFactory(protocol)
                host = created
                if (closed.get()) return@execute
                val result = created.start()
                if (closed.get()) return@execute
                val observedAt = monotonicMs()
                val snapshot = protocol.snapshot()
                deadline = observedAt + snapshot.remainingMs
                link = result.encode()
                publish(snapshot)
                val scheduled = startTicker { worker.execute { poll() } }
                ticker = scheduled
                if (closed.get()) scheduled.close()
            } catch (_: Exception) {
                if (!closed.get()) {
                    terminalState = "failed"
                    onUi(token, PhonePairingUi(null, deadline, "failed"))
                    close()
                }
            } finally { startDone.complete(Unit) }
        }
    }
    private fun poll() {
        if (closed.get()) return
        val snapshot = relay?.snapshot() ?: return
        publish(snapshot)
        if (snapshot.closed) close()
    }
    @Synchronized private fun publish(snapshot: RelaySnapshot) {
        if (closed.get()) return
        if (snapshot.state != RelayState.WAITING && snapshot.state != RelayState.VERIFYING) terminalState = snapshot.state.wireValue
        val state = terminalState ?: if (snapshot.closed) "expired" else snapshot.state.wireValue
        val presentation = PhonePairingUi(if (snapshot.claimed || snapshot.closed || terminalState != null) null else link, deadline, state)
        if (lastUi != presentation) { lastUi = presentation; onUi(token, presentation) }
    }
    /** Called inside the OAuth durable commit guard; no network or cleanup here. */
    fun terminal(state: RelayState) { relay?.terminal(state) }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        ticker?.close()
        if (!started.get()) startDone.complete(Unit)
        startDone.whenComplete { _, _ -> worker.execute {
            try { host?.close() ?: relay?.close() }
            finally { link = null; released.complete(Unit) }
        } }
    }
    companion object {
        private val workers = Executors.newCachedThreadPool { r -> Thread(r, "AIChat-tv-pairing").apply { isDaemon = true } }
        private val timer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "AIChat-tv-pairing-clock").apply { isDaemon = true } }
    }
}
