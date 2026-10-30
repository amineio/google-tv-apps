package com.amine.postbox.core

import org.junit.Assert.*
import org.junit.Test

class PairingLinkTest {
    private val valid =
        "postbox://pair?host=192.168.1.2&port=18765&pin=${"ab".repeat(32)}&token=${"a".repeat(43)}"

    @Test
    fun roundTrip() {
        assertEquals(valid, PairingLink.parse(valid).encode())
    }

    @Test
    fun rejectsUntrustedAndAmbiguousLinks() {
        listOf(
                valid.replace("postbox:", "https:"),
                valid.replace("//pair?", "//pair/path?"),
                valid + "&host=192.168.1.3",
                valid.replace("192.168.1.2", "127.0.0.1"),
                valid.replace("192.168.1.2", "8.8.8.8"),
                valid.replace("18765", "0"),
                valid.replace("ab".repeat(32), "short"),
                valid.replace("a".repeat(43), "short"),
                valid + "&extra=x",
            )
            .forEach {
                assertThrows(IllegalArgumentException::class.java) { PairingLink.parse(it) }
            }
    }
}
