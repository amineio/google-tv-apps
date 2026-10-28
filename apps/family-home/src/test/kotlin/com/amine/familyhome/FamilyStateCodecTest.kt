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

        assertEquals(4, migrated.schema)
        assertTrue(migrated.usage.isEmpty())
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
        for (schema in listOf(JsonPrimitive(5), JsonPrimitive("2"))) {
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

    @Test fun schemaTwoMigrationPreservesEveryExistingField() {
        val child = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        val saved = Json.parseToJsonElement(legacy).jsonObject + mapOf(
            "schema" to JsonPrimitive(2),
            "profiles" to Json { encodeDefaults = true }.encodeToJsonElement(defaultProfiles() + FamilyProfile(child, "Robin")),
        )
        val migrated = FamilyStateCodec.decode(JsonObject(saved).toString())
        val encoded = Json.parseToJsonElement(FamilyStateCodec.encode(migrated)).jsonObject
        assertEquals(4, migrated.schema)
        assertTrue(migrated.usage.isEmpty())
        for ((field, value) in saved) if (field != "schema") assertEquals(field, value, encoded[field])
    }

    @Test fun usageRoundTripAndValidationKeepKnownProfilesAndCanonicalDates() {
        val saved = FamilyState(usage = mapOf("2026-10-28" to mapOf(Viewer.AMINE to 12_345L, Viewer.ELIAS to 50L)))
        assertEquals(saved, FamilyStateCodec.decode(FamilyStateCodec.encode(saved)))
        val unknown = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        for (usage in listOf(
            mapOf("2026-02-30" to mapOf(Viewer.AMINE to 1L)),
            mapOf("2026-1-01" to mapOf(Viewer.AMINE to 1L)),
            mapOf("2026-10-28" to mapOf(unknown to 1L)),
            mapOf("2026-10-28" to mapOf(Viewer.AMINE to -1L)),
        )) assertThrows(IllegalArgumentException::class.java) { saved.copy(usage = usage).validated() }
    }

    @Test fun oldHistoryIsPrunedBeforeItCanMakeCoreStateUnsavable() {
        val core = FamilyState(pinSalt = "salt", pinHash = "x".repeat(64_000))
        assertTrue(FamilyStateCodec.encode(core).length <= 65_536)
        val dates = (1..30).associate { day -> "2026-09-%02d".format(day) to mapOf(
            Viewer.AMINE to 10_000L, Viewer.ELIAS to 20_000L, Viewer.SOFIA to 30_000L) }
        val prepared = FamilyStateCodec.prepareForSave(core.copy(usage = dates))
        assertTrue(prepared.usage.size < dates.size)
        assertEquals(core, prepared.copy(usage = emptyMap()))
        assertTrue(FamilyStateCodec.encode(prepared).length <= 65_536)
        assertEquals(prepared, FamilyStateCodec.decode(FamilyStateCodec.encode(prepared)))
        if (prepared.usage.isNotEmpty()) assertTrue(prepared.usage.containsKey("2026-09-30"))
    }

    @Test fun emptyUsageDoesNotIncreaseTheCoreWireSize() {
        val small = FamilyState(pinSalt = "salt", pinHash = "hash")
        val smallText = FamilyStateCodec.encode(small)
        assertFalse(Json.parseToJsonElement(smallText).jsonObject.containsKey("usage"))
        val exact = small.copy(pinHash = "x".repeat(65_536 - smallText.length + 4))
        assertEquals(65_536, FamilyStateCodec.encode(exact).length)
        val prepared = FamilyStateCodec.prepareForSave(exact.copy(
            usage = mapOf("2026-10-28" to mapOf(Viewer.AMINE to 1L))))
        assertTrue(prepared.usage.isEmpty())
        assertEquals(exact, prepared)
    }
}
