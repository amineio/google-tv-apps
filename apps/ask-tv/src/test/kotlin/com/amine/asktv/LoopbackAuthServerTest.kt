package com.amine.asktv

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

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

    private class CapturedBrowserSocket(private val input: InputStream) : Socket() {
        val output = ByteArrayOutputStream()
        var wasClosed = false
        var readTimeout = 0
        override fun getInputStream(): InputStream = input
        override fun getOutputStream(): OutputStream = output
        override fun setSoTimeout(timeout: Int) { readTimeout = timeout }
        override fun close() { wasClosed = true }
    }

    /** The supplied prefix arrives normally. Every later read deterministically times out. */
    private fun timeoutAfter(prefix: String): InputStream = object : InputStream() {
        private val bytes = prefix.toByteArray(Charsets.US_ASCII)
        private var position = 0
        override fun read(): Int {
            if (position == bytes.size) throw SocketTimeoutException("Synthetic browser input timeout")
            return bytes[position++].toInt() and 255
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position == bytes.size) throw SocketTimeoutException("Synthetic browser input timeout")
            val count = minOf(length, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
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
    private fun assertTimeoutClosesQuietly(prefix: (String) -> String, expectedStage: String) {
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        var grants = 0
        var failures = 0
        val diagnostics = mutableListOf<Pair<String, String>>()
        val server = LoopbackAuthServer(protocol, { grants++ }, { failures++ },
            diagnostic = { stage, exceptionClass -> diagnostics += stage to exceptionClass })
        val incomplete = CapturedBrowserSocket(timeoutAfter(prefix(attempt.state)))
        try {
            handle(server, incomplete)

            assertTrue(incomplete.wasClosed)
            assertEquals(5000, incomplete.readTimeout)
            assertEquals("An incomplete connection must not receive an unsolicited HTTP error", 0, incomplete.output.size())
            assertEquals(0, grants)
            assertEquals(0, failures)
            assertSame(attempt, protocol.pending)
            assertFalse(attempt.consumed)
            assertEquals(listOf(expectedStage to "java.net.SocketTimeoutException"), diagnostics)

            val callback = CapturedBrowserSocket(ByteArrayInputStream(callbackRequest(attempt.state).toByteArray(Charsets.US_ASCII)))
            handle(server, callback)
            assertTrue(callback.wasClosed)
            assertEquals(1, grants)
            assertEquals(0, failures)
            assertTrue(callback.output.toString("UTF-8").startsWith("HTTP/1.1 200 "))
            assertEquals(1, diagnostics.size)
        } finally { server.close() }
    }

    @Test fun preconnectedSocketTimeoutClosesQuietlyAndPreservesPendingAuthorization() {
        assertTimeoutClosesQuietly({ "" }, "READ_REQUEST")
    }

    @Test fun incompleteHeadersTimeoutClosesQuietlyAndPreservesPendingAuthorization() {
        assertTimeoutClosesQuietly({ state ->
            "GET /auth/callback?state=$state&code=synthetic-code&client_id=issued HTTP/1.1\r\n" +
                "Host: 127.0.0.1:1455\r\nX-Partial: "
        }, "READ_HEADERS")
    }

    @Test fun malformedSharedParserInputDoesNotConsumeMacAuthorization() {
        val badTargets = listOf("/auth/callback?state=%FF", "/auth/callback?state=x&%73tate=y",
            "http://127.0.0.1:1455/auth/callback?state=x", "/auth/callback?state=x#fragment")
        for (target in badTargets) {
            val protocol = OAuthProtocol("urn:uuid:test")
            val attempt = protocol.begin()
            var grants = 0
            var failures = 0
            val server = LoopbackAuthServer(protocol, { grants++ }, { failures++ }, { _, _ -> })
            try {
                val invalid = CapturedBrowserSocket(ByteArrayInputStream("GET $target HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(Charsets.US_ASCII)))
                handle(server, invalid)
                assertTrue(invalid.output.toString("UTF-8").startsWith("HTTP/1.1 400 "))
                assertEquals(0, failures)
                assertSame(attempt, protocol.pending)
                assertFalse(attempt.consumed)
                handle(server, CapturedBrowserSocket(ByteArrayInputStream(callbackRequest(attempt.state).toByteArray(Charsets.US_ASCII))))
                assertEquals(1, grants)
            } finally { server.close() }
        }
    }

    @Test fun sharedCallbackParserPreservesMacStartAndStatusRoutes() {
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        val server = LoopbackAuthServer(protocol, { fail("Diagnostic routes cannot deliver a grant") }, {})
        try {
            for ((path, response) in listOf("/start" to "HTTP/1.1 302 ", "/status" to "HTTP/1.1 200 ")) {
                val socket = CapturedBrowserSocket(ByteArrayInputStream("GET $path HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(Charsets.US_ASCII)))
                handle(server, socket)
                val body = socket.output.toString("UTF-8")
                assertTrue(body.startsWith(response))
                if (path == "/start") assertTrue(body.contains("Location: " + protocol.authorizationUrl()))
                else assertTrue(body.endsWith("pending"))
            }
            assertSame(attempt, protocol.pending)
            assertFalse(attempt.consumed)
        } finally { server.close() }
    }

}
