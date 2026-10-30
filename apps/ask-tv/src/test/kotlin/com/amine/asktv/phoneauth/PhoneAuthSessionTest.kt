package com.amine.asktv.phoneauth

import com.amine.asktv.OAuthProtocol
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** The network boundaries are fakes. The generation, deadline, retry, and launch state is real. */
class PhoneAuthSessionTest {
    private val link = PhonePairingLink("192.168.1.2", 8443, "ab".repeat(32), "a".repeat(43))
    private val authorization = OAuthProtocol("urn:uuid:00000000-0000-4000-8000-000000000001").run { begin(); authorizationUrl() }
    private val state = PhoneAuthorizationValidator.validate(authorization).state
    private fun await(message: String, condition: () -> Boolean) {
        val limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < limit) Thread.sleep(5)
        assertTrue(message, condition())
    }
    private class Listener(val callback: (String) -> Unit, val failure: (String) -> Unit, val events: MutableList<String>) : PhoneLoopbackListener {
        var bindFailure = false
        @Volatile var closed = false
        override fun start() { events += "bind"; if (bindFailure) throw IOException("occupied") }
        override fun expectState(value: String) { events += "expect:$value" }
        override fun close() { closed = true }
    }
    private inner class Relay(val events: MutableList<String>) : PhoneRelayTransport {
        var claimAction: () -> RelayClaim = { RelayClaim("b".repeat(43), authorization, 30_000) }
        val attempts = CopyOnWriteArrayList<Long>()
        val targets = CopyOnWriteArrayList<String>()
        var callbackAction: () -> RelayReceipt = { RelayReceipt("accepted") }
        @Volatile var result = RelayState.WAITING
        @Volatile var closed = false
        val cancellations = AtomicInteger()
        override fun claim(): RelayClaim { events += "claim"; return claimAction() }
        override fun callback(relayToken: String, target: String): RelayReceipt {
            assertEquals("b".repeat(43), relayToken)
            targets += target; attempts += System.nanoTime()
            return callbackAction()
        }
        override fun status(relayToken: String): RelayState { assertEquals("b".repeat(43), relayToken); events += "status"; return result }
        override fun cancel(relayToken: String) { assertEquals("b".repeat(43), relayToken); cancellations.incrementAndGet() }
        override fun close() { closed = true }
    }
    @Test fun bindsBeforeClaimAndInstallsExpectedStateBeforeBrowserReadiness() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events)
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events) }, { 1_000L }).use { session ->
            session.start(link)
            await("browser ready") { session.snapshot.value.canOpenBrowser }
            assertEquals(listOf("bind", "claim", "expect:$state"), events.take(3))
            assertEquals(PhoneAuthPhase.READY_FOR_BROWSER, session.snapshot.value.phase)
            assertEquals(authorization, session.takeBrowserLaunch()!!.url)
            assertNull(session.takeBrowserLaunch())
            assertEquals(PhoneAuthPhase.WAITING_FOR_CALLBACK, session.snapshot.value.phase)
        }
    }
    @Test fun occupiedCallbackPortNeverClaimsOrOffersBrowser() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events)
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events).apply { bindFailure = true } }, { 0L }).use { session ->
            session.start(link); await("bind failure") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertFalse(events.contains("claim")); assertNull(session.takeBrowserLaunch())
        }
    }
    @Test fun pinFailureNeverOffersBrowserAndClosesListener() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events).apply { claimAction = { throw IOException("pin") } }
        lateinit var listener: Listener
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events).also { listener = it } }, { 0L }).use { session ->
            session.start(link); await("pin failure") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertNull(session.takeBrowserLaunch()); await("listener closed") { listener.closed }
        }
    }
    @Test fun invalidAuthorizationUrlNeverOffersBrowser() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events).apply {
            claimAction = { RelayClaim("b".repeat(43), "https://example.test/", 30_000) }
        }
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events) }, { 0L }).use { session ->
            session.start(link); await("URL rejected") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertNull(session.takeBrowserLaunch())
        }
    }
    @Test fun claimRemainingLifetimeSubtractsClaimTransitAndDoesNotResetTenMinutes() {
        val events = CopyOnWriteArrayList<String>(); var clock = 10_000L
        val relay = Relay(events).apply { claimAction = { clock = 12_000; RelayClaim("b".repeat(43), authorization, 5_000) } }
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events) }, { clock }).use { session ->
            session.start(link); await("browser ready") { session.snapshot.value.canOpenBrowser }
            assertEquals(15_000L, session.snapshot.value.deadlineElapsedMs)
        }
    }
    @Test fun callbackRetriesIdenticalBytesOnlyThreeTimesWithBoundedBackoff() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events).apply { callbackAction = { throw IOException("lost response") } }
        lateinit var listener: Listener
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events).also { listener = it } }, { 0L }).use { session ->
            session.start(link); await("browser ready") { session.snapshot.value.canOpenBrowser }; session.takeBrowserLaunch()
            val target = "/auth/callback?state=$state&code=synthetic&client_id=issued"
            listener.callback(target)
            await("retry exhaustion") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertEquals(listOf(target, target, target), relay.targets.toList())
            assertTrue(TimeUnit.NANOSECONDS.toMillis(relay.attempts[1] - relay.attempts[0]) >= 200)
            assertTrue(TimeUnit.NANOSECONDS.toMillis(relay.attempts[2] - relay.attempts[1]) >= 450)
        }
    }
    @Test fun receiptIsNotConnectedAndStatusPollsAtMostOnceEachSecond() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events)
        lateinit var listener: Listener
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events).also { listener = it } }, { 0L }).use { session ->
            session.start(link); await("browser ready") { session.snapshot.value.canOpenBrowser }; session.takeBrowserLaunch()
            listener.callback("/auth/callback?state=$state&code=synthetic&client_id=issued")
            await("delivery state") { session.snapshot.value.phase == PhoneAuthPhase.VERIFYING }
            assertFalse(session.snapshot.value.message.contains("connected", ignoreCase = true))
            val before = events.count { it == "status" }; Thread.sleep(200)
            assertTrue(events.count { it == "status" } - before <= 1)
            relay.result = RelayState.CONNECTED
            await("TV terminal") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
        }
    }
    @Test fun canceledGenerationCannotDeliverLateCallbackOrFailureToReplacement() {
        val events = CopyOnWriteArrayList<String>(); val listeners = CopyOnWriteArrayList<Listener>(); val relays = CopyOnWriteArrayList<Relay>()
        PhoneAuthSession({ Relay(events).also { relays += it } }, { cb, fail -> Listener(cb, fail, events).also { listeners += it } }, { 0L }).use { session ->
            session.start(link); await("first ready") { session.snapshot.value.canOpenBrowser }
            session.cancel(); session.start(link)
            await("replacement ready") { listeners.size == 2 && session.snapshot.value.canOpenBrowser }
            listeners[0].callback("/auth/callback?state=$state&code=stale&client_id=issued")
            listeners[0].failure("stale failure"); Thread.sleep(100)
            assertEquals(PhoneAuthPhase.READY_FOR_BROWSER, session.snapshot.value.phase)
            assertTrue(relays.all { it.targets.isEmpty() })
        }
    }
    @Test fun lateBlockedClaimCannotReplaceNewGeneration() {
        val events = CopyOnWriteArrayList<String>(); val entered = CountDownLatch(1); val release = CountDownLatch(1)
        val first = Relay(events).apply { claimAction = { entered.countDown(); release.await(3, TimeUnit.SECONDS); RelayClaim("b".repeat(43), authorization, 1) } }
        val next = Relay(events); val count = AtomicInteger()
        PhoneAuthSession({ if (count.getAndIncrement() == 0) first else next }, { cb, fail -> Listener(cb, fail, events) }, { 0L }).use { session ->
            session.start(link); assertTrue(entered.await(2, TimeUnit.SECONDS)); session.cancel(); session.start(link)
            release.countDown(); await("replacement ready") { session.snapshot.value.canOpenBrowser }
            assertEquals(30_000L, session.snapshot.value.deadlineElapsedMs)
            assertTrue(first.closed)
        }
    }
    @Test fun cancellationPreservesAlreadyCommittedTvResult() {
        val events = CopyOnWriteArrayList<String>(); val relay = Relay(events).apply { result = RelayState.CONNECTED }
        PhoneAuthSession({ relay }, { cb, fail -> Listener(cb, fail, events) }, { 0L }).use { session ->
            session.start(link); await("ready") { session.snapshot.value.canOpenBrowser }; session.cancel()
            await("cancel completed") { session.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertFalse(session.snapshot.value.message.contains("erased", true))
            await("cancel request") { relay.cancellations.get() == 1 }
        }
    }
}
