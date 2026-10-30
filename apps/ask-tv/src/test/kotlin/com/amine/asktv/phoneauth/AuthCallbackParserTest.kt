package com.amine.asktv.phoneauth

import org.junit.Assert.*
import org.junit.Test

class AuthCallbackParserTest {
    @Test fun decodesFormOnceAndKeepsOAuthValues() {
        assertEquals(mapOf("state" to "expected", "code" to "a+b c", "client_id" to "issued"),
            AuthCallbackParser.parseTarget("/auth/callback?state=expected&code=a%2Bb+c&client_id=issued"))
        assertEquals("%2B", AuthCallbackParser.parseTarget("/auth/callback?code=%252B")["code"])
        assertEquals("café", AuthCallbackParser.parseTarget("/auth/callback?code=caf%C3%A9")["code"])
    }

    @Test fun rejectsWrongPathsAbsoluteTargetsFragmentsAndMalformedFields() {
        val targets = listOf("/start?state=x", "http://127.0.0.1:1455/auth/callback?state=x", "//host/auth/callback?state=x",
            "/auth/%63allback?state=x", "/auth/callback/?state=x", "/auth/callback?state=x#fragment",
            "/auth/callback?state=x&%73tate=y", "/auth/callback?code=x&code=y", "/auth/callback?code=%",
            "/auth/callback?code=%0G", "/auth/callback?code=%C3%28", "/auth/callback?code=%FF",
            "/auth/callback?code=%C0%AF", "/auth/callback?code=%ED%A0%80", "/auth/callback?code=%00",
            "/auth/callback?code=x%0D%0AX-Test:y", "/auth/callback?code=x y", "/auth/callback?=x",
            "/auth/callback?code=x&&state=y", "/auth/callback?code=x&", "/auth/callback?code")
        for (target in targets) {
            val error = assertThrows(IllegalArgumentException::class.java) { AuthCallbackParser.parseTarget(target) }
            assertEquals("Invalid authorization callback.", error.message)
        }
    }

    @Test fun enforcesCallbackTargetByteLimitBeforeDecoding() {
        val prefix = "/auth/callback?code="
        assertEquals(7168 - prefix.length, AuthCallbackParser.parseTarget(prefix + "a".repeat(7168 - prefix.length))["code"]!!.length)
        assertThrows(IllegalArgumentException::class.java) { AuthCallbackParser.parseTarget(prefix + "a".repeat(7169 - prefix.length)) }
        assertThrows(IllegalArgumentException::class.java) { AuthCallbackParser.parseTarget(prefix + "é".repeat(4000)) }
    }
}
