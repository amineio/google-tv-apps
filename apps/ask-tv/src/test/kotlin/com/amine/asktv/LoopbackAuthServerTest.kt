package com.amine.asktv

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class LoopbackAuthServerTest {
    /** Supplies a real HTTP callback request and deterministically resets its ACK output. */
    private class ResetBrowserSocket(request: String) : Socket() {
        private val input = ByteArrayInputStream(request.toByteArray(Charsets.US_ASCII))
        var writeAttempts = 0
        var wasClosed = false
        override fun getInputStream(): InputStream = input
        override fun getOutputStream(): OutputStream = object : OutputStream() {
            override fun write(value: Int) {
                writeAttempts++
                throw IOException("Synthetic browser connection reset")
            }
        }
        override fun setSoTimeout(timeout: Int) {}
        override fun close() { wasClosed = true }
    }

    private fun callbackRequest(state: String): String =
        "GET /auth/callback?state=$state&code=synthetic-code&client_id=issued HTTP/1.1\r\n" +
            "Host: 127.0.0.1:1455\r\n\r\n"

    /** Uses the production HTTP handler without opening port 1455 or adding a test-only server route. */
    private fun handle(server: LoopbackAuthServer, socket: Socket) {
        val method = LoopbackAuthServer::class.java.getDeclaredMethod("handle", Socket::class.java)
        method.isAccessible = true
        method.invoke(server, socket)
    }

    @Test fun acceptedCallbackPersistsIssuedClientDespiteBrowserAckFailure() {
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        val store = SessionStore(MemoryRecords(CredentialRecord("urn:uuid:test")))
        var deliveries = 0
        var failures = 0
        val server = LoopbackAuthServer(protocol, { grant ->
            deliveries++
            protocol.withActiveAttempt(grant.attempt) { store.rememberIssuedClient(grant.clientId) }
        }, { failures++ })
        val browser = ResetBrowserSocket(callbackRequest(attempt.state))
        try {
            handle(server, browser)
            assertTrue(browser.writeAttempts > 0)
            assertTrue(browser.wasClosed)
            assertEquals(1, deliveries)
            assertEquals(0, failures)
            assertEquals("issued", store.snapshot().pendingClientId)
            assertNull(store.snapshot().subject)
            assertNull(store.snapshot().accessToken)
        } finally { server.close() }
    }

    @Test fun repeatedCallbackAfterAckFailureNeverDeliversGrantTwice() {
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        var deliveries = 0
        val server = LoopbackAuthServer(protocol, { deliveries++ }, {})
        try {
            handle(server, ResetBrowserSocket(callbackRequest(attempt.state)))
            handle(server, ResetBrowserSocket(callbackRequest(attempt.state)))
            assertEquals(1, deliveries)
        } finally { server.close() }
    }
}
