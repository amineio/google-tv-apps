package com.amine.postbox.core

import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable internal data class PairBody(val token: String)

@Serializable internal data class ResultBody(val result: String)

class PostboxClient
internal constructor(
    private val connectHostOverride: String?,
    private val timeouts: TransferTimeouts = TransferTimeouts(),
) {
    constructor() : this(null)

    fun pair(link: PairingLink): PairedTv = safely {
        PairingLink.parse(link.encode())
        val response =
            request(
                link.host,
                link.port,
                link.pin,
                "/pair",
                null,
                postboxJson.encodeToString(PairBody(link.token)).toByteArray(),
            )
        if (response.status != 200) throw IOException()
        val token =
            postboxJson.decodeFromString<PairBody>(response.body.toString(Charsets.UTF_8)).token
        PairingLink(link.host, link.port, link.pin, token).encode()
        PairedTv(link.host, link.port, link.pin, token, "Postbox TV")
    }

    fun send(tv: PairedTv, capture: PostCapture): SaveResult = safely {
        PairingLink(tv.host, tv.port, tv.pin, tv.token).encode()
        val response =
            request(
                tv.host,
                tv.port,
                tv.pin,
                "/captures",
                tv.token,
                CaptureCodec().canonicalBytes(capture),
            )
        if (response.status !in listOf(200, 201, 409, 507)) throw IOException()
        val result =
            SaveResult.valueOf(
                postboxJson
                    .decodeFromString<ResultBody>(response.body.toString(Charsets.UTF_8))
                    .result
            )
        if (
            response.status !=
                when (result) {
                    SaveResult.ADDED -> 201
                    SaveResult.ALREADY_PRESENT -> 200
                    SaveResult.CONFLICT -> 409
                    SaveResult.FULL -> 507
                }
        )
            throw IOException()
        result
    }

    internal fun openPinned(
        host: String,
        port: Int,
        pin: String,
        totalDeadline: SocketDeadline? = null,
    ): SSLSocket {
        PairingLink(host, port, pin, "a".repeat(43)).encode()
        val expected = pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val trust =
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    throw CertificateException("Peer rejected.")
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    if (
                        chain.isEmpty() ||
                            !MessageDigest.isEqual(
                                expected,
                                MessageDigest.getInstance("SHA-256").digest(chain[0].encoded),
                            )
                    )
                        throw CertificateException("Peer rejected.")
                    chain[0].checkValidity()
                }
            }
        val factory =
            SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }.socketFactory
        val socket = factory.createSocket() as SSLSocket
        try {
            totalDeadline?.register(socket)
            socket.soTimeout = timeouts.readMs
            socket.connect(InetSocketAddress(connectHostOverride ?: host, port), timeouts.connectMs)
            SocketDeadline(timeouts.handshakeMs).use { handshake ->
                handshake.register(socket)
                socket.startHandshake()
                handshake.check()
            }
            totalDeadline?.check()
            // Verify the negotiated peer again before returning any application stream.
            val certificate = socket.session.peerCertificates.first() as X509Certificate
            trust.checkServerTrusted(arrayOf(certificate), "RSA")
            return socket
        } catch (error: Exception) {
            socket.close()
            throw IOException("TV connection unavailable.", error)
        }
    }

    private fun request(
        host: String,
        port: Int,
        pin: String,
        path: String,
        token: String?,
        body: ByteArray,
    ): HttpResponse {
        return SocketDeadline(timeouts.totalMs).use { deadline ->
            openPinned(host, port, pin, deadline).use { connection ->
                deadline.check()
                val authorization = if (token == null) "" else "Authorization: Bearer $token\r\n"
                val header =
                    "POST $path HTTP/1.1\r\nHost: $host\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\n${authorization}Connection: close\r\n\r\n"
                connection.outputStream.apply {
                    write(header.toByteArray(Charsets.US_ASCII))
                    write(body)
                    flush()
                }
                val response = BoundedHttp.readResponse(connection.inputStream)
                deadline.check()
                response
            }
        }
    }

    private inline fun <T> safely(block: () -> T): T =
        try {
            block()
        } catch (error: Exception) {
            val handshakeCause =
                if (error is IOException && error.message == "TV connection unavailable.") error
                else null
            throw IOException(
                "TV transfer unavailable. Keep the saved draft and retry.",
                handshakeCause,
            )
        }
}
