package com.amine.postbox.core

import java.io.ByteArrayInputStream
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class TransferProtocolTest {
    @Test
    fun temporaryTokenIsSingleUseAndOnePhoneReplacesAnother() {
        var now = 0L
        val p =
            TransferProtocol(
                PostboxStore(Files.createTempDirectory("protocol").toFile()),
                { now },
                {},
                {},
            )
        val secret = p.open(300000)
        val phone = p.pair(secret)
        assertTrue(p.authorized(phone))
        assertThrows(ProtocolFailure::class.java) { p.pair(secret) }
        val next = p.pair(p.open(300000))
        assertFalse(p.authorized(phone))
        assertTrue(p.authorized(next))
        p.forget()
        assertFalse(p.authorized(next))
        val exp = p.open(10)
        now = 11
        assertThrows(ProtocolFailure::class.java) { p.pair(exp) }
    }

    @Test
    fun conflictingRetryPreservesStoredCapture() {
        val s = PostboxStore(Files.createTempDirectory("protocol").toFile())
        val p = TransferProtocol(s, { 0L }, {}, {})
        val token = p.pair(p.open(100))
        val c = CaptureCodecTest.sample()
        assertEquals(SaveResult.ADDED, p.save(token, CaptureCodec().canonicalBytes(c)))
        assertEquals(SaveResult.ALREADY_PRESENT, p.save(token, CaptureCodec().canonicalBytes(c)))
        assertEquals(
            SaveResult.CONFLICT,
            p.save(token, CaptureCodec().canonicalBytes(c.copy(text = "Changed"))),
        )
        assertEquals(c, s.list().single().capture)
        assertThrows(ProtocolFailure::class.java) {
            p.save("wrong", CaptureCodec().canonicalBytes(c))
        }
    }

    @Test
    fun totalDeadlineClosesSocketDuringBlockedRead() {
        java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1")).use { listener ->
            java.net.Socket("127.0.0.1", listener.localPort).use { client ->
                listener.accept().use {
                    SocketDeadline(100).use { deadline ->
                        deadline.register(client)
                        client.soTimeout = 2000
                        val started = System.nanoTime()
                        assertThrows(java.io.IOException::class.java) { client.inputStream.read() }
                        assertTrue(client.isClosed)
                        assertTrue(
                            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                System.nanoTime() - started
                            ) < 1500
                        )
                    }
                }
            }
        }
    }

    @Test
    fun expiredDeadlineClosesLateRegisteredSocket() {
        SocketDeadline(1).use { deadline ->
            Thread.sleep(20)
            java.net.Socket().use { socket ->
                assertThrows(java.net.SocketTimeoutException::class.java) {
                    deadline.register(socket)
                }
                assertTrue(socket.isClosed)
            }
        }
    }

    @Test
    fun boundedHttpRejectsAmbiguityBeforeBody() {
        listOf(
                "Content-Length: 1\r\nContent-Length: 1",
                "Content-Length: 1\r\nAuthorization: x\r\nAuthorization: y",
                "Content-Length: 1\r\nTransfer-Encoding: chunked",
                "Content-Length: 4194305",
                "Content-Length: -1",
            )
            .forEach { headers ->
                assertThrows(ProtocolFailure::class.java) {
                    BoundedHttp.readRequest(
                        ByteArrayInputStream(
                            "POST /captures HTTP/1.1\r\n$headers\r\n\r\nx".toByteArray()
                        )
                    )
                }
            }
    }

    @Test
    fun boundedHttpRejectsUnknownMethodsAndOversizedLines() {
        listOf(
                "GET /pair HTTP/1.1\r\nContent-Length: 0\r\n\r\n",
                "POST /unknown HTTP/1.1\r\nContent-Length: 0\r\n\r\n",
                "P".repeat(2049) + "\r\n\r\n",
                "POST /pair HTTP/1.1\r\nX-Long: " +
                    "a".repeat(8192) +
                    "\r\nContent-Length: 0\r\n\r\n",
            )
            .forEach {
                assertThrows(ProtocolFailure::class.java) {
                    BoundedHttp.readRequest(ByteArrayInputStream(it.toByteArray()))
                }
            }
    }

    @Test
    fun boundedHttpRejectsOversizedPairAndTruncatedBody() {
        listOf(
                "POST /pair HTTP/1.1\r\nContent-Length: 4097\r\n\r\n",
                "POST /captures HTTP/1.1\r\nContent-Length: 2\r\n\r\nx",
            )
            .forEach {
                assertThrows(ProtocolFailure::class.java) {
                    BoundedHttp.readRequest(ByteArrayInputStream(it.toByteArray()))
                }
            }
    }
}
