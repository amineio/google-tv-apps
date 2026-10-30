package com.amine.postbox.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory
import javax.security.auth.x500.X500Principal

internal class TlsIdentity(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val keyStore: KeyStore

    init {
        synchronized(identityLock) {
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(ALIAS)) {
                val now = System.currentTimeMillis()
                val spec =
                    KeyGenParameterSpec.Builder(
                            ALIAS,
                            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                        )
                        .setKeySize(2048)
                        .setDigests(
                            KeyProperties.DIGEST_SHA256,
                            KeyProperties.DIGEST_SHA384,
                            KeyProperties.DIGEST_SHA512,
                            // Conscrypt signs prehashed TLS messages through raw RSA.
                            KeyProperties.DIGEST_NONE,
                        )
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setSignaturePaddings(
                            KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                            KeyProperties.SIGNATURE_PADDING_RSA_PSS,
                        )
                        .setCertificateSubject(X500Principal("CN=Postbox"))
                        .setCertificateSerialNumber(BigInteger.ONE)
                        .setCertificateNotBefore(Date(now - 86_400_000))
                        .setCertificateNotAfter(Date(now + 20L * 365 * 86_400_000))
                        .build()
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                    .apply {
                        initialize(spec)
                        generateKeyPair()
                    }
            }
        }
    }

    val pin: String
        get() =
            MessageDigest.getInstance("SHA-256")
                .digest(keyStore.getCertificate(ALIAS).encoded)
                .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun serverFactory(): SSLServerSocketFactory {
        val manager =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                init(keyStore, null)
            }
        return SSLContext.getInstance("TLS")
            .apply { init(manager.keyManagers, null, null) }
            .serverSocketFactory
    }

    companion object {
        private const val ALIAS = "postbox.receiver.v2"
        private val identityLock = Any()
    }
}
