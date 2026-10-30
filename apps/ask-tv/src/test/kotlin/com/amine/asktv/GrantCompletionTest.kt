package com.amine.asktv

import com.amine.asktv.phoneauth.RelayState
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Proposed internal helper contract is recorded in task-4-test-seams.md.pending. */
class GrantCompletionTest {
    private fun attempt(store: SessionStore): Pair<OAuthProtocol, AuthGrant> {
        val saved = store.snapshot()
        val protocol = OAuthProtocol(saved.hostId, saved.clientId ?: saved.pendingClientId)
        val pending = protocol.begin()
        return protocol to protocol.callback(mapOf("state" to pending.state, "code" to "synthetic", "client_id" to "issued"))
    }

    private fun emptyStore() = SessionStore(MemoryRecords(CredentialRecord("urn:uuid:test")))

    @Test fun pendingClientIsDurableBeforeExchangeAndSurvivesExchangeFailure() {
        val records = MemoryRecords(CredentialRecord("urn:uuid:test"))
        val store = SessionStore(records)
        val (protocol, grant) = attempt(store)
        val completion = GrantCompletion(store) { actualGrant, current, _ ->
            assertSame(grant, actualGrant)
            assertEquals("issued", current.pendingClientId)
            assertEquals("issued", SessionStore(records).snapshot().pendingClientId)
            assertNull(current.accessToken)
            throw InvalidGrant()
        }
        assertThrows(InvalidGrant::class.java) {
            completion.complete(protocol, grant, Cancellation()) { fail("Failed exchange cannot publish success") }
        }
        assertEquals("issued", store.snapshot().pendingClientId)
        assertNull(store.snapshot().subject)
        assertNull(store.snapshot().accessToken)
    }

    @Test fun canceledAttemptCannotPersistPendingClientOrExchange() {
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        protocol.cancel()
        val completion = GrantCompletion(store) { _, _, _ -> fail("Canceled grant cannot exchange"); record() }
        assertThrows(AuthFailure::class.java) {
            completion.complete(protocol, grant, Cancellation()) { fail("Canceled grant cannot publish") }
        }
        assertNull(store.snapshot().pendingClientId)
        assertNull(store.snapshot().accessToken)
    }

    @Test fun mismatchedVerifiedIdentityCannotReplaceExistingAccountOrPublishSuccess() {
        val store = SessionStore(MemoryRecords(record()))
        val before = store.snapshot()
        val (protocol, grant) = attempt(store)
        val completion = GrantCompletion(store) { _, _, _ -> record(subject = "different-account", access = "new") }
        assertThrows(AuthFailure::class.java) {
            completion.complete(protocol, grant, Cancellation()) { fail("Identity mismatch cannot publish success") }
        }
        assertEquals(before, store.snapshot())
    }

    @Test fun failedIdentityOrScopeValidationAtExchangeCannotInstallOrPublish() {
        for (reason in listOf("identity", "scope")) {
            val store = emptyStore()
            val (protocol, grant) = attempt(store)
            val completion = GrantCompletion(store) { _, _, _ -> throw AuthFailure("synthetic $reason rejection") }
            assertThrows(AuthFailure::class.java) {
                completion.complete(protocol, grant, Cancellation()) { fail("Rejected exchange cannot publish") }
            }
            assertNull(store.snapshot().accessToken)
            assertFalse(store.snapshot().canInfer)
            assertEquals("issued", store.snapshot().pendingClientId)
        }
    }

    @Test fun missingPlanPermissionInstallsVerifiedIdentityAndPublishesPlanRequired() {
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        val completion = GrantCompletion(store) { _, _, _ -> record(access = "new", scopes = setOf("openid", "profile", "email")) }
        val states = mutableListOf<RelayState>()
        val result = completion.complete(protocol, grant, Cancellation(), states::add)
        assertEquals(listOf(RelayState.PLAN_REQUIRED), states)
        assertEquals("subject", store.snapshot().subject)
        assertEquals("new", store.snapshot().accessToken)
        assertFalse(result.canInfer)
        assertNull(store.snapshot().pendingClientId)
    }

    @Test fun terminalPublicationOccursAfterDurableInstallInsideTheOAuthGuard() {
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        var publications = 0
        val completion = GrantCompletion(store) { _, _, _ -> record(access = "new", expires = 9000) }
        val result = completion.complete(protocol, grant, Cancellation()) { state ->
            assertTrue("Publication must run under the OAuth commit guard", Thread.holdsLock(protocol))
            assertEquals("new", store.snapshot().accessToken)
            assertNull(store.snapshot().pendingClientId)
            assertEquals(RelayState.CONNECTED, state)
            publications++
        }
        assertTrue(result.canInfer)
        assertEquals(1, publications)
        assertNull(protocol.pending)
    }

    @Test fun failedDurableWriteCannotPublishConnected() {
        val records = MemoryRecords(CredentialRecord("urn:uuid:test"))
        val store = SessionStore(records)
        val (protocol, grant) = attempt(store)
        val completion = GrantCompletion(store) { _, _, _ -> records.fail = true; record(access = "new") }
        assertThrows(java.io.IOException::class.java) {
            completion.complete(protocol, grant, Cancellation()) { fail("Failed durable write cannot publish success") }
        }
        assertNull(store.snapshot().accessToken)
        assertEquals("issued", store.snapshot().pendingClientId)
    }

