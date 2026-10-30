package com.amine.asktv.phoneauth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amine.asktv.OAuthProtocol
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLServerSocket

/** Real AndroidKeyStore certificates and Conscrypt sockets; no permissive test trust manager. */
@RunWith(AndroidJUnit4::class)
class PhoneRelayTlsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun server(protocol: PhoneRelayProtocol, timeouts: RelayTimeouts = RelayTimeouts(), duration: Long = 600_000) =
        PhoneRelayServer(context, protocol, timeouts, bindHostOverride = "127.0.0.1", durationMs = duration)
    private fun client(link: PhonePairingLink, timeouts: RelayTimeouts = RelayTimeouts()) =
        PhoneRelayClient(link, timeouts, connectHostOverride = "127.0.0.1")
    private fun closedWithinBound(socket: Socket) {
        socket.soTimeout = 1500
        val before = System.nanoTime()
        try { assertEquals(-1, socket.inputStream.read()) }
        catch (error: java.io.IOException) { if (error is SocketTimeoutException) throw error }
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 1500)
    }
    @Test fun realKeystorePinClaimWrongTokenAndCallbackReplay() {
        val oauth = OAuthProtocol("urn:uuid:test")
        val dispatches = AtomicInteger()
        val protocol = PhoneRelayProtocol(oauth, { System.nanoTime() / 1_000_000 }, { dispatches.incrementAndGet() })
        server(protocol).use { relay ->
            val link = relay.start()
            client(link).use { phone ->
                val claim = phone.claim()
                assertEquals(RelayState.WAITING, phone.status(claim.relayToken))
                assertEquals(403, assertThrows(RelayRejected::class.java) { phone.status("b".repeat(43)) }.status)
                val target = "/auth/callback?state=${oauth.pending!!.state}&code=synthetic&client_id=issued"
                assertEquals("accepted", phone.callback(claim.relayToken, target).receipt)
                assertEquals("accepted", phone.callback(claim.relayToken, target).receipt)
                assertEquals(409, assertThrows(RelayRejected::class.java) { phone.callback(claim.relayToken, "$target&extra=x") }.status)
                assertEquals(1, dispatches.get())
                phone.cancel(claim.relayToken)
                assertEquals(RelayState.CANCELED, phone.status(claim.relayToken))
            }
        }
    }
    @Test fun wrongCertificateReceivesZeroApplicationBytes() {
        val identity = RelayTlsIdentity(context)
        val socket = identity.serverFactory().createServerSocket(0, 2, InetAddress.getByName("127.0.0.1")) as SSLServerSocket
        val observed = AtomicInteger(-2)
        val done = CountDownLatch(1)
        Thread {
            try { socket.accept().use { peer ->
                peer.soTimeout = 5000
                try { observed.set(peer.inputStream.read()) } catch (_: Exception) { observed.set(-1) }
            } } finally { done.countDown() }
        }.start()
        socket.use {
            val link = PhonePairingLink("192.168.1.2", socket.localPort, "00".repeat(32), "a".repeat(43))
            client(link).use { phone -> assertThrows(Exception::class.java) { phone.claim() } }
            assertTrue(done.await(7, TimeUnit.SECONDS)); assertEquals(-1, observed.get())
        }
    }
    @Test fun drippingHandshakeClosesAtAbsoluteDeadline() {
        val protocol = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { System.nanoTime() / 1_000_000 }, {})
        server(protocol, RelayTimeouts(handshakeMs = 250, totalMs = 250)).use { relay ->
            val link = relay.start()
            Socket("127.0.0.1", link.port).use { peer ->
                peer.outputStream.write(byteArrayOf(22, 3, 3, 0, 100)); peer.outputStream.flush()
                val writer = Thread { runCatching { repeat(25) { peer.outputStream.write(0); peer.outputStream.flush(); Thread.sleep(40) } } }
                writer.start(); closedWithinBound(peer); writer.join(1500)
            }
        }
    }
    @Test fun partialHeadersAndBodiesCloseAtAbsoluteDeadline() {
        for (partial in listOf("POST /claim HTTP/1.1\r\nContent-Length: ", "POST /claim HTTP/1.1\r\nContent-Length: 100\r\n\r\ntoken=")) {
            val protocol = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { System.nanoTime() / 1_000_000 }, {})
            server(protocol, RelayTimeouts(totalMs = 250)).use { relay ->
                val link = relay.start()
                client(link).use { phone -> phone.openPinned().use { peer ->
                    peer.outputStream.write(partial.toByteArray()); peer.outputStream.flush(); closedWithinBound(peer)
                } }
            }
        }
    }
    @Test fun fifthConnectionIsRejectedAndQueuedSocketDeadlineStartsOnAccept() {
        val protocol = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { System.nanoTime() / 1_000_000 }, {})
        server(protocol, RelayTimeouts(totalMs = 250)).use { relay ->
            val link = relay.start()
            val peers = mutableListOf<Socket>()
            try {
                repeat(4) { peers += Socket("127.0.0.1", link.port) }
                Socket("127.0.0.1", link.port).use { closedWithinBound(it) }
                peers.forEach { closedWithinBound(it) }
            } finally { peers.forEach { it.close() } }
        }
    }
    @Test fun expiryClosesListenerAndAcceptedSocket() {
        val protocol = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { System.nanoTime() / 1_000_000 }, {})
        server(protocol, duration = 250).use { relay ->
            val link = relay.start()
            Socket("127.0.0.1", link.port).use { closedWithinBound(it) }
            assertThrows(Exception::class.java) { Socket("127.0.0.1", link.port).close() }
        }
    }
    @Test fun canceledGraceEndsAndClosesListenerWithinOriginalDeadline() {
        val protocol = PhoneRelayProtocol(OAuthProtocol("urn:uuid:test"), { System.nanoTime() / 1_000_000 }, {})
        server(protocol, duration = 800).use { relay ->
            val link = relay.start()
            client(link).use { phone -> val claim = phone.claim(); phone.cancel(claim.relayToken); assertEquals(RelayState.CANCELED, phone.status(claim.relayToken)) }
            Socket("127.0.0.1", link.port).use { closedWithinBound(it) }
            assertThrows(Exception::class.java) { Socket("127.0.0.1", link.port).close() }
        }
    }
    @Test fun blockedOutputIsClosedBySocketDeadline() {
        val accepted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val listener = ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))
        val workers = RelaySocketWorkers(RelayTimeouts(totalMs = 250)) { socket ->
            accepted.countDown()
            try { val chunk = ByteArray(8192); repeat(8192) { socket.outputStream.write(chunk) } }
            catch (_: java.io.IOException) { } finally { finished.countDown() }
        }
        listener.use {
            Socket("127.0.0.1", listener.localPort).use { peer ->
                peer.receiveBufferSize = 1024
                workers.use {
                    workers.accept(listener.accept().apply { sendBufferSize = 1024 })
                    assertTrue(accepted.await(1, TimeUnit.SECONDS))
                    assertTrue(finished.await(1500, TimeUnit.MILLISECONDS))
                }
            }
        }
    }
}
