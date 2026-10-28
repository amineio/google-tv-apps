package com.amine.familyhome

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AppOrderTest {
    private val order = listOf("com.spotify.tv.android", "be.vrt.vrtnu")
    private val child = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")

    @Test fun schemasOneThroughThreeMigrateWithoutChangingExistingFields() {
        val state = FamilyState(selected = Viewer.ELIAS,
            sessions = mapOf(Viewer.ELIAS to ChildSession(600_000, 300_000)),
            allowed = mapOf(Viewer.ELIAS to order.toSet()),
            pinSalt = "salt", pinHash = "hash", revision = 31, recovered = true,
            usage = mapOf("2026-10-28" to mapOf(Viewer.ELIAS to 123L)))
        val wire = Json.parseToJsonElement(FamilyStateCodec.encode(state)).jsonObject
        for (schema in 1..3) {
            var legacy = wire - "appOrder" + ("schema" to JsonPrimitive(schema))
            if (schema < 3) legacy = legacy - "usage"
            if (schema == 1) legacy = legacy - "profiles"
            val migrated = FamilyStateCodec.decode(JsonObject(legacy).toString())
            assertEquals(4, migrated.schema)
            assertTrue(migrated.appOrder.isEmpty())
            val encoded = Json.parseToJsonElement(FamilyStateCodec.encode(migrated)).jsonObject
            for ((key, value) in legacy) if (key != "schema") assertEquals(key, value, encoded[key])
        }
    }

    @Test fun separateParentAndDynamicChildOrdersSurviveRoundTripAndRecovery() {
        val state = FamilyState(profiles = defaultProfiles() + FamilyProfile(child, "Robin"),
            appOrder = mapOf(Viewer.AMINE to order, Viewer.ELIAS to order.reversed(), child to order.take(1)))
        val restored = FamilyStateCodec.decode(FamilyStateCodec.encode(state))
        assertEquals(state, restored)
        assertEquals(state.appOrder, SessionEngine.recover(restored).appOrder)
    }

    @Test fun rejectsUnknownProfilesDuplicatePackagesAndInvalidPackageNames() {
        for (orders in listOf(mapOf(child to order), mapOf(Viewer.AMINE to order + order.first()),
            mapOf(Viewer.ELIAS to listOf("invalid")), mapOf(Viewer.ELIAS to listOf("com.bad-name.app")),
            mapOf(Viewer.ELIAS to listOf("com.valid.app ")))) {
            assertThrows(IllegalArgumentException::class.java) { FamilyState(appOrder = orders).validated() }
        }
    }

    @Test fun emptyOrderDoesNotIncreaseNearLimitCoreWire() {
        val small = FamilyState(pinSalt = "salt", pinHash = "hash")
        val text = FamilyStateCodec.encode(small)
        assertFalse(Json.parseToJsonElement(text).jsonObject.containsKey("appOrder"))
        val exact = small.copy(pinHash = "x".repeat(65_536 - text.length + 4))
        assertEquals(65_536, FamilyStateCodec.encode(exact).length)
        assertEquals(exact, FamilyStateCodec.decode(FamilyStateCodec.encode(exact)))
    }

    private class Store(var saved: FamilyState = FamilyState()) : FamilyRepository {
        var fail = false
        override suspend fun load() = saved
        override suspend fun save(state: FamilyState) {
            if (fail) throw IOException("disk full")
            saved = FamilyStateCodec.decode(FamilyStateCodec.encode(state))
        }
    }

    @Test fun selectedChildMaySaveOnlyOwnOrderWithoutChangingPermissionsOrSession() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        assertFalse(controller.setAppOrder(Viewer.ELIAS, order))
        assertTrue(controller.load())
        assertFalse(controller.setAppOrder(Viewer.AMINE, order))
        controller.helperConnected(true)
        assertTrue(controller.start(Viewer.ELIAS, 20))
        val before = controller.ui.value.state
        assertFalse(controller.setAppOrder(Viewer.SOFIA, order))
        assertFalse(controller.setAppOrder(child, order))
        assertFalse(controller.setAppOrder(Viewer.ELIAS, order + order.first()))
        assertFalse(controller.setAppOrder(Viewer.ELIAS, listOf("bad package")))
        assertEquals(before, controller.ui.value.state)
        assertTrue(controller.setAppOrder(Viewer.ELIAS, order))
        val after = controller.ui.value.state
        assertEquals(before.copy(appOrder = mapOf(Viewer.ELIAS to order), revision = before.revision + 1), after)
        assertEquals(after, store.saved)
    }

    @Test fun eachProfileRetainsItsOrderAcrossSelectionNewChildAndProcessRestore() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load(); controller.helperConnected(true)
        assertTrue(controller.select(Viewer.AMINE))
        assertTrue(controller.setAppOrder(Viewer.AMINE, order))
        val added = requireNotNull(controller.addChild("Robin"))
        assertTrue(controller.start(added, 20))
        assertTrue(controller.ui.value.state.appOrder[added].isNullOrEmpty())
        assertTrue(controller.setAppOrder(added, order.reversed()))
        val restored = FamilyController(store) { 100L }
        assertTrue(restored.load())
        assertEquals(mapOf(Viewer.AMINE to order, added to order.reversed()), restored.ui.value.state.appOrder)
    }

    @Test fun failedSaveKeepsPublishedAndDurableOrdersUnchanged() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load(); controller.select(Viewer.AMINE)
        assertTrue(controller.setAppOrder(Viewer.AMINE, order))
        val before = controller.ui.value.state
        store.fail = true
        assertFalse(controller.setAppOrder(Viewer.AMINE, order.reversed()))
        assertEquals(before, controller.ui.value.state)
        assertEquals(before, store.saved)
        assertTrue(controller.ui.value.storageFailed)
    }
}
