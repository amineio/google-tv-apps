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
) : AutoCloseable {
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
    private var authorizationUrl: String? = null
    private var state = RelayState.WAITING
    private var terminalAt: Long? = null
    private var dispatchedDigest: ByteArray? = null
    private var accepted = false

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
                if (state.isTerminal) throw RelayRejected(409)
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
            dispatchedDigest = digest
            if (!state.isTerminal) state = RelayState.VERIFYING
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

    /** Call synchronously INSIDE OAuthProtocol.commit's durable action, after installation.
     * Queued UI delivery must not establish CONNECTED or PLAN_REQUIRED.
     */
    fun terminal(state: RelayState) {
        require(state.isTerminal) { "Invalid terminal relay state." }
        synchronized(stateLock) {
            if (!opened || this.state.isTerminal) return
            this.state = state
            terminalAt = monotonicMs()
            bootstrap = null
        }
    }

    fun cancel(relayToken: String) {
        synchronized(stateLock) { authorize(relayToken) }
        // A commit may hold OAuth while publishing terminal under stateLock.
        // Waiting for it under stateLock would deadlock and could erase its result.
        protocol.cancel()
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