    @Test fun cancellationDuringExchangePreventsLateInstallation() {
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        val cancel = Cancellation()
        val exchanging = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val completion = GrantCompletion(store) { _, _, _ ->
            exchanging.countDown()
            check(release.await(3, TimeUnit.SECONDS))
            record(access = "late")
        }
        try {
            val result = pool.submit<Throwable?> {
                runCatching { completion.complete(protocol, grant, cancel) { fail("Canceled exchange cannot publish") } }.exceptionOrNull()
            }
            assertTrue(exchanging.await(2, TimeUnit.SECONDS))
            cancel.cancel()
            protocol.cancel()
            release.countDown()
            assertNotNull(result.get(2, TimeUnit.SECONDS))
            assertNull(store.snapshot().accessToken)
        } finally { release.countDown(); pool.shutdownNow() }
    }

    @Test fun signOutWaitsForCommitGuardThenClearsTheCommittedGrant() {
        val installed = CountDownLatch(1)
        val allowCommitReturn = CountDownLatch(1)
        val signOutEntered = CountDownLatch(1)
        val events = java.util.Collections.synchronizedList(mutableListOf<String>())
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        val cancel = Cancellation()
        val completion = GrantCompletion(store) { _, _, _ -> record(access = "committed", expires = 9000) }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val commit = pool.submit {
                completion.complete(protocol, grant, cancel) {
                    assertTrue(Thread.holdsLock(protocol))
                    events.add("terminal")
                    installed.countDown()
                    check(allowCommitReturn.await(3, TimeUnit.SECONDS))
                    events.add("commit-leaving")
                }
            }
            assertTrue(installed.await(2, TimeUnit.SECONDS))
            val signOut = pool.submit<Boolean> {
                signOutEntered.countDown()
                completion.cancelAndSignOut(protocol, cancel) {
                    events.add("revoke")
                    assertEquals("committed", it.accessToken)
                    true
                }
            }
            assertTrue(signOutEntered.await(2, TimeUnit.SECONDS))
            assertThrows(java.util.concurrent.TimeoutException::class.java) { signOut.get(100, TimeUnit.MILLISECONDS) }
            assertEquals("committed", store.snapshot().accessToken)
            // The future cannot finish while the established commit barrier remains held.
            allowCommitReturn.countDown()
            commit.get(2, TimeUnit.SECONDS)
            assertTrue(signOut.get(2, TimeUnit.SECONDS))
            assertEquals(listOf("terminal", "commit-leaving", "revoke"), events)
            assertNull(store.snapshot().accessToken)
            assertNull(store.snapshot().refreshToken)
            assertNull(store.snapshot().idToken)
            assertEquals("issued", store.snapshot().clientId)
        } finally { allowCommitReturn.countDown(); pool.shutdownNow() }
    }

    @Test fun signOutBeforeLateExchangeReturnCannotRestoreCredentials() {
        val store = emptyStore()
        val (protocol, grant) = attempt(store)
        val cancel = Cancellation()
        val exchanging = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val completion = GrantCompletion(store) { _, _, _ ->
            exchanging.countDown()
            check(release.await(3, TimeUnit.SECONDS))
            record(access = "late")
        }
        try {
            val result = pool.submit<Throwable?> {
                runCatching { completion.complete(protocol, grant, cancel) { fail("Signed-out grant cannot publish") } }.exceptionOrNull()
            }
            assertTrue(exchanging.await(2, TimeUnit.SECONDS))
            assertTrue(completion.cancelAndSignOut(protocol, cancel) { true })
            release.countDown()
            assertNotNull(result.get(2, TimeUnit.SECONDS))
            assertNull(store.snapshot().accessToken)
            assertNull(store.snapshot().refreshToken)
            assertEquals("issued", store.snapshot().pendingClientId)
        } finally { release.countDown(); pool.shutdownNow() }
    }

    @Test fun signOutAlsoWaitsForAnOlderRetiringAttemptAfterRouteReplacement() {
        val store = emptyStore()
        val (oldProtocol, oldGrant) = attempt(store)
        val oldCancel = Cancellation()
        val installed = CountDownLatch(1)
        val releaseOldGuard = CountDownLatch(1)
        val signOutEntered = CountDownLatch(1)
        val completion = GrantCompletion(store) { _, _, _ -> record(access = "older-commit", expires = 9000) }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val oldCommit = pool.submit {
                completion.complete(oldProtocol, oldGrant, oldCancel) {
                    installed.countDown()
                    check(releaseOldGuard.await(3, TimeUnit.SECONDS))
                }
            }
            assertTrue(installed.await(2, TimeUnit.SECONDS))
            // The new route is now current, but the previous route still owns its commit guard.
            val newProtocol = OAuthProtocol("urn:uuid:test", "issued")
            newProtocol.begin()
            val newCancel = Cancellation()
            val signOut = pool.submit<Boolean> {
                signOutEntered.countDown()
                completion.cancelAndSignOut(listOf(oldProtocol to oldCancel, newProtocol to newCancel)) { true }
            }
            assertTrue(signOutEntered.await(2, TimeUnit.SECONDS))
            assertThrows(java.util.concurrent.TimeoutException::class.java) { signOut.get(100, TimeUnit.MILLISECONDS) }
            assertEquals("older-commit", store.snapshot().accessToken)
            releaseOldGuard.countDown()
            oldCommit.get(2, TimeUnit.SECONDS)
            assertTrue(signOut.get(2, TimeUnit.SECONDS))
            assertNull(oldProtocol.pending)
            assertNull(newProtocol.pending)
            assertTrue(oldCancel.canceled)
            assertTrue(newCancel.canceled)
            assertNull(store.snapshot().accessToken)
        } finally { releaseOldGuard.countDown(); pool.shutdownNow() }
    }
}
