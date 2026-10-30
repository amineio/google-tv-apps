package com.amine.asktv.phoneauth

import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PhoneAuthPhase { IDLE, STARTING, READY_FOR_BROWSER, WAITING_FOR_CALLBACK, VERIFYING, FINISHED }

data class PhoneAuthUi(
    val phase: PhoneAuthPhase = PhoneAuthPhase.IDLE,
    val message: String = "Use this phone to sign in to AI Chat on your TV.",
    val deadlineElapsedMs: Long? = null,
    val canOpenBrowser: Boolean = false,
)

/** Owns one in-memory attempt. Blocking transports never run on the caller's thread. */
class PhoneAuthSession(
    private val clientFactory: (PhonePairingLink) -> PhoneRelayTransport,
    private val listenerFactory: ((String) -> Unit, (String) -> Unit) -> PhoneLoopbackListener,
    private val monotonicMs: () -> Long,
) : AutoCloseable {
    private class Attempt(val generation: Long, val link: PhonePairingLink, val localDeadline: Long) {
        var listener: PhoneLoopbackListener? = null
        var client: PhoneRelayTransport? = null
        var token: String? = null
        var authorization: PhoneAuthorization? = null
        var deadline = localDeadline
        var launchTaken = false
        var callbackStarted = false
        var worker: Job? = null
        var expiry: Job? = null
    }
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(4))
    private val state = MutableStateFlow(PhoneAuthUi())
    val snapshot: StateFlow<PhoneAuthUi> = state.asStateFlow()
    private var generation = 0L
    private var current: Attempt? = null
    private var closed = false

    fun start(link: PhonePairingLink) {
        PhonePairingLink.parse(link.encode())
        val (attempt, previous) = synchronized(lock) {
            if (closed) return
            val previous = current
            val next = Attempt(++generation, link, monotonicMs() + 600_000)
            current = next
            state.value = PhoneAuthUi(PhoneAuthPhase.STARTING, "Connecting to your TV…", next.deadline)
            next to previous
        }
        previous?.let { retire(it, cancelRemote = true) }
        attempt.worker = scope.launch {
            try {
                if (!owns(attempt)) return@launch
                val listener = listenerFactory({ target -> acceptCallback(attempt, target) }, { _ -> fail(attempt, "The phone listener stopped. Start a new code on your TV.") })
                if (!installListener(attempt, listener)) { listener.close(); return@launch }
                listener.start()
                if (!owns(attempt)) return@launch
                val client = clientFactory(link)
                if (!installClient(attempt, client)) { client.close(); return@launch }
                val claimStarted = monotonicMs()
                val claim = client.claim()
                val authorization = PhoneAuthorizationValidator.validate(claim.authorizationUrl)
                require(claim.relayToken.matches(Regex("[A-Za-z0-9_-]{43}")))
                require(claim.expiresInMs in 1..600_000)
                val deadline = minOf(attempt.localDeadline, claimStarted + claim.expiresInMs)
                if (monotonicMs() >= deadline) throw IOException("Expired")
                synchronized(lock) {
                    if (current !== attempt || closed) return@launch
                    attempt.token = claim.relayToken
                    attempt.authorization = authorization
                    attempt.deadline = deadline
                }
                listener.expectState(authorization.state)
                synchronized(lock) {
                    if (current !== attempt || closed) return@launch
                    state.value = PhoneAuthUi(PhoneAuthPhase.READY_FOR_BROWSER, "Opening the secure ChatGPT sign-in page…", deadline, true)
                    attempt.expiry = scope.launch {
                        delay((deadline - monotonicMs()).coerceAtLeast(1))
                        fail(attempt, "Sign-in expired. Get a new code on your TV.")
                    }
                }
            } catch (_: Exception) {
                fail(attempt, "Could not connect securely. Keep AI Chat open on your TV and get a new code.")
            }
        }
    }
    private fun installListener(attempt: Attempt, listener: PhoneLoopbackListener): Boolean = synchronized(lock) {
        if (current !== attempt || closed) false else { attempt.listener = listener; true }
    }
    private fun installClient(attempt: Attempt, client: PhoneRelayTransport): Boolean = synchronized(lock) {
        if (current !== attempt || closed) false else { attempt.client = client; true }
    }
    private fun owns(attempt: Attempt): Boolean = synchronized(lock) { current === attempt && !closed }

    fun takeBrowserLaunch(): PhoneAuthorization? = synchronized(lock) {
        val attempt = current ?: return null
        if (closed || attempt.launchTaken || !state.value.canOpenBrowser || monotonicMs() >= attempt.deadline) return null
        attempt.launchTaken = true
        state.value = PhoneAuthUi(PhoneAuthPhase.WAITING_FOR_CALLBACK, "Finish sign-in in your browser. You can return here to cancel.", attempt.deadline)
        attempt.authorization
    }
    private fun acceptCallback(attempt: Attempt, target: String) {
        val accepted = synchronized(lock) {
            if (current !== attempt || closed || !attempt.launchTaken || attempt.callbackStarted || monotonicMs() >= attempt.deadline) false
            else {
                val values = runCatching { AuthCallbackParser.parseTarget(target) }.getOrNull()
                val actual = values?.get("state")
                val expected = attempt.authorization?.state
                if (actual == null || expected == null || !MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())) false
                else {
                    attempt.callbackStarted = true
                    state.value = PhoneAuthUi(PhoneAuthPhase.VERIFYING, "Sending authorization to your TV…", attempt.deadline)
                    true
                }
            }
        }
        if (!accepted) return
        attempt.worker = scope.launch {
            val client = attempt.client ?: return@launch
            val token = attempt.token ?: return@launch
            try {
                var delivered = false
                for (index in 0..2) {
                    if (index > 0) delay(if (index == 1) 250 else 500)
                    if (!owns(attempt) || monotonicMs() >= attempt.deadline) return@launch
                    try {
                        check(client.callback(token, target).receipt == "accepted")
                        delivered = true
                        break
                    } catch (failure: RelayRejected) { throw failure }
                    catch (_: IOException) { /* Retry only a lost transport response, within the shared deadline. */ }
                }
                if (!delivered) throw IOException("Delivery failed")
                synchronized(lock) {
                    if (current !== attempt || closed) return@launch
                    state.value = PhoneAuthUi(PhoneAuthPhase.VERIFYING, "Authorization sent. Check AI Chat on your TV.", attempt.deadline)
                }
                while (isActive && owns(attempt) && monotonicMs() < attempt.deadline) {
                    val result = client.status(token)
                    if (result.isTerminal) { finish(attempt, terminalMessage(result)); return@launch }
                    delay(1_000)
                }
            } catch (_: Exception) {
                fail(attempt, "Authorization could not be confirmed. Check AI Chat on your TV before trying again.")
            }
        }
    }
    private fun finish(attempt: Attempt, message: String) {
        synchronized(lock) {
            if (current !== attempt || closed) return
            current = null
            state.value = PhoneAuthUi(PhoneAuthPhase.FINISHED, message)
        }
        retire(attempt, cancelRemote = false)
    }
    private fun fail(attempt: Attempt, message: String) {
        synchronized(lock) {
            if (current !== attempt || closed) return
            current = null
            state.value = PhoneAuthUi(PhoneAuthPhase.FINISHED, message)
        }
        retire(attempt, cancelRemote = true)
    }
    fun cancel() {
        val attempt = synchronized(lock) {
            val old = current
            current = null
            ++generation
            state.value = PhoneAuthUi(PhoneAuthPhase.FINISHED, "Sign-in stopped on this phone. Check AI Chat on your TV.")
            old
        }
        attempt?.let { retire(it, cancelRemote = true) }
    }
    private fun retire(attempt: Attempt, cancelRemote: Boolean) {
        attempt.expiry?.cancel()
        attempt.worker?.cancel()
        scope.launch {
            // Closing first also unblocks a claim or callback currently inside a blocking socket call.
            runCatching { attempt.listener?.close() }
            runCatching { attempt.client?.close() }
            val token = synchronized(lock) { attempt.token.also { attempt.token = null; attempt.authorization = null } }
            val remaining = attempt.deadline - monotonicMs()
            if (cancelRemote && token != null && remaining > 0) {
                runCatching {
                    clientFactory(attempt.link).use { cancellation ->
                        val deadline = scope.launch { delay(remaining); cancellation.close() }
                        try {
                            cancellation.cancel(token)
                            // The result can already be committed. Never describe cancellation as erasing it.
                            if (monotonicMs() < attempt.deadline) cancellation.status(token)
                        } finally { deadline.cancel() }
                    }
                }
            }
        }
    }
    override fun close() {
        val attempt = synchronized(lock) {
            if (closed) return
            closed = true
            current.also { current = null; ++generation }
        }
        attempt?.let { retire(it, cancelRemote = true) }
        // Cleanup jobs are allowed to finish. No active attempt retains a listener or secret afterward.
        scope.launch {
            scope.coroutineContext[Job]?.children?.filter { it != coroutineContext[Job] }?.toList()?.forEach { it.join() }
            scope.cancel()
        }
    }
    private fun terminalMessage(result: RelayState): String = when (result) {
        RelayState.CONNECTED -> "Your TV is connected. You can close this helper."
        RelayState.PLAN_REQUIRED -> "Your TV needs ChatGPT plan permission. Continue on your TV."
        RelayState.CANCELED -> "Sign-in was canceled. You can get a new code on your TV."
        else -> "Sign-in did not finish. Check AI Chat on your TV."
    }
}
