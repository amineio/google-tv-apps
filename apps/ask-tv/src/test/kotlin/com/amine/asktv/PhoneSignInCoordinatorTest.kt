package com.amine.asktv

import com.amine.asktv.phoneauth.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.util.ArrayDeque
import java.util.concurrent.Executor

class PhoneSignInCoordinatorTest {
    private class Worker : Executor {
        val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun drain() { var count = 0; while (tasks.isNotEmpty()) { check(++count < 100); tasks.removeFirst().run() } }
    }
    private class Ticker {
        private var tick: (() -> Unit)? = null
        var closed = false
        fun start(action: () -> Unit): AutoCloseable {
            tick = action
            return AutoCloseable { closed = true }
        }
        fun fire() { if (!closed) tick?.invoke() }
        fun staleFire() { tick?.invoke() }
    }
    private class Host(val relay: PhoneRelayProtocol) : PhoneRelayHost {
        lateinit var link: PhonePairingLink
        var starts = 0
        var closes = 0
        var beforeReturn: (() -> Unit)? = null
        override fun start(): PhonePairingLink {
            starts++
            link = PhonePairingLink("192.168.1.2", 4444, "a".repeat(64), relay.open())
            beforeReturn?.invoke()
            return link
        }
        override fun close() { closes++; relay.close() }
        fun claim() = relay.claim(link.token)
    }
    private class Fixture {
        val worker = Worker()
        val ticker = Ticker()
        var elapsed = 1_000L
        val hosts = mutableListOf<Host>()
        val updates = mutableListOf<Pair<Long, PhonePairingUi>>()
        val grants = mutableListOf<Triple<OAuthProtocol, AuthGrant, Long>>()
        var onCreate: (Host) -> Unit = {}
        val owner = PhoneSignInCoordinator(
            onGrant = { protocol, grant, token -> grants.add(Triple(protocol, grant, token)) },
            onUi = { token, ui -> updates.add(token to ui) },
            hostFactory = { relay -> Host(relay).also { hosts.add(it); onCreate(it) } },
            worker = worker,
            monotonicMs = { elapsed },
            startTicker = ticker::start,
        )
        fun begin(record: CredentialRecord = CredentialRecord(HOST), consent: Boolean = false, token: Long = 41) {
            owner.start(record, consent, token)
            worker.drain()
        }
        fun tick() { ticker.fire(); worker.drain() }
        fun latest() = updates.last().second
    }
    private fun query(url: String) = URI(url).rawQuery.split('&').associate {
        val pieces = it.split('=', limit = 2)
        URLDecoder.decode(pieces[0], "UTF-8") to URLDecoder.decode(pieces[1], "UTF-8")
    }

    @Test fun startDefersTransportAndKeepsHostAndSavedClientBindings() {
        for (saved in listOf(
            CredentialRecord(HOST),
            CredentialRecord(HOST, pendingClientId = "pending-issued"),
            CredentialRecord(HOST, clientId = "issued", idToken = "synthetic-hint-that-must-not-leave-tv"),
        )) {
            val fixture = Fixture()
            fixture.owner.start(saved, false, 41)
            assertTrue("No host starts on the caller thread", fixture.hosts.isEmpty())
            fixture.worker.drain()
            val host = fixture.hosts.single()
            val params = query(host.claim().authorizationUrl)
            assertEquals(HOST, params["ext_agent_host_id"])
            assertEquals(saved.clientId ?: saved.pendingClientId ?: "dynamic_agent_client", params["client_id"])
            assertEquals(CALLBACK, params["redirect_uri"])
            assertFalse(params.containsKey("id_token_hint"))
            assertFalse(params.containsKey("prompt"))
            assertEquals(1, host.starts)
            fixture.owner.close(); fixture.worker.drain()
        }
    }

    @Test fun explicitConsentIsRetainedWithoutChangingTheRegistration() {
        val fixture = Fixture()
        fixture.begin(CredentialRecord(HOST, pendingClientId = "issued"), consent = true)
        val params = query(fixture.hosts.single().claim().authorizationUrl)
        assertEquals("consent", params["prompt"])
        assertEquals("issued", params["client_id"])
        fixture.owner.close(); fixture.worker.drain()
    }

    @Test fun claimRemovesQrWithoutExtendingDeadlineOrDispatchingAGrant() {
        val fixture = Fixture()
        fixture.begin()
        val before = fixture.latest()
        assertNotNull(before.link)
        assertEquals(601_000L, before.expiresAtElapsedMs)
        fixture.elapsed += 20_000
        fixture.hosts.single().claim()
        fixture.tick()
        assertNull(fixture.latest().link)
        assertEquals(before.expiresAtElapsedMs, fixture.latest().expiresAtElapsedMs)
        assertTrue(fixture.grants.isEmpty())
        fixture.owner.close(); fixture.worker.drain()
    }

