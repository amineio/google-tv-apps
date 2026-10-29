package com.amine.asktv

import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque

class AuthorizationCompletionTest {
    @Test fun committedAuthorizationRemainsVisibleAfterCancelBeforeUiDelivery() {
        val store = SessionStore(MemoryRecords(CredentialRecord("urn:uuid:test")))
        val operations = AskController()
        val token = operations.begin {}
        val queue = ArrayDeque<() -> Unit>()
        var visible = store.snapshot()
        var operationMessages = 0
        var modelRequests = 0
        val completion = AccountCompletion(operations, { queue.addLast(it) }, { store.snapshot() }, { visible = it!! })

        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        val grant = protocol.callback(mapOf("state" to attempt.state, "code" to "synthetic", "client_id" to "issued"))
        protocol.withActiveAttempt(grant.attempt) { store.rememberIssuedClient(grant.clientId) }
        protocol.commit(grant.attempt) { store.install(record(access="committed", expires=9000)) }
        completion.committed(token) { operationMessages++; modelRequests++ }

        operations.stop()
        assertNull(visible.accessToken)
        queue.removeFirst().invoke()

        assertTrue(visible.canInfer)
        assertEquals("committed", visible.accessToken)
        assertEquals("subject", visible.subject)
        assertEquals(0, operationMessages)
        assertEquals(0, modelRequests)
    }

    @Test fun queuedCompletionReadsCurrentStoreInsteadOfCommittedCredentials() {
        val store = SessionStore(MemoryRecords())
        val operations = AskController()
        val token = operations.begin {}
        val queue = ArrayDeque<() -> Unit>()
        var visible = store.snapshot()
        val completion = AccountCompletion(operations, { queue.addLast(it) }, { store.snapshot() }, { visible = it!! })
        store.install(record(access="committed", expires=9000))
        completion.committed(token) { fail("Canceled completion must not run model requests or messages") }

        operations.stop()
        store.signOut { true }
        queue.removeFirst().invoke()

        assertNull(visible.accessToken)
        assertFalse(visible.canInfer)
        assertEquals("issued", visible.clientId)
    }

    @Test fun currentCompletionRefreshesAccountBeforeDeliveringItsActiveActions() {
        val store = SessionStore(MemoryRecords())
        val operations = AskController()
        val token = operations.begin {}
        val queue = ArrayDeque<() -> Unit>()
        var visible: CredentialRecord? = null
        var actions = 0
        val completion = AccountCompletion(operations, { queue.addLast(it) }, { store.snapshot() }, { visible = it })
        store.install(record(access="committed", expires=9000))
        completion.committed(token) {
            assertEquals("committed", visible?.accessToken)
            actions++
        }
        queue.removeFirst().invoke()
        assertEquals(1, actions)
    }
}
