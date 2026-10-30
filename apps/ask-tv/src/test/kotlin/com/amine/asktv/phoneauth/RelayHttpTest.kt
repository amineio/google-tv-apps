package com.amine.asktv.phoneauth

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class RelayHttpTest {
    private fun request(path: String = "/status", body: String = "", headers: String = "") =
        "POST $path HTTP/1.1\r\nContent-Length: ${body.length}\r\n$headers\r\n$body".byteInputStream()
    private fun rejected(status: Int = 400, action: () -> Unit) {
        assertEquals(status, assertThrows(RelayRejected::class.java, action).status)
    }
    @Test fun declaredBodyIsBoundedBeforeAllocationOrRead() {
        val prefix = "POST /claim HTTP/1.1\r\nContent-Length: 2147483647\r\n\r\n".toByteArray()
        val input = object : InputStream() {
            var i = 0
            override fun read(): Int {
                if (i >= prefix.size) fail("Oversized body must not be read")
                return prefix[i++].toInt() and 255
            }
        }
        rejected(413) { RelayHttp.readRequest(input) }
    }
    @Test fun duplicateHeadersEncodingAndFormsAreRejected() {
        for (header in listOf("Content-Length: 0\r\n", "Authorization: Bearer a\r\nAuthorization: Bearer b\r\n", "Transfer-Encoding: chunked\r\n", "Host: a\r\nHost: b\r\n")) {
            rejected { RelayHttp.readRequest(request(headers = header)) }
        }
        for (form in listOf("token=a&token=b", "token=a&%74oken=b", "token=%FF", "token=a&other=b", "")) {
            rejected { RelayHttp.form(RelayHttp.readRequest(request("/claim", form)), "token") }
        }
    }
    @Test fun unsupportedRoutesMethodsAndNonemptyStatusAreRejected() {
        rejected(405) { RelayHttp.readRequest("GET /status HTTP/1.1\r\nContent-Length: 0\r\n\r\n".byteInputStream()) }
        rejected { RelayHttp.readRequest(request("/other")) }
        rejected { RelayHttp.readRequest(request("/status", "x")) }
        rejected { RelayHttp.readRequest(request("/cancel", "x")) }
        rejected { RelayHttp.readRequest(request(headers = "Authorization: Bearer x\r\n") .let { ByteArrayInputStream(it.readBytes() + "POST /status HTTP/1.1\r\n".toByteArray()) }) }
    }
    @Test fun headerAndLineByteBoundsApplyWhileReading() {
        rejected { RelayHttp.readRequest(("POST /" + "a".repeat(8192) + " HTTP/1.1\r\n").byteInputStream()) }
        rejected { RelayHttp.readRequest(("POST /status HTTP/1.1\r\nX: " + "a".repeat(16384) + "\r\n\r\n").byteInputStream()) }
    }
    @Test fun fixedResponseSchemasRoundTripAndRejectUnexpectedFields() {
        val claim = RelayClaim("a".repeat(43), "https://auth.openai.com/oauth/authorize?state=s", 4000)
        assertEquals(claim, RelayHttp.decodeClaim(RelayHttp.claimBody(claim)))
        for (state in RelayState.entries) assertEquals(state, RelayHttp.decodeState(RelayHttp.stateBody(state)))
        assertEquals(RelayReceipt("accepted"), RelayHttp.decodeReceipt(RelayHttp.receiptBody()))
        rejected { RelayHttp.decodeState("{\"state\":\"waiting\",\"extra\":true}".toByteArray()) }
        rejected { RelayHttp.decodeState("{\"state\":\"waiting\",\"state\":\"failed\"}".toByteArray()) }
        rejected { RelayHttp.decodeReceipt("{\"receipt\":\"connected\"}".toByteArray()) }
        val output = ByteArrayOutputStream()
        RelayHttp.writeResponse(output, 202, RelayHttp.receiptBody())
        val response = RelayHttp.readResponse(ByteArrayInputStream(output.toByteArray()))
        assertEquals(202, response.status)
        assertEquals(RelayReceipt("accepted"), RelayHttp.decodeReceipt(response.body))
    }
}
