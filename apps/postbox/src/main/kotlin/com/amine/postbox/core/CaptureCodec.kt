package com.amine.postbox.core

import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val postboxJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
    isLenient = false
}
internal const val MAX_CAPTURE_BYTES = 4 * 1024 * 1024

class CaptureCodec {
    fun canonicalBytes(capture: PostCapture): ByteArray = safely {
        require(
            capture.id.matches(
                Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            )
        )
        require(capture.title.length <= 160 && capture.text.length <= 20_000)
        require(capture.capturedAtEpochMs >= 0 && capture.imagesJpegBase64.size <= 4)
        require(
            capture.text.isNotBlank() ||
                capture.sourceUrl.isNotBlank() ||
                capture.imagesJpegBase64.isNotEmpty()
        )
        val normalized = capture.copy(sourceUrl = normalizeSourceUrl(capture.sourceUrl))
        // Bound input before decoding a potentially large base64 allocation.
        require(normalized.imagesJpegBase64.sumOf { it.length.toLong() } <= MAX_CAPTURE_BYTES)
        normalized.imagesJpegBase64.forEach { value ->
            val bytes = Base64.getDecoder().decode(value)
            require(Base64.getEncoder().encodeToString(bytes) == value)
            validateJpeg(bytes)
        }
        postboxJson.encodeToString(normalized).toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_CAPTURE_BYTES)
        }
    }

    fun decode(bytes: ByteArray): PostCapture = safely {
        require(bytes.size <= MAX_CAPTURE_BYTES)
        val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        val capture = postboxJson.decodeFromString<PostCapture>(text)
        val canonical = canonicalBytes(capture)
        postboxJson.decodeFromString<PostCapture>(canonical.toString(Charsets.UTF_8))
    }

    fun digest(capture: PostCapture): String =
        MessageDigest.getInstance("SHA-256").digest(canonicalBytes(capture)).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }

    fun normalizeSourceUrl(sourceUrl: String): String = safely {
        if (sourceUrl.isEmpty()) return@safely ""
        require(sourceUrl.length <= 2048)
        val uri = URI(sourceUrl)
        require(
            uri.scheme.equals("https", true) &&
                uri.rawUserInfo == null &&
                uri.port in listOf(-1, 443)
        )
        val host =
            when (uri.host?.lowercase()) {
                "x.com",
                "www.x.com",
                "mobile.x.com",
                "twitter.com",
                "www.twitter.com",
                "mobile.twitter.com",
                "m.twitter.com" -> "x.com"
                "instagram.com",
                "www.instagram.com" -> "instagram.com"
                else -> throw IllegalArgumentException()
            }
        require(uri.rawAuthority != null && !uri.isOpaque)
        val path = uri.normalize().rawPath.orEmpty().ifEmpty { "/" }
        require(!path.contains('\\') && !path.contains(Regex("(?i)%0[ad]")))
        "https://$host$path"
    }

    private fun validateJpeg(bytes: ByteArray) {
        fun unsigned(index: Int) = bytes[index].toInt() and 255
        fun word(index: Int) = (unsigned(index) shl 8) or unsigned(index + 1)
        require(bytes.size >= 12 && unsigned(0) == 255 && unsigned(1) == 216)
        require(unsigned(bytes.size - 2) == 255 && unsigned(bytes.size - 1) == 217)
        var position = 2
        var components = 0
        var scans = 0
        var entropyBytes = 0
        while (position < bytes.size) {
            require(unsigned(position++) == 255)
            while (position < bytes.size && unsigned(position) == 255) position++
            require(position < bytes.size)
            val marker = unsigned(position++)
            if (marker == 0xd9) {
                require(position == bytes.size && scans > 0 && entropyBytes > 0)
                return
            }
            require(
                marker !in listOf(0, 0xd8) && marker !in 0xd0..0xd7 && position + 2 <= bytes.size
            )
            val length = word(position)
            require(length >= 2 && position + length <= bytes.size - 2)
            if (marker in listOf(0xc0, 0xc1, 0xc2)) {
                require(components == 0 && length >= 8 && unsigned(position + 2) == 8)
                val height = word(position + 3)
                val width = word(position + 5)
                components = unsigned(position + 7)
                require(components in listOf(1, 3) && length == 8 + 3 * components)
                require(width in 1..2048 && height in 1..2048)
            } else if (marker in 0xc0..0xcf && marker !in listOf(0xc4, 0xc8, 0xcc)) {
                throw IllegalArgumentException()
            }
            if (marker == 0xda) {
                require(components > 0 && length >= 6)
                val scanComponents = unsigned(position + 2)
                require(scanComponents in 1..components && length == 6 + 2 * scanComponents)
                scans++
                position += length
                while (position < bytes.size - 2) {
                    if (unsigned(position) != 255) {
                        entropyBytes++
                        position++
                        continue
                    }
                    require(position + 1 < bytes.size)
                    val next = unsigned(position + 1)
                    if (next == 0 || next in 0xd0..0xd7) {
                        entropyBytes++
                        position += 2
                        continue
                    }
                    if (next == 255) {
                        position++
                        continue
                    }
                    break
                }
            } else {
                position += length
            }
        }
        throw IllegalArgumentException()
    }

    private inline fun <T> safely(block: () -> T): T =
        try {
            block()
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid capture.")
        }
}
