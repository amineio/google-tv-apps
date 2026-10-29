package com.amine.familyhome

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyUsageControllerTest {
    private class Store(var state: FamilyState = FamilyState()) : FamilyRepository {
        var fail = false
        var writes = 0
        var beforeSave: (suspend () -> Unit)? = null
        override suspend fun load() = state
        override suspend fun save(state: FamilyState) {
            beforeSave?.invoke()
            if (fail) throw IOException("disk full")
            this.state = state
            writes++
        }
    }
    private class Fixture(val store: Store = Store()) {
        var now = 0L
        val controller = FamilyController(store, { now }) {
            UsageSample(now, Instant.parse("2026-10-28T10:00:00Z").toEpochMilli() + now, ZoneId.of("UTC"))
        }
        fun used(viewer: Viewer) = controller.ui.value.state.usage["2026-10-28"]?.get(viewer) ?: 0L
        suspend fun open() { controller.load(); controller.helperConnected(true) }
    }

    @Test fun parentGetsDurableTenSecondCheckpointsWithoutAChildTimer() = runTest {
        val f = Fixture(); f.open(); f.controller.select(Viewer.AMINE)
        val writes = f.store.writes
        f.now = 9_000; f.controller.pulse()
        assertEquals(writes, f.store.writes)
        f.now = 10_000; f.controller.pulse()
        assertEquals(10_000L, f.used(Viewer.AMINE))
        assertEquals(f.controller.ui.value.state, f.store.state)
    }

    @Test fun profileChangesCreditOutgoingProfileAndExpiryCapsChildUse() = runTest {
        val f = Fixture(); f.open(); f.controller.select(Viewer.AMINE)
        f.now = 12_000; f.controller.start(Viewer.ELIAS, 5)
        f.now = 400_000; f.controller.pulse()
        assertEquals(12_000L, f.used(Viewer.AMINE))
        assertEquals(300_000L, f.used(Viewer.ELIAS))
        f.now = 500_000; f.controller.chooseProfiles()
        assertEquals(300_000L, f.used(Viewer.ELIAS))
    }

    @Test fun chooserAndStandbyAndRecoveredDowntimeAreExcluded() = runTest {
        val f = Fixture(); f.open()
        f.now = 20_000; f.controller.select(Viewer.AMINE)
        f.now = 30_000; f.controller.screenOff()
        f.now = 300_000; f.controller.wake()
        assertEquals(10_000L, f.used(Viewer.AMINE))
        f.controller.select(Viewer.AMINE)
        f.now = 310_000; f.controller.pulse()
        f.now = 9_000_000; f.controller.load()
        f.controller.select(Viewer.AMINE)
        f.now += 10_000; f.controller.pulse()
        assertEquals(30_000L, f.used(Viewer.AMINE))
    }

    @Test fun helperLossCutsUsageAtCallbackEvenWhenPauseIsQueuedLater() = runTest {
        for (viewer in listOf(Viewer.AMINE, Viewer.HASNAA, Viewer.ELIAS)) {
            val f = Fixture(); f.open()
            if (viewer.isChild) f.controller.start(viewer, 20) else f.controller.select(viewer)
            f.now = 15_000; f.controller.helperConnected(false)
            f.now = 100_000; f.controller.screenOff()
            assertEquals(15_000L, f.used(viewer))
            f.now = 200_000; f.controller.helperConnected(true); f.controller.wake()
            assertEquals(15_000L, f.used(viewer))
        }
    }

    @Test fun failedSaveDoesNotPublishUsageAndRecoveryDoesNotCountItTwice() = runTest {
        val f = Fixture(); f.open(); f.controller.select(Viewer.AMINE)
        f.now = 10_000; f.controller.pulse()
        f.store.fail = true
        f.now = 20_000; f.controller.pulse()
        assertEquals(10_000L, f.used(Viewer.AMINE))
        assertTrue(f.controller.ui.value.storageFailed)
        f.store.fail = false
        f.now = 100_000; f.controller.load(); f.controller.select(Viewer.AMINE)
        f.now = 110_000; f.controller.pulse()
        assertEquals(20_000L, f.used(Viewer.AMINE))
    }

    @Test fun helperLossDuringSaveSurvivesReconnectBeforeQueuedPause() = runTest {
        val f = Fixture(); f.open(); f.controller.select(Viewer.AMINE)
        val resume = CompletableDeferred<Unit>()
        f.store.beforeSave = { resume.await() }
        f.now = 10_000
        val checkpoint = launch { f.controller.pulse() }
        runCurrent()
        f.now = 15_000; f.controller.helperConnected(false)
        f.now = 20_000
        val pause = launch { f.controller.screenOff() }
        runCurrent()
        f.now = 80_000; f.controller.helperConnected(true)
        f.now = 100_000; f.store.beforeSave = null; resume.complete(Unit)
        checkpoint.join(); pause.join()
        assertEquals(15_000L, f.used(Viewer.AMINE))
        assertNull(f.controller.ui.value.state.selected)
        assertTrue(f.controller.ui.value.helper)
        f.controller.select(Viewer.AMINE)
        f.now = 110_000; f.controller.pulse()
        assertEquals(25_000L, f.used(Viewer.AMINE))
    }

    @Test fun standbyDuringSaveKeepsTheScreenOffObservationBeforeMutexWait() = runTest {
        val f = Fixture(); f.open(); f.controller.select(Viewer.AMINE)
        val resume = CompletableDeferred<Unit>()
        f.store.beforeSave = { resume.await() }
        f.now = 10_000
        val checkpoint = launch { f.controller.pulse() }
        runCurrent()
        f.now = 15_000
        val pause = launch { f.controller.screenOff() }
        runCurrent()
        f.now = 100_000; f.store.beforeSave = null; resume.complete(Unit)
        checkpoint.join(); pause.join()
        assertEquals(15_000L, f.used(Viewer.AMINE))
        assertNull(f.controller.ui.value.state.selected)
    }
}
