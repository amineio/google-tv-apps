package com.amine.familyhome

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FamilyProfileControllerTest {
    private class Store(var state: FamilyState = FamilyState()) : FamilyRepository {
        var writes = 0
        var fail = false
        override suspend fun load() = state
        override suspend fun save(state: FamilyState) {
            if (fail) throw IOException("disk full")
            this.state = state
            writes++
        }
    }

    @Test fun creationRequiresTheAuthenticatedSelectedParent() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load()
        val initialWrites = store.writes
        assertNull(controller.addChild("Robin"))
        controller.helperConnected(true)
        controller.select(Viewer.ELIAS)
        val childWrites = store.writes
        assertNull(controller.addChild("Robin"))
        assertEquals(childWrites, store.writes)
        controller.select(Viewer.AMINE)
        controller.setPin("1234")
        controller.chooseProfiles()
        assertFalse(controller.select(Viewer.AMINE, "4321"))
        val lockedWrites = store.writes
        assertNull(controller.addChild("Robin"))
        assertEquals(lockedWrites, store.writes)
        assertTrue(controller.select(Viewer.AMINE, "1234"))
        assertNotNull(controller.addChild("Robin"))
        assertTrue(store.writes > initialWrites)
        controller.screenOff()
        val sleepingWrites = store.writes
        assertNull(controller.addChild("Alex"))
        assertEquals(sleepingWrites, store.writes)
        assertFalse(controller.ui.value.storageFailed)
    }

    @Test fun explicitCreationPreservesTheFamilyAndStartsWithoutASession() = runTest {
        val existing = FamilyState(sessions = mapOf(Viewer.ELIAS to ChildSession(1_200_000, 421_000),
            Viewer.SOFIA to ChildSession(1_200_000, 600_000)),
            allowed = mapOf(Viewer.ELIAS to setOf("be.vrt.vrtnu")))
        val store = Store(existing)
        val controller = FamilyController(store) { 0L }
        controller.load()
        assertEquals(3, controller.ui.value.state.profiles.size)
        controller.select(Viewer.AMINE)
        controller.setPin("1234")
        val before = controller.ui.value.state

        val child = requireNotNull(controller.addChild("  Robin  "))
        val saved = controller.ui.value.state

        assertEquals(Viewer.AMINE, saved.selected)
        assertEquals(before.profiles, saved.profiles.take(3))
        assertEquals(FamilyProfile(child, "Robin", Portrait.CHILD), saved.profiles.last())
        assertEquals(before.sessions, saved.sessions)
        assertFalse(saved.sessions.containsKey(child))
        assertEquals(before.allowed, saved.allowed - child)
        assertFalse(saved.allowed.containsKey(Viewer.SOFIA))
        assertEquals(setOf("com.amine.chessmath", "com.yanka.mc.tv.prod.android", "be.vmma.vtm.zenderapp",
            "be.vrt.vrtnu", "com.spotify.tv.android", "com.amazon.amazonvideo.livingroom"), saved.allowed[child])
        assertEquals(before.pinSalt, saved.pinSalt)
        assertEquals(before.pinHash, saved.pinHash)
        assertTrue(ParentPin.accepts(saved, "1234"))
        assertEquals(before.revision + 1, saved.revision)
        assertEquals(saved, FamilyStateCodec.decode(FamilyStateCodec.encode(store.state)))
    }

    @Test fun invalidAndDuplicateNamesDoNotWriteOrFailStorage() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load(); controller.select(Viewer.AMINE)
        val before = controller.ui.value.state
        val writes = store.writes
        for (name in listOf("", "   ", "x".repeat(25), "Robin\nA", "\nRobin", "Robin\u0000", " elIAS ", "AMINE")) {
            assertNull(name, controller.addChild(name))
            assertEquals(before, controller.ui.value.state)
            assertEquals(writes, store.writes)
            assertFalse(controller.ui.value.storageFailed)
            assertNotNull(controller.ui.value.notice)
        }
        assertNotNull(controller.addChild("A"))
        assertNotNull(controller.addChild("x".repeat(24)))
        assertNull(controller.addChild(" a "))
    }

    @Test fun failedWriteLeavesProfilesAndExistingDataUnchanged() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load(); controller.select(Viewer.AMINE)
        val before = controller.ui.value.state
        store.fail = true

        assertNull(controller.addChild("Robin"))

        assertEquals(before, controller.ui.value.state)
        assertEquals(before, store.state)
        assertTrue(controller.ui.value.storageFailed)
    }

    @Test fun unknownProfileCommandsDoNotWriteOrFailStorage() = runTest {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        controller.load(); controller.helperConnected(true); controller.select(Viewer.AMINE)
        val unknown = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        val before = controller.ui.value.state
        val writes = store.writes

        assertFalse(controller.select(unknown))
        assertFalse(controller.start(unknown, 20))
        assertFalse(controller.end(unknown))
        assertFalse(controller.setAllowed(unknown, "be.vrt.vrtnu", true))

        assertEquals(before, controller.ui.value.state)
        assertEquals(writes, store.writes)
        assertFalse(controller.ui.value.storageFailed)
        assertNotNull(controller.ui.value.notice)
    }

    @Test fun newChildUsesExistingTimerAndSeparateAppRules() = runTest {
        var now = 0L
        val controller = FamilyController(Store()) { now }
        controller.load(); controller.helperConnected(true); controller.select(Viewer.AMINE)
        val child = requireNotNull(controller.addChild("Robin"))
        controller.setAllowed(child, "be.vrt.vrtnu", false)
        assertTrue(controller.select(child))
        assertFalse(controller.mayLaunch("com.spotify.tv.android"))
        assertTrue(controller.start(child, 20))
        now = 60_000
        assertTrue(controller.start(Viewer.ELIAS, 5))
        now = 120_000
        assertTrue(controller.select(child))
        val state = controller.ui.value.state
        assertEquals(1_140_000L, SessionEngine.remaining(state, child, now))
        assertEquals(240_000L, SessionEngine.remaining(state, Viewer.ELIAS, now))
        assertFalse(controller.mayLaunch("be.vrt.vrtnu"))
        assertTrue(controller.mayLaunch("com.spotify.tv.android"))
        assertTrue("be.vrt.vrtnu" in state.allowed.getValue(Viewer.ELIAS))
        val effects = mutableListOf<FamilyEffect>()
        controller.effect = { effects.add(it) }
        now = 1_200_000
        controller.pulse()
        assertEquals(listOf(FamilyEffect.Warning("Robin: 1 minute left")), effects)
        for (duration in listOf(0, 6, 65)) assertFalse(controller.start(child, duration))
        assertFalse(controller.ui.value.storageFailed)
        assertTrue(controller.start(child, 60))
        assertEquals(3_600_000L, SessionEngine.remaining(controller.ui.value.state, child, now))
    }
}
