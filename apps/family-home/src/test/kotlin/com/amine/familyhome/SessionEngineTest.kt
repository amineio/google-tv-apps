package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test

class SessionEngineTest {
    @Test fun unknownProfilesCannotChangeSessionsOrUseApps() {
        val unknown = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
        val state = FamilyState()
        assertThrows(IllegalArgumentException::class.java) { SessionEngine.select(state, unknown, 0) }
        assertThrows(IllegalArgumentException::class.java) { SessionEngine.start(state, unknown, 20, 0) }
        assertThrows(IllegalArgumentException::class.java) { SessionEngine.end(state, unknown, 0) }
        assertFalse(SessionEngine.canUseApps(state.copy(selected = unknown), 0))
    }

    @Test fun newChildNeedsTimerBeforeApps() {
        val selected = SessionEngine.select(FamilyState(), Viewer.ELIAS, 100)
        assertFalse(SessionEngine.canUseApps(selected, 100))
        val started = SessionEngine.start(selected, Viewer.ELIAS, 20, 100)
        assertEquals(1_200_000, SessionEngine.remaining(started, Viewer.ELIAS, 100))
        assertTrue(SessionEngine.canUseApps(started, 100))
    }

    @Test fun rejectsDurationsOutsideFiveMinuteStepsAndOneHour() {
        for (duration in listOf(-5, 0, 1, 6, 61, 65, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                SessionEngine.start(FamilyState(), Viewer.ELIAS, duration, 0)
            }
        }
        val state = SessionEngine.start(FamilyState(), Viewer.SOFIA, 60, 0)
        assertEquals(3_600_000, SessionEngine.remaining(state, Viewer.SOFIA, 0))
        assertFalse(SessionEngine.canUseApps(state, 3_600_000))
    }

    @Test fun standbyPausesAndWakeRequiresSelection() {
        val start = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)
        val sleep = SessionEngine.suspend(start, 300_000)
        assertNull(sleep.selected)
        assertEquals(900_000, SessionEngine.remaining(sleep, Viewer.ELIAS, 9_000_000))
        assertFalse(SessionEngine.canUseApps(sleep, 9_000_000))
        val wake = SessionEngine.select(sleep, Viewer.ELIAS, 9_000_000)
        assertEquals(899_000, SessionEngine.remaining(wake, Viewer.ELIAS, 9_001_000))
    }

    @Test fun childrenKeepIndependentBalancesDuringProfileSwitches() {
        val elias = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)
        val sofia = SessionEngine.start(elias, Viewer.SOFIA, 10, 60_000)
        assertEquals(1_140_000, SessionEngine.remaining(sofia, Viewer.ELIAS, 120_000))
        val back = SessionEngine.select(sofia, Viewer.ELIAS, 120_000)
        assertEquals(540_000, SessionEngine.remaining(back, Viewer.SOFIA, 2_000_000))
        assertEquals(1_139_000, SessionEngine.remaining(back, Viewer.ELIAS, 121_000))
    }

    @Test fun selectingSameChildNeverRestartsTimer() {
        val state = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)
        val again = SessionEngine.select(state, Viewer.ELIAS, 600_000)
        assertEquals(599_000, SessionEngine.remaining(again, Viewer.ELIAS, 601_000))
    }

    @Test fun expiryKeepsProfileButNeverPermitsPlayback() {
        val state = SessionEngine.start(FamilyState(), Viewer.SOFIA, 5, 0)
        val expired = SessionEngine.tick(state, 300_000, state.revision)
        assertEquals(Viewer.SOFIA, expired.selected)
        assertEquals(0, SessionEngine.remaining(expired, Viewer.SOFIA, 300_000))
        assertFalse(SessionEngine.canUseApps(expired, 300_000))
        assertEquals(0, expired.sessions.getValue(Viewer.SOFIA).remainingMs)
    }

    @Test fun staleDeadlineCannotExpireNewSession() {
        val old = SessionEngine.start(FamilyState(), Viewer.ELIAS, 5, 0)
        val fresh = SessionEngine.start(old, Viewer.ELIAS, 20, 10_000)
        assertEquals(fresh, SessionEngine.tick(fresh, 300_000, old.revision))
        assertTrue(SessionEngine.canUseApps(fresh, 300_000))
    }

    @Test fun restartRestoresLatestCheckpointPaused() {
        val active = SessionEngine.start(FamilyState(), Viewer.SOFIA, 20, 100)
        val saved = SessionEngine.checkpoint(active, 300_100)
        val restored = SessionEngine.recover(saved)
        assertNull(restored.selected)
        assertEquals(900_000, SessionEngine.remaining(restored, Viewer.SOFIA, 8_000_000))
        assertTrue(restored.recovered)
    }

    @Test fun amineNeedsNoTimerAndPausesChild() {
        val child = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)
        val adult = SessionEngine.select(child, Viewer.AMINE, 600_000)
        assertTrue(SessionEngine.canUseApps(adult, 9_000_000))
        assertEquals(600_000, SessionEngine.remaining(adult, Viewer.ELIAS, 9_000_000))
    }

    @Test fun endingSessionDiscardsOnlyThatChildsBalance() {
        val one = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)
        val two = SessionEngine.start(one, Viewer.SOFIA, 10, 60_000)
        val ended = SessionEngine.end(two, Viewer.SOFIA, 120_000)
        assertNull(ended.selected)
        assertFalse(ended.sessions.containsKey(Viewer.SOFIA))
        assertEquals(1_140_000, SessionEngine.remaining(ended, Viewer.ELIAS, 8_000_000))
    }
}
