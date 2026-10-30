package com.amine.asktv.phoneauth

import com.amine.asktv.AuthFailure
import com.amine.asktv.AuthGrant
import com.amine.asktv.OAuthProtocol
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class RelayClaim(val relayToken: String, val authorizationUrl: String, val expiresInMs: Long) {
    override fun toString(): String = "RelayClaim(redacted)"
}
data class RelayReceipt(val receipt: String)
data class RelaySnapshot(val claimed: Boolean, val state: RelayState, val remainingMs: Long, val closed: Boolean)

enum class RelayState(val wireValue: String) {
    WAITING("waiting"), VERIFYING("verifying"), CONNECTED("connected"),
    PLAN_REQUIRED("plan_required"), FAILED("failed"), CANCELED("canceled");

    override fun toString(): String = wireValue
    internal val isTerminal: Boolean get() = this != WAITING && this != VERIFYING
}

class RelayRejected(val status: Int) : Exception("Relay request rejected.")

/** One instance owns one OAuth attempt. This class performs no network or storage I/O. */
class PhoneRelayProtocol(
    private val protocol: OAuthProtocol,
    private val monotonicMs: () -> Long,
    private val onGrant: (AuthGrant) -> Unit,
    private val cancelOAuth: () -> Unit,
) : AutoCloseable {
    constructor(protocol: OAuthProtocol, monotonicMs: () -> Long, onGrant: (AuthGrant) -> Unit) :
        this(protocol, monotonicMs, onGrant, { protocol.cancel() })

    private val stateLock = Any()
    // Never hold stateLock while acquiring this monitor, OAuth, or storage locks.
    // Identical retries wait here until validation AND dispatch have completed.
    private val dispatchLock = Any()
    private var opened = false
    private var closed = false
    private var started = 0L
    private var duration = 0L
    private var bootstrap: String? = null
    private var relayToken: String? = null
    private var claimed = false
    private var authorizationUrl: String? = null
    private var state = RelayState.WAITING
    private var terminalAt: Long? = null
    private var dispatchedDigest: ByteArray? = null
    private var accepted = false
    private var cancellationRequested = false

    fun open(durationMs: Long = 600_000): String {
        require(durationMs in 1..600_000) { "Invalid relay duration." }
        synchronized(stateLock) {
            check(!opened && !closed) { "Relay already opened or closed." }
            opened = true
            started = monotonicMs()
            duration = durationMs
        }
        // Snapshot exactly once, outside stateLock. Claim and retry never call OAuth begin.
        val url = try {
            protocol.begin()
            protocol.authorizationUrl()
        } catch (_: Exception) {
            synchronized(stateLock) { closed = true }
            protocol.cancel()
            throw RelayRejected(410)
        }
        val token = randomToken()
        val available = synchronized(stateLock) {
            if (closed) false else {
                authorizationUrl = url
                bootstrap = token
                true
            }
        }
        if (!available) {
            // close() may have run before begin(); do not leave that late attempt active.
            protocol.cancel()
            throw RelayRejected(410)
        }
        return token
    }

    fun claim(bootstrap: String): RelayClaim = synchronized(stateLock) {
        checkAlive()
        if (!sameSecret(this.bootstrap, bootstrap)) throw RelayRejected(403)
        val token = randomToken()
        this.bootstrap = null
        relayToken = token
        claimed = true
        RelayClaim(token, authorizationUrl!!, duration - (monotonicMs() - started))
    }

    fun callback(relayToken: String, target: String): RelayReceipt = synchronized(dispatchLock) {
        synchronized(stateLock) { authorize(relayToken) }
        // Bound before hashing, allocating a large byte array, or entering OAuth.
        if (target.length > AuthCallbackParser.MAX_TARGET_BYTES) throw RelayRejected(400)
        val bytes = target.toByteArray(Charsets.UTF_8)
        if (bytes.size > AuthCallbackParser.MAX_TARGET_BYTES) throw RelayRejected(400)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val alreadyAccepted = synchronized(stateLock) {
            authorize(relayToken)
            val previous = dispatchedDigest
            if (previous != null) {
                if (!accepted || !MessageDigest.isEqual(previous, digest)) throw RelayRejected(409)
                true
            } else {
                if (state.isTerminal || cancellationRequested) throw RelayRejected(409)
                false
            }
        }
        if (alreadyAccepted) return@synchronized RelayReceipt("accepted")
        val params = try { AuthCallbackParser.parseTarget(target) }
        catch (_: IllegalArgumentException) { throw RelayRejected(400) }

        val grant = try { protocol.callback(params) }
        catch (_: AuthFailure) {
            val consumed = synchronized(protocol) { protocol.pending?.consumed != false }
            if (consumed) terminal(RelayState.FAILED)
            throw RelayRejected(400)
        }
        synchronized(stateLock) {
            if (closed || cancellationRequested || state.isTerminal) throw RelayRejected(409)
            dispatchedDigest = digest
            state = RelayState.VERIFYING
        }
        try {
            // May synchronously enter OAuth's durable commit guard, then terminal().
            // No relay state lock is held at this boundary.
            onGrant(grant)
        } catch (_: Exception) {
            terminal(RelayState.FAILED)
            throw RelayRejected(400)
        }
        synchronized(stateLock) { accepted = true }
        RelayReceipt("accepted")
    }

    fun status(relayToken: String): RelayState = synchronized(stateLock) {
        authorize(relayToken)
        state
    }

    /** Secret-free observation. Expiry reporting does not cancel or mutate the attempt. */
    fun snapshot(): RelaySnapshot = synchronized(stateLock) {
        val now = monotonicMs()
        val elapsed = now - started
        val unavailable = !opened || closed || authorizationUrl == null || now < started ||
            elapsed < 0 || elapsed >= duration ||
            terminalAt?.let { now < it || now - it >= 30_000 } == true
        val remaining = if (unavailable) 0L else (duration - elapsed).coerceIn(0L, duration)
        RelaySnapshot(claimed, state, remaining, unavailable)
    }

    /** Call synchronously INSIDE OAuthProtocol.commit's durable action, after installation.
     * Queued UI delivery must not establish CONNECTED or PLAN_REQUIRED.
     */
    fun terminal(state: RelayState) {
        require(state.isTerminal) { "Invalid terminal relay state." }
        synchronized(stateLock) {
            if (!opened || this.state.isTerminal) return
            // OAuth invalidation and CANCELED publication are separate operations.
            // A callback rejected in that gap must not claim the terminal result.
            if (state == RelayState.FAILED && cancellationRequested) return
            this.state = state
            terminalAt = monotonicMs()
            bootstrap = null
        }
    }

    fun cancel(relayToken: String) {
        synchronized(stateLock) {
            authorize(relayToken)
            if (!state.isTerminal) cancellationRequested = true
        }
        // A commit may hold OAuth while publishing terminal under stateLock.
        // Waiting for it under stateLock would deadlock and could erase its result.
        cancelOAuth()
        terminal(RelayState.CANCELED)
    }

    override fun close() {
        synchronized(stateLock) {
            closed = true
            bootstrap = null
            relayToken = null
            authorizationUrl = null
        }
        protocol.cancel()
    }

    /** Only called under stateLock. Does not call OAuth or any external action. */
    private fun checkAlive() {
        val elapsed = monotonicMs() - started
        if (!opened || closed || authorizationUrl == null || elapsed < 0 || elapsed >= duration ||
            terminalAt?.let { monotonicMs() - it >= 30_000 } == true) throw RelayRejected(410)
    }

    private fun authorize(value: String) {
        checkAlive()
        if (!sameSecret(relayToken, value)) throw RelayRejected(403)
    }

    private fun sameSecret(expected: String?, supplied: String): Boolean {
        // Hash both values to equal-length byte arrays before constant-time comparison.
        if (supplied.length > 2_048) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val equal = MessageDigest.isEqual(digest.digest((expected ?: "").toByteArray(Charsets.UTF_8)),
            digest.digest(supplied.toByteArray(Charsets.UTF_8)))
        return expected != null && equal
    }

    private fun randomToken(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
}
