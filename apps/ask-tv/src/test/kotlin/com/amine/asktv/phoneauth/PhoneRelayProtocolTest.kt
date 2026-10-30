package com.amine.asktv.phoneauth

import com.amine.asktv.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class PhoneRelayProtocolTest {
    private fun rejected(action: () -> Unit): RelayRejected = assertThrows(RelayRejected::class.java, action)
    private fun target(protocol: OAuthProtocol, code: String = "synthetic-code") =
        "/auth/callback?state=${protocol.pending!!.state}&code=$code&client_id=issued"

    @Test fun bootstrapIsSingleUseAndUntrustedRequestsDoNotConsumeAttempt() {
        var elapsed = 0L
        val oauth = OAuthProtocol("urn:uuid:test", now = { elapsed })
        val relay = PhoneRelayProtocol(oauth, { elapsed }, {})
        val bootstrap = relay.open()
        assertEquals(403, rejected { relay.claim("wrong") }.status)
        val claim = relay.claim(bootstrap)
        assertEquals(600_000L, claim.expiresInMs)
        assertEquals(43, claim.relayToken.length)
        assertFalse(claim.toString().contains(claim.relayToken))
        assertFalse(claim.toString().contains(claim.authorizationUrl))
        assertNotEquals(bootstrap, claim.relayToken)
        assertEquals(RelayState.WAITING, relay.status(claim.relayToken))
        assertEquals(403, rejected { relay.claim(bootstrap) }.status)
        assertEquals(403, rejected { relay.status(bootstrap) }.status)
        assertEquals(403, rejected { relay.cancel("wrong") }.status)
        assertEquals(403, rejected { relay.callback("wrong", target(oauth)) }.status)
        assertFalse(oauth.pending!!.consumed)
        elapsed = 600_000
        assertEquals(410, rejected { relay.status(claim.relayToken) }.status)
    }

    @Test fun openBeginsOnceAndAllRequestsReuseTheUrlAndOriginalDeadline() {
        var elapsed = 100L
        val oauth = OAuthProtocol("urn:uuid:test", "issued", now = { elapsed })
        val relay = PhoneRelayProtocol(oauth, { elapsed }, {})
        val bootstrap = relay.open()
        val attempt = oauth.pending!!
        assertThrows(IllegalStateException::class.java) { relay.open() }
        assertSame(attempt, oauth.pending)
        val originalUrl = oauth.authorizationUrl()
        elapsed += 25_000
        val claim = relay.claim(bootstrap)
        assertEquals(575_000L, claim.expiresInMs)
        assertEquals(originalUrl, claim.authorizationUrl)
        assertSame(attempt, oauth.pending)
        elapsed = 600_100
        assertEquals(410, rejected { relay.callback(claim.relayToken, target(oauth)) }.status)
    }

    @Test fun identicalCallbackRetriesHaveOneDispatchAndChangedBytesConflict() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val count = AtomicInteger()
        val relay = PhoneRelayProtocol(oauth, { 0L }, { count.incrementAndGet() })
        val claim = relay.claim(relay.open())
        val callback = target(oauth)
        assertEquals("accepted", relay.callback(claim.relayToken, callback).receipt)
        assertEquals("accepted", relay.callback(claim.relayToken, callback).receipt)
        assertEquals(1, count.get())
        assertEquals(RelayState.VERIFYING, relay.status(claim.relayToken))
        assertEquals(409, rejected { relay.callback(claim.relayToken, callback + "&extra=x") }.status)
        assertEquals(1, count.get())
    }

    @Test fun malformedAndWrongStateCallbacksNeverDispatchOrConsumeValidAttempt() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val count = AtomicInteger()
        val relay = PhoneRelayProtocol(oauth, { 0L }, { count.incrementAndGet() })
        val claim = relay.claim(relay.open())
        assertEquals(400, rejected { relay.callback(claim.relayToken, "/auth/callback?state=%FF") }.status)
        assertEquals(400, rejected { relay.callback(claim.relayToken, "/auth/callback?state=wrong&code=x&client_id=issued") }.status)
        assertEquals(0, count.get())
        assertFalse(oauth.pending!!.consumed)
        assertEquals("accepted", relay.callback(claim.relayToken, target(oauth)).receipt)
        assertEquals(1, count.get())
    }

    @Test fun deniedAuthorizationFailsWithoutDispatchOrAcceptedReceipt() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val relay = PhoneRelayProtocol(oauth, { 0L }, { fail("Denied authorization must not dispatch") })
        val claim = relay.claim(relay.open())
        assertEquals(400, rejected { relay.callback(claim.relayToken, "/auth/callback?state=${oauth.pending!!.state}&error=access_denied") }.status)
        assertEquals(RelayState.FAILED, relay.status(claim.relayToken))
    }

    @Test fun grantHandlerFailureDoesNotCreateAnAcceptedRetryReceipt() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val count = AtomicInteger()
        val relay = PhoneRelayProtocol(oauth, { 0L }, { count.incrementAndGet(); throw IllegalStateException("synthetic failure") })
        val claim = relay.claim(relay.open())
        val callback = target(oauth)
        assertEquals(400, rejected { relay.callback(claim.relayToken, callback) }.status)
        assertEquals(409, rejected { relay.callback(claim.relayToken, callback) }.status)
        assertEquals(RelayState.FAILED, relay.status(claim.relayToken))
        assertEquals(1, count.get())
    }

    @Test fun concurrentRetryWaitsUntilTheOriginalDispatchSucceeds() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val count = AtomicInteger()
        val oauth = OAuthProtocol("urn:uuid:test")
        val relay = PhoneRelayProtocol(oauth, { 0L }, {
            count.incrementAndGet(); entered.countDown(); check(release.await(3, TimeUnit.SECONDS))
        })
        val claim = relay.claim(relay.open())
        val callback = target(oauth)
        try {
            val first = pool.submit<RelayReceipt> { relay.callback(claim.relayToken, callback) }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val retry = pool.submit<RelayReceipt> { relay.callback(claim.relayToken, callback) }
            assertThrows(TimeoutException::class.java) { retry.get(100, TimeUnit.MILLISECONDS) }
            assertEquals(RelayState.VERIFYING, relay.status(claim.relayToken))
            release.countDown()
            assertEquals("accepted", first.get(2, TimeUnit.SECONDS).receipt)
            assertEquals("accepted", retry.get(2, TimeUnit.SECONDS).receipt)
            assertEquals(1, count.get())
        } finally { release.countDown(); pool.shutdownNow(); relay.close() }
    }

    @Test fun commitAndCancelDoNotInvertLocksOrEraseCommittedTerminalState() {
        val guarded = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cancelStarted = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val oauth = OAuthProtocol("urn:uuid:test")
        lateinit var relay: PhoneRelayProtocol
        var durable = false
        relay = PhoneRelayProtocol(oauth, { 0L }, { grant ->
            oauth.commit(grant.attempt) {
                guarded.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                durable = true
                relay.terminal(RelayState.CONNECTED)
            }
        })
        val claim = relay.claim(relay.open())
        val callback = target(oauth)
        try {
            val dispatch = pool.submit<RelayReceipt> { relay.callback(claim.relayToken, callback) }
            assertTrue(guarded.await(2, TimeUnit.SECONDS))
            val cancel = pool.submit { cancelStarted.countDown(); relay.cancel(claim.relayToken) }
            assertTrue(cancelStarted.await(2, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { cancel.get(100, TimeUnit.MILLISECONDS) }
            release.countDown()
            assertEquals("accepted", dispatch.get(2, TimeUnit.SECONDS).receipt)
            cancel.get(2, TimeUnit.SECONDS)
            assertTrue(durable)
            assertEquals(RelayState.CONNECTED, relay.status(claim.relayToken))
            assertNull(oauth.pending)
        } finally { release.countDown(); pool.shutdownNow(); relay.close() }
    }

    @Test fun cancelBeforeCommitPreventsLateGrantInstallation() {
        val oauth = OAuthProtocol("urn:uuid:test")
        var grant: AuthGrant? = null
        val relay = PhoneRelayProtocol(oauth, { 0L }, { grant = it })
        val claim = relay.claim(relay.open())
        relay.callback(claim.relayToken, target(oauth))
        relay.cancel(claim.relayToken)
        var installed = false
        assertThrows(AuthFailure::class.java) { oauth.commit(grant!!.attempt) { installed = true } }
        assertFalse(installed)
        assertEquals(RelayState.CANCELED, relay.status(claim.relayToken))
    }

    @Test fun cancelBeforeValidationPreservesCanceledDuringOAuthPublicationGap() {
        val invalidated = CountDownLatch(1)
        val publishCancellation = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val oauth = OAuthProtocol("urn:uuid:test")
        val dispatches = AtomicInteger()
        val commits = AtomicInteger()
        val relay = PhoneRelayProtocol(oauth, { 0L }, { grant ->
            dispatches.incrementAndGet()
            oauth.commit(grant.attempt) { commits.incrementAndGet() }
        }, {
            oauth.cancel()
            invalidated.countDown()
            check(publishCancellation.await(3, TimeUnit.SECONDS))
        })
        val claim = relay.claim(relay.open())
        val callback = target(oauth)
        try {
            val cancel = pool.submit { relay.cancel(claim.relayToken) }
            assertTrue(invalidated.await(2, TimeUnit.SECONDS))
            assertNull(oauth.pending)
            // The real OAuth attempt is invalid, but cancel() has not published its result yet.
            val rejectedCallback = pool.submit<RelayRejected> {
                rejected { relay.callback(claim.relayToken, callback) }
            }
            assertTrue(rejectedCallback.get(2, TimeUnit.SECONDS).status in setOf(400, 409))
            assertEquals(0, dispatches.get())
            assertEquals(0, commits.get())
            publishCancellation.countDown()
            cancel.get(2, TimeUnit.SECONDS)
            assertEquals(RelayState.CANCELED, relay.status(claim.relayToken))
        } finally { publishCancellation.countDown(); pool.shutdownNow(); relay.close() }
    }

    @Test fun terminalGraceIsThirtySecondsAndNeverExtendsAttemptDeadline() {
        for (terminalAt in listOf(10_000L, 590_000L)) {
            var elapsed = 0L
            val relay = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { elapsed }, {})
            val claim = relay.claim(relay.open())
            elapsed = terminalAt
            relay.terminal(RelayState.PLAN_REQUIRED)
            val end = minOf(terminalAt + 30_000, 600_000L)
            elapsed = end - 1
            assertEquals(RelayState.PLAN_REQUIRED, relay.status(claim.relayToken))
            relay.cancel(claim.relayToken)
            assertEquals(RelayState.PLAN_REQUIRED, relay.status(claim.relayToken))
            elapsed = end
            assertEquals(410, rejected { relay.status(claim.relayToken) }.status)
        }
    }

    @Test fun closedRelayCannotRestartOrUseOldTokens() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val relay = PhoneRelayProtocol(oauth, { 0L }, {})
        val bootstrap = relay.open()
        val claim = relay.claim(bootstrap)
        relay.close()
        assertNull(oauth.pending)
        assertEquals(410, rejected { relay.claim(bootstrap) }.status)
        assertEquals(410, rejected { relay.status(claim.relayToken) }.status)
        assertThrows(IllegalStateException::class.java) { relay.open() }
    }
}
