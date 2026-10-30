package com.amine.asktv.phoneauth

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PhoneLoopbackServerTest {
    private fun exchange(port: Int, target: String): String = Socket("127.0.0.1", port).use {
        it.soTimeout = 1500
        it.outputStream.write("GET $target HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray())
        it.inputStream.readBytes().toString(Charsets.UTF_8)
    }
    @Test fun productionPortIsExclusiveAndOccupiedPortFails() {
        ServerSocket(1455, 2, InetAddress.getByName("127.0.0.1")).use {
            PhoneLoopbackServer({}, {}).use { listener -> assertThrows(Exception::class.java) { listener.start() } }
        }
        PhoneLoopbackServer({}, {}).use { listener ->
            listener.start()
            assertThrows(Exception::class.java) { ServerSocket(1455, 2, InetAddress.getByName("127.0.0.1")) }
        }
    }
    @Test fun exactPathStateAndDuplicatesAreValidatedBeforeForwarding() {
        val calls = AtomicInteger()
        PhoneLoopbackServer({ calls.incrementAndGet() }, {}, portOverride = 0).use { listener ->
            listener.start()
            assertTrue(exchange(listener.localPort, "/auth/callback?state=s&code=x").contains("400"))
            listener.expectState("s")
            for (target in listOf("/other?state=s", "/auth/callback?state=wrong&code=x", "/auth/callback?state=s&state=s&code=x", "http://127.0.0.1:1455/auth/callback?state=s&code=x")) {
                assertTrue(exchange(listener.localPort, target).contains("400"))
            }
            assertEquals(0, calls.get())
            assertTrue(exchange(listener.localPort, "/auth/callback?state=s&code=x").contains("Authorization sent"))
            assertEquals(1, calls.get())
        }
    }
    @Test fun browserWriteResetDoesNotDiscardCallback() {
        val delivered = CountDownLatch(1)
        val failures = AtomicInteger()
        PhoneLoopbackServer({ delivered.countDown() }, { failures.incrementAndGet() }, portOverride = 0).use { listener ->
            listener.start(); listener.expectState("s")
            Socket("127.0.0.1", listener.localPort).use {
                it.setSoLinger(true, 0)
                it.outputStream.write("GET /auth/callback?state=s&code=x HTTP/1.1\r\n\r\n".toByteArray())
                it.outputStream.flush()
                assertTrue(delivered.await(1, TimeUnit.SECONDS))
            }
            assertEquals(0, failures.get())
        }
    }
    @Test fun preconnectionAndPartialHeadersCloseQuietlyWithinAbsoluteDeadline() {
        val failures = AtomicInteger()
        PhoneLoopbackServer({}, { failures.incrementAndGet() }, RelayTimeouts(readMs = 5000, totalMs = 250), portOverride = 0).use { listener ->
            listener.start()
            for (partial in listOf("", "GET /auth/callback?state=s HTTP/1.1\r\nHost: ")) {
                Socket("127.0.0.1", listener.localPort).use {
                    it.soTimeout = 1500
                    it.outputStream.write(partial.toByteArray()); it.outputStream.flush()
                    val before = System.nanoTime()
                    assertEquals(-1, it.inputStream.read())
                    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 1500)
                }
            }
            assertEquals(0, failures.get())
        }
    }
}