    @Test fun callbackDispatchCarriesTheExactProtocolAndOperationGenerationOnce() {
        val fixture = Fixture()
        fixture.begin(token = 73)
        val host = fixture.hosts.single()
        val claim = host.claim()
        val target = "/auth/callback?state=${query(claim.authorizationUrl).getValue("state")}&code=synthetic&client_id=issued"
        assertEquals("accepted", host.relay.callback(claim.relayToken, target).receipt)
        assertEquals("accepted", host.relay.callback(claim.relayToken, target).receipt)
        val (protocol, grant, token) = fixture.grants.single()
        assertEquals(73L, token)
        assertSame(grant.attempt, protocol.pending)
        fixture.tick()
        assertEquals("verifying", fixture.latest().state)
        assertNull(fixture.latest().link)
        fixture.owner.close(); fixture.worker.drain()
    }

    @Test fun phoneCancelAndDenialBecomeVisibleWithoutAGrantCallback() {
        for (denied in listOf(false, true)) {
            val fixture = Fixture()
            fixture.begin()
            val host = fixture.hosts.single()
            val claim = host.claim()
            if (denied) {
                val state = query(claim.authorizationUrl).getValue("state")
                assertThrows(RelayRejected::class.java) {
                    host.relay.callback(claim.relayToken, "/auth/callback?state=$state&error=access_denied")
                }
            } else host.relay.cancel(claim.relayToken)
            fixture.tick()
            assertEquals(if (denied) "failed" else "canceled", fixture.latest().state)
            assertNull(fixture.latest().link)
            assertTrue(fixture.grants.isEmpty())
            fixture.owner.close(); fixture.worker.drain()
        }
    }

    @Test fun committedTerminalSurvivesOperationFinishAndRetainsOnlyBoundedStatusWindow() {
        val fixture = Fixture()
        val operations = AskController()
        val token = operations.begin { fixture.owner.close() }
        fixture.begin(token = token)
        val host = fixture.hosts.single()
        val claim = host.claim()
        fixture.elapsed += 5_000
        fixture.owner.terminal(RelayState.CONNECTED)
        operations.finish(token)
        assertFalse(operations.isCurrent(token))
        fixture.tick()
        assertEquals("connected", fixture.latest().state)
        assertEquals(0, host.closes)
        assertEquals(RelayState.CONNECTED, host.relay.status(claim.relayToken))
        fixture.elapsed += 29_999
        fixture.tick()
        assertEquals(0, host.closes)
        fixture.elapsed++
        fixture.tick()
        assertTrue(host.closes > 0)
        assertEquals("connected", fixture.latest().state)
        assertNull(fixture.latest().link)
    }

    @Test fun explicitSurfaceExitClosesTerminalOwnerAfterOperationFinish() {
        for (terminal in listOf(RelayState.CONNECTED, RelayState.PLAN_REQUIRED)) {
            val fixture = Fixture()
            fixture.begin()
            val host = fixture.hosts.single()
            host.claim()
            fixture.owner.terminal(terminal)
            fixture.tick()
            fixture.owner.close() // ViewModel surface owner must call this even after operation.finish().
            fixture.worker.drain()
            assertTrue(host.relay.snapshot().closed)
            assertTrue(fixture.ticker.closed)
        }
    }

    @Test fun closingDuringStartSuppressesLateQrAndClosesOnlyThatOwner() {
        val old = Fixture()
        old.onCreate = { host -> host.beforeReturn = { old.owner.close() } }
        old.begin(token = 1)
        assertTrue(old.hosts.single().closes > 0)
        assertFalse(old.updates.any { it.second.link != null })
        val replacement = Fixture()
        replacement.begin(token = 2)
        val before = replacement.updates.toList()
        old.ticker.staleFire(); old.worker.drain()
        assertEquals(before, replacement.updates)
        assertEquals(0, replacement.hosts.single().closes)
        replacement.owner.close(); replacement.worker.drain()
    }

    @Test fun retryUsesNewBootstrapStateNonceAndChallenge() {
        val first = Fixture()
        first.begin()
        val firstLink = first.latest().link
        val firstAuth = query(first.hosts.single().claim().authorizationUrl)
        first.owner.close(); first.worker.drain()
        val retry = Fixture()
        retry.begin()
        val secondAuth = query(retry.hosts.single().claim().authorizationUrl)
        assertNotEquals(firstLink, retry.latest().link)
        for (field in listOf("state", "nonce", "code_challenge")) assertNotEquals(firstAuth[field], secondAuth[field])
        assertEquals(firstAuth["ext_agent_host_id"], secondAuth["ext_agent_host_id"])
        retry.owner.close(); retry.worker.drain()
    }

    @Test fun diagnosticsNeverContainThePairingLinkOrBootstrap() {
        val fixture = Fixture()
        fixture.begin()
        val ui = fixture.latest()
        val bootstrap = PhonePairingLink.parse(ui.link!!).token
        assertFalse(ui.toString().contains(ui.link))
        assertFalse(ui.toString().contains(bootstrap))
        // This is the containing UI type's redaction contract, not just the leaf object's contract.
        assertFalse(AskUi(phonePairing = ui).toString().contains(bootstrap))
        fixture.owner.close(); fixture.worker.drain()
    }

    companion object { private const val HOST = "urn:uuid:00000000-0000-0000-0000-000000000001" }
}
