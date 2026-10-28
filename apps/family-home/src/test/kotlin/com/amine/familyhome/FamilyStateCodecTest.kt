package com.amine.familyhome

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class FamilyStateCodecTest {
    private val legacy = """
        {
          "schema":1,
          "selected":"ELIAS",
          "sessions":{
            "ELIAS":{"totalMs":1200000,"remainingMs":421000,"deadlineElapsedMs":900000,"warnedFive":true,"warnedOne":false},
            "SOFIA":{"totalMs":1200000,"remainingMs":600000,"deadlineElapsedMs":null,"warnedFive":false,"warnedOne":true}
          },
          "allowed":{"ELIAS":["be.vrt.vrtnu","com.spotify.tv.android"]},
          "pinSalt":"preserved-salt",
          "pinHash":"preserved-hash",
          "revision":23,
          "recovered":true
        }
    """.trimIndent()

    @Test fun migrationChangesOnlySchemaAndAddsTheThreeProfileRecords() {
        val migrated = FamilyStateCodec.decode(legacy)
        val original = Json.parseToJsonElement(legacy).jsonObject
        val encoded = Json.parseToJsonElement(FamilyStateCodec.encode(migrated)).jsonObject

        assertEquals(2, migrated.schema)
        assertEquals(listOf(Viewer.AMINE, Viewer.ELIAS, Viewer.SOFIA), migrated.profiles.map { it.viewer })
        assertEquals(listOf("Amine", "Elias", "Sofia"), migrated.profiles.map { it.name })
        assertEquals(listOf(Portrait.AMINE, Portrait.ELIAS, Portrait.SOFIA), migrated.profiles.map { it.portrait })
        for ((field, value) in original) if (field != "schema") assertEquals(field, value, encoded[field])
        assertEquals(Viewer.ELIAS, migrated.selected)
        assertEquals(ChildSession(1_200_000, 421_000, 900_000, true, false), migrated.sessions[Viewer.ELIAS])
        assertEquals(ChildSession(1_200_000, 600_000, null, false, true), migrated.sessions[Viewer.SOFIA])
        assertEquals(setOf(Viewer.ELIAS), migrated.allowed.keys)
        assertFalse(migrated.allowed.containsKey(Viewer.SOFIA))

        val recovered = SessionEngine.recover(migrated)
        assertNull(recovered.selected)
        assertNull(recovered.sessions.getValue(Viewer.ELIAS).deadlineElapsedMs)
        assertEquals(421_000L, recovered.sessions.getValue(Viewer.ELIAS).remainingMs)
        assertEquals(24L, recovered.revision)
    }

    @Test fun schemaTwoRoundTripKeepsDynamicIdentityMetadataAndSeparateMaps() {
        val child = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        val state = FamilyState(profiles = defaultProfiles() + FamilyProfile(child, "Robin"),
            selected = child,
            sessions = mapOf(child to ChildSession(600_000, 240_000, 500_000),
                Viewer.ELIAS to ChildSession(1_200_000, 900_000)),
            allowed = mapOf(child to setOf("com.spotify.tv.android"), Viewer.ELIAS to emptySet()),
            pinSalt = "salt", pinHash = "hash", revision = 17, recovered = true)

        val encoded = FamilyStateCodec.encode(state)
        val restored = FamilyStateCodec.decode(encoded)

        assertEquals(state, restored)
        assertEquals("Robin", restored.label(child))
        assertEquals(Portrait.CHILD, restored.profile(child)?.portrait)
        assertEquals(listOf(Viewer.ELIAS, Viewer.SOFIA, child), restored.children.map { it.viewer })
        assertEquals("child_49d15187-5685-4722-bfb0-f340176fd340", restored.selected?.id)
        assertEquals(JsonPrimitive("AMINE"), Json.parseToJsonElement(encoded).jsonObject["profiles"]
            ?.jsonArray?.first()?.jsonObject?.get("viewer"))
    }

    @Test fun schemaTwoRequiresExplicitProfilesAndRejectsUnknownSchemas() {
        val saved = Json.parseToJsonElement(FamilyStateCodec.encode(FamilyState())).jsonObject
        assertThrows(IllegalArgumentException::class.java) {
            FamilyStateCodec.decode(JsonObject(saved - "profiles").toString())
        }
        for (schema in listOf(JsonPrimitive(3), JsonPrimitive("2"))) {
            assertThrows(IllegalArgumentException::class.java) {
                FamilyStateCodec.decode(JsonObject(saved + ("schema" to schema)).toString())
            }
        }
    }

    @Test fun rejectsInvalidProfileListsAndReferences() {
        val state = FamilyState()
        val unknown = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        val invalid = listOf(
            state.copy(profiles = state.profiles.drop(1)),
            state.copy(profiles = state.profiles + state.profiles.last()),
            state.copy(profiles = state.profiles + FamilyProfile(Viewer("Robin"), "Robin")),
            state.copy(profiles = state.profiles + FamilyProfile(unknown, "elias")),
            state.copy(profiles = state.profiles + FamilyProfile(unknown, "\nRobin")),
            state.copy(selected = unknown),
            state.copy(sessions = mapOf(unknown to ChildSession(600_000, 300_000))),
            state.copy(allowed = mapOf(unknown to setOf("com.spotify.tv.android"))),
            state.copy(allowed = mapOf(Viewer.AMINE to emptySet())),
            state.copy(sessions = mapOf(Viewer.AMINE to ChildSession(600_000, 300_000))),
        )
        invalid.forEach { saved ->
            assertThrows(IllegalArgumentException::class.java) { saved.validated() }
        }
    }

    @Test fun durableFormatRetainsItsSizeLimit() {
        assertThrows(IllegalArgumentException::class.java) { FamilyStateCodec.decode(" ".repeat(65_537)) }
        assertThrows(IllegalArgumentException::class.java) {
            FamilyStateCodec.encode(FamilyState(pinSalt = "salt", pinHash = "x".repeat(65_537)))
        }
    }
}
