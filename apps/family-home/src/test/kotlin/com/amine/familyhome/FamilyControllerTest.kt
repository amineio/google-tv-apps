package com.amine.familyhome

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FamilyControllerTest {
    private class MemoryStore(var saved: FamilyState = FamilyState()) : FamilyRepository {
        var fail = false
        override suspend fun load() = saved
        override suspend fun save(state: FamilyState) {
            if (fail) throw IOException("disk full")
            saved = state
        }
    }

    @Test fun failedWriteCannotActivateChildOrReplacePreviousBalance() = runTest {
        val store = MemoryStore()
        val controller = FamilyController(store) { 0L }
        controller.load()
        controller.helperConnected(true)
        assertTrue(controller.start(Viewer.ELIAS, 20))
        val before = controller.ui.value.state
        store.fail = true
        assertFalse(controller.start(Viewer.ELIAS, 60))
        assertEquals(before, controller.ui.value.state)
        assertFalse(controller.mayLaunch("be.vrt.vrtnu"))
        assertTrue(controller.ui.value.storageFailed)
    }

    @Test fun unavailableHelperPreventsNewOrResumedChildSessions() = runTest {
        val controller = FamilyController(MemoryStore()) { 0L }
        controller.load()
        assertFalse(controller.start(Viewer.ELIAS, 20))
        assertFalse(controller.select(Viewer.ELIAS))
        assertTrue(controller.select(Viewer.AMINE))
    }

    @Test fun reloadPausesCheckpointAndRequiresProfileChoice() = runTest {
        var now = 0L
        val store = MemoryStore()
        val first = FamilyController(store) { now }
        first.load(); first.helperConnected(true); first.start(Viewer.ELIAS, 20)
        now = 300_000
        first.pulse()
        val second = FamilyController(store) { 9_000_000 }
        second.load(); second.helperConnected(true)
        assertNull(second.ui.value.state.selected)
        assertEquals(900_000, second.ui.value.state.sessions.getValue(Viewer.ELIAS).remainingMs)
        assertFalse(second.mayLaunch("be.vrt.vrtnu"))
    }

    @Test fun pinProtectsNewSessionsButNotExistingBalance() = runTest {
        var now = 0L
        val controller = FamilyController(MemoryStore()) { now }
        controller.load(); controller.helperConnected(true)
        controller.start(Viewer.ELIAS, 20)
        controller.select(Viewer.AMINE)
        assertTrue(controller.setPin("1234"))
        controller.chooseProfiles()
        assertFalse(controller.select(Viewer.AMINE, "4321"))
        assertTrue(controller.select(Viewer.ELIAS))
        assertFalse(controller.start(Viewer.ELIAS, 60))
        now = 60_000
        controller.chooseProfiles()
        assertTrue(controller.select(Viewer.ELIAS))
        assertEquals(1_140_000, SessionEngine.remaining(controller.ui.value.state, Viewer.ELIAS, now))
        assertTrue(controller.start(Viewer.SOFIA, 20, "1234"))
    }

    @Test fun childCannotChangeConfigurationWithoutAdminProfile() = runTest {
        val controller = FamilyController(MemoryStore()) { 0L }
        controller.load(); controller.helperConnected(true); controller.start(Viewer.ELIAS, 20)
        assertFalse(controller.setAllowed(Viewer.ELIAS, "com.google.android.youtube.tv", true))
        assertFalse(controller.setPin("1234"))
        controller.select(Viewer.AMINE)
        assertTrue(controller.setAllowed(Viewer.ELIAS, "com.google.android.youtube.tv", true))
    }

    @Test fun expiryEmitsPauseAndHomeOnceAfterSavingExpiredState() = runTest {
        var now = 0L
        val controller = FamilyController(MemoryStore()) { now }
        controller.load(); controller.helperConnected(true); controller.start(Viewer.ELIAS, 5)
        val effects = mutableListOf<FamilyEffect>()
        controller.effect = { effects.add(it) }
        now = 300_000
        controller.pulse()
        assertEquals(0L, controller.ui.value.state.sessions.getValue(Viewer.ELIAS).remainingMs)
        assertTrue(effects.contains(FamilyEffect.PauseMedia))
        assertTrue(effects.contains(FamilyEffect.OpenHome))
        effects.clear()
        controller.pulse()
        assertTrue(effects.isEmpty())
    }

    @Test fun standbyEndsAdminAuthorizationAndPausesChildMedia() = runTest {
        val controller = FamilyController(MemoryStore()) { 0L }
        controller.load(); controller.helperConnected(true); controller.select(Viewer.AMINE)
        controller.setPin("1234")
        controller.screenOff()
        assertFalse(controller.select(Viewer.AMINE))
        controller.start(Viewer.ELIAS, 20, "1234")
        val effects = mutableListOf<FamilyEffect>()
        controller.effect = { effects.add(it) }
        controller.screenOff()
        assertNull(controller.ui.value.state.selected)
        assertTrue(effects.contains(FamilyEffect.PauseMedia))
    }
}
