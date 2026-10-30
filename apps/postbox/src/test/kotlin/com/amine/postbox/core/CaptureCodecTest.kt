package com.amine.postbox.core

import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class CaptureCodecTest {
    private val codec = CaptureCodec()

    @Test
    fun preservesMultilineAndNormalizesSource() {
        val c =
            sample()
                .copy(
                    text = "one\n\ntwo",
                    sourceUrl =
                        "https://mobile.twitter.com/user/status/123?utm_source=test#fragment",
                )
        val decoded = codec.decode(codec.canonicalBytes(c))
        assertEquals("one\n\ntwo", decoded.text)
        assertEquals("https://x.com/user/status/123", decoded.sourceUrl)
    }

    @Test
    fun rejectsInvalidCapture() {
        val c = sample()
        listOf(
                c.copy(title = "x".repeat(161)),
                c.copy(text = "x".repeat(20001)),
                c.copy(id = "bad"),
                c.copy(sourceUrl = "http://x.com/u/status/1"),
                c.copy(imagesJpegBase64 = listOf("bad")),
                c.copy(imagesJpegBase64 = List(5) { "bad" }),
            )
            .forEach {
                assertThrows(IllegalArgumentException::class.java) { codec.canonicalBytes(it) }
            }
    }

    @Test
    fun rejectsOversizedEncoding() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.decode(ByteArray(4 * 1024 * 1024 + 1))
        }
    }

    @Test
    fun acceptsRealJpeg() {
        val capture =
            sample()
                .copy(
                    imagesJpegBase64 =
                        listOf(
                            "/9j/4AAQSkZJRgABAgAAAQABAAD//gAPTGF2YzYzLjEuMTAyAP/bAEMACAQEBAQEBQUFBQUFBgYGBgYGBgYGBgYGBgcHBwgICAcHBwYGBwcICAgICQkJCAgICAkJCgoKDAwLCw4ODhERFP/EAEoAAQAAAAAAAAAAAAAAAAAAAAABAQAAAAAAAAAAAAAAAAAAAAAQAQAAAAAAAAAAAAAAAAAAAAARAQAAAAAAAAAAAAAAAAAAAAD/wAARCAACAAIDASIAAhEAAxEA/9oADAMBAAIRAxEAPwAAD//Z"
                        )
                )
        assertEquals(capture, codec.decode(codec.canonicalBytes(capture)))
    }

    @Test
    fun rejectsBrokenJpegScan() {
        val bytes =
            byteArrayOf(
                0xff.toByte(),
                0xd8.toByte(),
                0xff.toByte(),
                0xc0.toByte(),
                0,
                8,
                8,
                0,
                1,
                0,
                1,
                1,
                0xff.toByte(),
                0xda.toByte(),
                0xff.toByte(),
                0xd9.toByte(),
            )
        assertThrows(IllegalArgumentException::class.java) {
            codec.canonicalBytes(
                sample()
                    .copy(
                        imagesJpegBase64 =
                            listOf(java.util.Base64.getEncoder().encodeToString(bytes))
                    )
            )
        }
    }

    @Test
    fun rejectsUnsafeSource() {
        listOf(
                "https://x.com.evil.test/a",
                "https://user@x.com/a",
                "https://instagram.com:444/a",
                "https://127.0.0.1/a",
            )
            .forEach {
                assertThrows(IllegalArgumentException::class.java) { codec.normalizeSourceUrl(it) }
            }
    }

    companion object {
        fun sample() =
            PostCapture(
                UUID.randomUUID().toString(),
                "Saved post",
                "Text",
                "",
                emptyList(),
                1700000000000,
            )
    }
}
