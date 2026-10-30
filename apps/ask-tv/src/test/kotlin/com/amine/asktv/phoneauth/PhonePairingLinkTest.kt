package com.amine.asktv.phoneauth

import org.junit.Assert.*
import org.junit.Test

class PhonePairingLinkTest {
    private val link = PhonePairingLink("192.168.1.8", 43210, "a".repeat(64), "A".repeat(43))

    @Test fun roundTripsPrivateLanLink() {
        assertEquals(link, PhonePairingLink.parse(link.encode()))
        assertFalse(link.toString().contains(link.token))
        assertFalse(link.toString().contains(link.pin))
        for (host in listOf("10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.0.1")) {
            val local = link.copy(host = host)
            assertEquals(local, PhonePairingLink.parse(local.encode()))
        }
    }

    @Test fun rejectsPublicAndNoncanonicalAddresses() {
        for (host in listOf("8.8.8.8", "127.0.0.1", "0.0.0.0", "169.254.1.1", "172.32.0.1", "192.168.001.8", "192.168.1.256", "localhost", "[::1]")) {
            assertThrows(IllegalArgumentException::class.java) { PhonePairingLink.parse(link.encode().replace(link.host, host)) }
            assertThrows(IllegalArgumentException::class.java) { link.copy(host = host).encode() }
        }
    }

    @Test fun rejectsAmbiguousFieldsAndUrlParts() {
        val valid = link.encode()
        val bad = listOf(valid + "&token=" + link.token, valid + "&other=x", valid.replace("v=1", "v=2"),
            valid.replace("v=1&", ""), valid.replace("token=", "%74oken="), valid.replace("pair?", "pair/?"),
            valid + "#fragment", valid.replace("pair?", "user@pair?"), valid.replace("aichat:", "https:"),
            valid.replace("host=192", "host=%31%39%32"), valid.replace("port=43210", "port=043210"),
            valid.replace("port=43210", "port=65536"), valid.replace("port=43210", "port=0"),
            valid.replace("pin=" + link.pin, "pin=" + "A".repeat(64)), valid.replace("token=" + link.token, "token=%GG"),
            valid + "x".repeat(2048))
        for (value in bad) {
            val error = assertThrows(IllegalArgumentException::class.java) { PhonePairingLink.parse(value) }
            assertEquals("Invalid pairing link.", error.message)
        }
    }
}
