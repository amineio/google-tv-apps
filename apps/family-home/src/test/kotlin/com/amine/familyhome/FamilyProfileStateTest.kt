package com.amine.familyhome

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FamilyProfileStateTest {
    @Test fun schemaTwoCustomChildResumesItsOwnBalanceAndAllowedApps() {
        val saved = Json { ignoreUnknownKeys = true }.decodeFromString<FamilyState>("""
            {
              "schema": 2,
              "profiles": [
                {"viewer":"AMINE","name":"Amine","portrait":"AMINE"},
                {"viewer":"ELIAS","name":"Elias","portrait":"ELIAS"},
                {"viewer":"SOFIA","name":"Sofia","portrait":"SOFIA"},
                {"viewer":"child_49d15187-5685-4722-bfb0-f340176fd340","name":"Robin","portrait":"CHILD"}
              ],
              "selected": null,
              "sessions": {
                "ELIAS": {"totalMs":1200000,"remainingMs":900000},
                "child_49d15187-5685-4722-bfb0-f340176fd340": {"totalMs":600000,"remainingMs":420000}
              },
              "allowed": {
                "ELIAS": ["be.vrt.vrtnu"],
                "child_49d15187-5685-4722-bfb0-f340176fd340": ["com.spotify.tv.android"]
              },
              "pinSalt": "",
              "pinHash": "",
              "revision": 7,
              "recovered": true
            }
        """.trimIndent()).validated()
        val child = saved.sessions.keys.single { it != Viewer.ELIAS }

        val resumed = SessionEngine.select(saved, child, 100_000)

        assertTrue(child.isChild)
        assertEquals(child, resumed.selected)
        assertEquals(419_000L, SessionEngine.remaining(resumed, child, 101_000))
        assertEquals(900_000L, SessionEngine.remaining(resumed, Viewer.ELIAS, 101_000))
        assertEquals(AccessDecision.ALLOW,
            AccessPolicy.decide(resumed, "com.spotify.tv.android", "Player", 101_000))
        assertEquals(AccessDecision.CHILD_HOME,
            AccessPolicy.decide(resumed, "be.vrt.vrtnu", "Player", 101_000))
    }
}
