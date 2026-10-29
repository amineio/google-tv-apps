package com.amine.familyhome

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AdultProfileTest {
    private val adult = Viewer("HASNAA")
    private val child = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
    // These are the actual three pre-migration records, independent of current defaults.
    private val oldProfiles = Json.parseToJsonElement("""[
        {"viewer":"AMINE","name":"Amine","portrait":"AMINE"},
        {"viewer":"ELIAS","name":"Elias","portrait":"ELIAS"},
        {"viewer":"SOFIA","name":"Sofia","portrait":"SOFIA"}
    ]""").jsonArray
    private fun legacy(schema: Int): JsonObject {
        val fields = mutableMapOf<String, JsonElement>(
            "schema" to JsonPrimitive(schema), "selected" to JsonPrimitive("ELIAS"),
            "sessions" to Json.parseToJsonElement("""{"ELIAS":{"totalMs":600000,"remainingMs":312345,"deadlineElapsedMs":900000,"warnedFive":true,"warnedOne":false}}"""),
            "allowed" to Json.parseToJsonElement("""{"ELIAS":["be.vrt.vrtnu"]}"""),
            "pinSalt" to JsonPrimitive("salt"), "pinHash" to JsonPrimitive("hash"),
            "revision" to JsonPrimitive(37), "recovered" to JsonPrimitive(true),
            "voiceAnnouncementsEnabled" to JsonPrimitive(false))
        if (schema >= 2) fields["profiles"] = oldProfiles
        if (schema >= 3) fields["usage"] = Json.parseToJsonElement("""{"2026-10-28":{"AMINE":777,"ELIAS":999}}""")
        if (schema >= 4) fields["appOrder"] = Json.parseToJsonElement("""{"ELIAS":["be.vrt.vrtnu"],"AMINE":["com.spotify.tv.android"]}""")
        return JsonObject(fields)
    }

    @Test fun allLegacySchemasKeepEveryExistingFieldAndAddExactlyOneAdult() {
        for (schema in 1..4) {
            val old = legacy(schema)
            val state = FamilyStateCodec.decode(old.toString())
            assertEquals(5, state.schema)
            assertEquals(listOf("AMINE", "ELIAS", "SOFIA", "HASNAA"), state.profiles.map { it.viewer.id })
            assertEquals("Hasnaa", state.label(adult))
            assertFalse(adult.isChild)
            assertEquals("HASNAA", state.profile(adult)?.portrait?.name)
            val wire = Json.parseToJsonElement(FamilyStateCodec.encode(state)).jsonObject
            for ((field, value) in old) if (field !in listOf("schema", "profiles")) assertEquals("schema $schema field $field", value, wire[field])
            assertEquals(state, FamilyStateCodec.decode(FamilyStateCodec.encode(state)))
        }
    }

    @Test fun existingChildNamedHasnaaKeepsItsIdentityPolicyAndBalance() {
        val old = legacy(4)
        val saved = JsonObject(old + mapOf(
            "profiles" to JsonArray(oldProfiles + Json.parseToJsonElement("""{"viewer":"${child.id}","name":"Hasnaa","portrait":"CHILD"}""")),
            "selected" to JsonPrimitive(child.id),
            "sessions" to Json.parseToJsonElement("""{"${child.id}":{"totalMs":600000,"remainingMs":300000}}"""),
            "allowed" to Json.parseToJsonElement("""{"${child.id}":["be.vrt.vrtnu"]}""")))
        val state = FamilyStateCodec.decode(saved.toString())
        assertEquals(child, state.selected)
        assertTrue(child.isChild)
        assertEquals("Hasnaa", state.label(child))
        assertEquals(Portrait.CHILD, state.profile(child)?.portrait)
        assertEquals(300000L, SessionEngine.remaining(state, child, 0))
        val running = SessionEngine.select(state, child, 0)
        assertEquals(AccessDecision.CHILD_HOME, AccessPolicy.decide(running, "com.spotify.tv.android", "Player", 0))
        assertEquals(AccessDecision.ALLOW, AccessPolicy.decide(running, "be.vrt.vrtnu", "Player", 0))
        assertEquals(state, FamilyStateCodec.decode(FamilyStateCodec.encode(state)))
        assertEquals(1, state.profiles.count { it.viewer == adult })
    }

    @Test fun exactLimitLegacyCoreRemainsSavableAfterAddingAdult() {
        val small = JsonObject(legacy(4) - "usage" - "appOrder" - "voiceAnnouncementsEnabled")
        val exact = JsonObject(small + ("pinHash" to JsonPrimitive("x".repeat(65536 - small.toString().length + 4)))).toString()
        assertEquals(65536, exact.length)
        val state = FamilyStateCodec.decode(exact)
        assertNotNull(state.profile(adult))
        val encoded = FamilyStateCodec.encode(state)
        assertTrue(encoded.length <= 65536)
        assertEquals(state, FamilyStateCodec.decode(encoded))
    }

    private class Store(var state: FamilyState = FamilyState()) : FamilyRepository {
        override suspend fun load() = state
        override suspend fun save(state: FamilyState) { this.state = state }
    }

    @Test fun secondAdultUsesExistingPinAndAllParentCommandsWithoutATimer() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        assertTrue(controller.load())
        assertTrue(controller.select(Viewer.AMINE))
        assertTrue(controller.setPin("1234"))
        assertTrue(controller.chooseProfiles())
        assertFalse(controller.select(adult, "4321"))
        assertFalse(controller.setVoiceAnnouncementsEnabled(false))
        assertTrue(controller.select(adult, "1234"))
        assertTrue(controller.mayLaunch("example.unlisted.app"))
        assertFalse(controller.ui.value.state.sessions.containsKey(adult))
        assertFalse(controller.start(adult, 20, "1234"))
        assertTrue(controller.setVoiceAnnouncementsEnabled(false))
        assertTrue(controller.setAllowed(Viewer.ELIAS, "example.allowed.app", true))
        assertTrue(controller.setAppOrder(adult, listOf("example.allowed.app")))
        assertNotNull(controller.addChild("Child One"))
        assertNull(controller.addChild("Hasnaa"))
        assertTrue(controller.setPin(null))
        assertFalse(controller.select(Viewer("UNKNOWN")))
        assertTrue(Viewer("UNKNOWN").isChild)
        assertEquals(adult, controller.ui.value.state.selected)
        controller.screenOff()
        assertFalse(controller.setPin("4321"))
    }
}
