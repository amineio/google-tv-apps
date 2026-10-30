package com.amine.postbox.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLServerSocket
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinnedTransferTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun directory() =
        File(context.cacheDir, "tls-${java.util.UUID.randomUUID()}").apply { mkdirs() }

    private fun receiver(store: PostboxStore, window: Long = 300000) =
        CaptureReceiver(
            context,
            store,
            {},
            ReceiverOptions(
                bindHost = "127.0.0.1",
                advertisedHost = "192.168.1.2",
                port = 0,
                windowMs = window,
            ),
        )

    private fun sample() =
        PostCapture(
            java.util.UUID.randomUUID().toString(),
            "Test post",
            "Text\n\nMore",
            "",
            emptyList(),
            1700000000000,
        )

    @Test
    fun realIdentityPairTransferReloadAndRetry() {
        val d = directory()
        val s = PostboxStore(d)
        val r = receiver(s)
        try {
            val link = PairingLink.parse(r.start().pairingLink)
            val c = PostboxClient("127.0.0.1")
            val tv = c.pair(link)
            val capture = sample()
            assertEquals(SaveResult.ADDED, c.send(tv, capture))
            assertEquals(SaveResult.ALREADY_PRESENT, c.send(tv, capture))
            assertEquals(capture, PostboxStore(d).list().single().capture)
            assertEquals(SaveResult.CONFLICT, c.send(tv, capture.copy(text = "Changed")))
        } finally {
            r.close()
        }
    }

    @Test
    fun validPortraitImagesAreDurablyReloaded() {
        fun jpeg(color: Int): String {
            val bitmap =
                android.graphics.Bitmap.createBitmap(
                    48,
                    96,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
            return try {
                bitmap.eraseColor(color)
                val output = java.io.ByteArrayOutputStream()
                assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, output))
                java.util.Base64.getEncoder().encodeToString(output.toByteArray())
            } finally {
                bitmap.recycle()
            }
        }
        val directory = directory()
        val capture =
            sample()
                .copy(
                    text = "",
                    imagesJpegBase64 = listOf(jpeg(0xff445544.toInt()), jpeg(0xff776655.toInt())),
                )
        val store = PostboxStore(directory)
        assertEquals(SaveResult.ADDED, store.save(capture))
        assertEquals(capture, PostboxStore(directory).list().single().capture)
    }

    @Test
    fun invalidJpegTablesNeverCommit() {
        val hex = "FFD8FFC0000B080001000101011100FFDA0008010100003F0000FFD9"
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val capture =
            sample()
                .copy(
                    text = "",
                    imagesJpegBase64 = listOf(java.util.Base64.getEncoder().encodeToString(bytes)),
                )
        val store = PostboxStore(directory())
        assertThrows(IllegalArgumentException::class.java) { store.save(capture) }
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun clientHandshakeBudgetStopsDrippingPeer() {
        assertDrippingPeerClosesClient(
            TransferTimeouts(handshakeMs = 250, totalMs = 3000, readMs = 2000)
        )
    }

    @Test
    fun clientTotalDeadlineIncludesHandshake() {
        assertDrippingPeerClosesClient(
            TransferTimeouts(handshakeMs = 3000, totalMs = 250, readMs = 2000)
        )
    }

    private fun assertDrippingPeerClosesClient(timeouts: TransferTimeouts) {
        java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1")).use { listener ->
            val worker =
                Thread {
                        try {
                            listener.accept().use { peer -> dripHandshake(peer) }
                        } catch (_: java.io.IOException) {}
                    }
                    .apply { start() }
            try {
                val started = android.os.SystemClock.elapsedRealtime()
                val link =
                    PairingLink("192.168.1.2", listener.localPort, "00".repeat(32), "a".repeat(43))
                assertThrows(java.io.IOException::class.java) {
                    PostboxClient("127.0.0.1", timeouts).pair(link)
                }
                assertTrue(
                    "The elapsed deadline must stop partial TLS reads",
                    android.os.SystemClock.elapsedRealtime() - started < 1500,
                )
            } finally {
                listener.close()
                worker.join(2000)
            }
        }
    }

    @Test
    fun receiverHandshakeBudgetStopsDrippingPeer() {
        val store = PostboxStore(directory())
        val receiver =
            CaptureReceiver(
                context,
                store,
                {},
                ReceiverOptions(
                    bindHost = "127.0.0.1",
                    advertisedHost = "192.168.1.2",
                    port = 0,
                    timeouts = TransferTimeouts(handshakeMs = 250, totalMs = 3000, readMs = 2000),
                ),
            )
        try {
            val link = PairingLink.parse(receiver.start().pairingLink)
            java.net.Socket("127.0.0.1", link.port).use { peer ->
                peer.soTimeout = 1500
                val writer =
                    Thread {
                            try {
                                dripHandshake(peer)
                            } catch (_: java.io.IOException) {}
                        }
                        .apply { start() }
                val started = android.os.SystemClock.elapsedRealtime()
                try {
                    assertEquals(-1, peer.inputStream.read())
                } catch (error: java.io.IOException) {
                    if (error is java.net.SocketTimeoutException) throw error
                }
                assertTrue(android.os.SystemClock.elapsedRealtime() - started < 1500)
                peer.close()
                writer.join(2000)
            }
            assertTrue(store.list().isEmpty())
        } finally {
            receiver.close()
        }
    }

    private fun dripHandshake(socket: java.net.Socket) {
        // A complete TLS record header followed by an incomplete 100-byte handshake record.
        socket.outputStream.write(byteArrayOf(22, 3, 3, 0, 100))
        socket.outputStream.flush()
        repeat(50) {
            socket.outputStream.write(0)
            socket.outputStream.flush()
            Thread.sleep(80)
        }
    }

    @Test
    fun nativeIdentityCompletesHandshake() {
        val identity = TlsIdentity(context)
        val server =
            identity
                .serverFactory()
                .createServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1"))
                as SSLServerSocket
        val problem = java.util.concurrent.atomic.AtomicReference<Throwable>()
        val done = CountDownLatch(1)
        Thread {
                try {
                    (server.accept() as javax.net.ssl.SSLSocket).use {
                        it.soTimeout = 5000
                        it.startHandshake()
                    }
                } catch (error: Throwable) {
                    problem.set(error)
                } finally {
                    done.countDown()
                }
            }
            .start()
        try {
            try {
                PostboxClient("127.0.0.1")
                    .openPinned("192.168.1.2", server.localPort, identity.pin)
                    .close()
            } catch (error: Exception) {
                done.await(7, TimeUnit.SECONDS)
                throw AssertionError("Native server handshake failed", problem.get() ?: error)
            }
            assertTrue(done.await(7, TimeUnit.SECONDS))
            problem.get()?.let { throw AssertionError("Native server handshake failed", it) }
        } finally {
            server.close()
        }
    }

    @Test
    fun badSecretIsRejected() {
        val s = PostboxStore(directory())
        val r = receiver(s)
        try {
            val link = PairingLink.parse(r.start().pairingLink)
            val client = PostboxClient("127.0.0.1")
            val tv = client.pair(link)
            assertThrows(Exception::class.java) {
                client.send(tv.copy(token = "b".repeat(43)), sample())
            }
            assertTrue(s.list().isEmpty())
        } finally {
            r.close()
        }
    }

    @Test
    fun wrongPinSendsNoApplicationData() {
        val identity = TlsIdentity(context)
        val server =
            identity
                .serverFactory()
                .createServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1"))
                as SSLServerSocket
        val observed = AtomicInteger(-2)
        val done = CountDownLatch(1)
        val thread = Thread {
            try {
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    try {
                        observed.set(socket.getInputStream().read())
                    } catch (_: Exception) {
                        observed.set(-1)
                    }
                }
            } finally {
                done.countDown()
            }
        }
        thread.start()
        try {
            val link = PairingLink("192.168.1.2", server.localPort, "00".repeat(32), "a".repeat(43))
            assertThrows(Exception::class.java) { PostboxClient("127.0.0.1").pair(link) }
            assertTrue(done.await(10, TimeUnit.SECONDS))
            assertEquals(-1, observed.get())
        } finally {
            server.close()
        }
    }

    @Test
    fun expiryClosesAcceptedUpload() {
        val s = PostboxStore(directory())
        val r = receiver(s, 1500)
        try {
            val link = PairingLink.parse(r.start().pairingLink)
            val client = PostboxClient("127.0.0.1")
            val tv = client.pair(link)
            client.openPinned(tv.host, tv.port, tv.pin).use { socket ->
                socket.soTimeout = 5000
                val out = socket.outputStream
                out.write(
                    "POST /captures HTTP/1.1\r\nContent-Length: 1000\r\nAuthorization: Bearer ${tv.token}\r\n\r\nx"
                        .toByteArray()
                )
                out.flush()
                try {
                    assertEquals(-1, socket.inputStream.read())
                } catch (error: java.io.IOException) {
                    if (error is java.net.SocketTimeoutException) throw error
                }
            }
            assertTrue(s.list().isEmpty())
        } finally {
            r.close()
        }
    }

    @Test
    fun closeClosesAcceptedSocket() {
        val s = PostboxStore(directory())
        val r = receiver(s)
        val link = PairingLink.parse(r.start().pairingLink)
        assertTrue(r.isReceiving)
        PostboxClient("127.0.0.1").openPinned(link.host, link.port, link.pin).use { socket ->
            socket.soTimeout = 5000
            r.close()
            assertFalse(r.isReceiving)
            try {
                assertEquals(-1, socket.inputStream.read())
            } catch (error: java.io.IOException) {
                if (error is java.net.SocketTimeoutException) throw error
            }
        }
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun idleReadTimeoutClosesStalledUpload() {
        val s = PostboxStore(directory())
        val r = receiver(s)
        try {
            val link = PairingLink.parse(r.start().pairingLink)
            PostboxClient("127.0.0.1").openPinned(link.host, link.port, link.pin).use { socket ->
                socket.soTimeout = 8000
                socket.outputStream.write(
                    "POST /captures HTTP/1.1\r\nContent-Length: 1000\r\n\r\nx".toByteArray()
                )
                socket.outputStream.flush()
                try {
                    assertEquals(-1, socket.inputStream.read())
                } catch (error: java.io.IOException) {
                    if (error is java.net.SocketTimeoutException) throw error
                }
            }
            assertTrue(s.list().isEmpty())
        } finally {
            r.close()
        }
    }

    @Test
    fun connectionLimitRejectsFifthConnection() {
        val s = PostboxStore(directory())
        val r = receiver(s)
        val sockets = mutableListOf<java.net.Socket>()
        try {
            val link = PairingLink.parse(r.start().pairingLink)
            repeat(4) { sockets += java.net.Socket("127.0.0.1", link.port) }
            java.net.Socket("127.0.0.1", link.port).use { fifth ->
                fifth.soTimeout = 2000
                assertEquals(-1, fifth.inputStream.read())
            }
            assertTrue(s.list().isEmpty())
        } finally {
            sockets.forEach { it.close() }
            r.close()
        }
    }

    @Test
    fun pairedTokenSurvivesReceiveWindow() {
        val s = PostboxStore(directory())
        val first = receiver(s)
        val client = PostboxClient("127.0.0.1")
        val tv =
            try {
                client.pair(PairingLink.parse(first.start().pairingLink))
            } finally {
                first.close()
            }
        val second = receiver(s)
        try {
            val next = PairingLink.parse(second.start().pairingLink)
            assertEquals(SaveResult.ADDED, client.send(tv.copy(port = next.port), sample()))
            second.forgetPhone()
            assertThrows(Exception::class.java) { client.send(tv.copy(port = next.port), sample()) }
        } finally {
            second.close()
        }
    }
}
