package com.amine.familyhome

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VoiceAnnouncementsTest {
    private class Store(var state: FamilyState = FamilyState()) : FamilyRepository {
        var fail = false
        var waiting: CompletableDeferred<Unit>? = null
        override suspend fun load() = state
        override suspend fun save(state: FamilyState) {
            waiting?.await()
            if (fail) throw IOException("full")
            this.state = state
        }
    }
    private class Clock { var now = 0L }
    private suspend fun ready(store: Store, clock: Clock) = FamilyController(store) { clock.now }.also {
        assertTrue(it.load()); it.helperConnected(true)
    }

    @Test fun successfulStartAndResumePublishOnlyDurableBalanceAndNoAppReturnRepeat() = runTest {
        val store = Store(); val clock = Clock(); val controller = ready(store, clock)
        store.waiting = CompletableDeferred()
        val start = launch { controller.start(Viewer.ELIAS, 20) }
        runCurrent()
        assertNull(controller.voice.value)
        store.waiting!!.complete(Unit); start.join()
        val first = requireNotNull(controller.voice.value)
        assertEquals(VoicePurpose.START, first.purpose)
        assertEquals(Viewer.ELIAS, store.state.selected)
        assertEquals(1_200_000L, first.remainingMs)
        clock.now = 10_000; controller.pulse(); controller.select(Viewer.ELIAS)
        assertEquals(first, controller.voice.value)
        controller.chooseProfiles()
        assertNull(controller.voice.value)
        controller.select(Viewer.ELIAS)
        val resumed = requireNotNull(controller.voice.value)
        assertEquals(VoicePurpose.RESUME, resumed.purpose)
        assertEquals(1_190_000L, resumed.remainingMs)
        assertFalse(controller.isVoiceCurrent(first))
        assertTrue(controller.isVoiceCurrent(resumed))
    }

    @Test fun failedStoragePublishesNothingAndInvalidatesPendingSpeech() = runTest {
        val store = Store(); val controller = ready(store, Clock())
        controller.start(Viewer.ELIAS, 20)
        val old = requireNotNull(controller.voice.value)
        store.fail = true
        assertFalse(controller.start(Viewer.ELIAS, 60))
        assertNull(controller.voice.value)
        assertFalse(controller.isVoiceCurrent(old))
    }

    @Test fun helperLossDuringSaveCannotPublishOrReviveSpeech() = runTest {
        val store = Store(); val controller = ready(store, Clock())
        store.waiting = CompletableDeferred()
        val start = launch { controller.start(Viewer.ELIAS, 20) }
        runCurrent(); controller.helperConnected(false)
        store.waiting!!.complete(Unit); start.join()
        assertNull(controller.voice.value)
        controller.helperConnected(true)
        assertNull(controller.voice.value)
    }

    @Test fun standbyCancelsSpeechBeforeWaitingForAnEarlierCheckpointWrite() = runTest {
        val store = Store(); val clock = Clock(); val controller = ready(store, clock)
        controller.start(Viewer.ELIAS, 20)
        val pending = requireNotNull(controller.voice.value)
        store.waiting = CompletableDeferred()
        clock.now = 10_000
        val checkpoint = launch { controller.pulse() }
        runCurrent()
        val standby = launch { controller.screenOff() }
        runCurrent()
        assertNull(controller.voice.value)
        assertFalse(controller.isVoiceCurrent(pending))
        store.waiting!!.complete(Unit)
        checkpoint.join(); standby.join()
    }

    @Test fun staleCallbacksCannotSpeakAfterEachSessionCancellation() = runTest {
        for (reason in listOf("profile", "standby", "replace", "helper", "mute", "recovery", "end")) {
            val controller = ready(Store(), Clock())
            controller.start(Viewer.ELIAS, 20)
            val pending = requireNotNull(controller.voice.value)
            when (reason) {
                "profile" -> controller.select(Viewer.SOFIA)
                "standby" -> controller.screenOff()
                "replace" -> controller.start(Viewer.ELIAS, 20)
                "helper" -> controller.helperConnected(false)
                "mute" -> { controller.select(Viewer.AMINE); controller.setVoiceAnnouncementsEnabled(false) }
                "recovery" -> controller.load()
                "end" -> controller.end(Viewer.ELIAS)
            }
            assertFalse("A delayed callback survived $reason", controller.isVoiceCurrent(pending))
        }
    }

    @Test fun muteIsParentOnlyDurableDefaultsOnAndDoesNotReplayWhenEnabled() = runTest {
        val store = Store(); val controller = ready(store, Clock())
        assertTrue(FamilyStateCodec.decode("""{"schema":1}""").voiceAnnouncementsEnabled)
        controller.start(Viewer.ELIAS, 20)
        assertFalse(controller.setVoiceAnnouncementsEnabled(false))
        controller.select(Viewer.AMINE)
        assertTrue(controller.setVoiceAnnouncementsEnabled(false))
        val decoded = FamilyStateCodec.decode(FamilyStateCodec.encode(store.state))
        assertFalse(decoded.voiceAnnouncementsEnabled)
        controller.start(Viewer.ELIAS, 20)
        assertNull(controller.voice.value)
        controller.select(Viewer.AMINE); controller.setVoiceAnnouncementsEnabled(true)
        assertNull(controller.voice.value)
    }

    @Test fun checkpointsAndUnrelatedConfigurationPreserveCurrentToken() = runTest {
        val clock = Clock(); val controller = ready(Store(), clock)
        controller.start(Viewer.ELIAS, 20)
        val pending = requireNotNull(controller.voice.value)
        clock.now = 10_000; controller.pulse()
        controller.setAppOrder(Viewer.ELIAS, listOf("example.app"))
        assertEquals(pending, controller.voice.value)
        assertTrue(controller.isVoiceCurrent(pending))
    }

    @Test fun thresholdOverlapEmitsOnlyOneMinuteAndExpiryReplacesItOnceAtZero() = runTest {
        val clock = Clock(); val controller = ready(Store(), clock)
        controller.start(Viewer.ELIAS, 20)
        clock.now = 1_150_000; controller.pulse()
        val warning = requireNotNull(controller.voice.value)
        assertEquals(VoicePurpose.ONE_MINUTE, warning.purpose)
        assertEquals(50_000L, warning.remainingMs)
        controller.pulse(); assertEquals(warning, controller.voice.value)
        clock.now = 1_200_000; controller.pulse()
        val expired = requireNotNull(controller.voice.value)
        assertEquals(VoicePurpose.EXPIRY, expired.purpose)
        assertTrue(controller.isVoiceCurrent(expired))
        assertFalse(controller.isVoiceCurrent(warning))
        controller.pulse(); assertEquals(expired, controller.voice.value)
        controller.start(Viewer.ELIAS, 20)
        assertFalse(controller.isVoiceCurrent(expired))
    }

    @Test fun exactFiveMinuteWarningIsSkippedForFiveMinuteSession() = runTest {
        val clock = Clock(); val controller = ready(Store(), clock)
        controller.start(Viewer.ELIAS, 5)
        val start = controller.voice.value
        controller.pulse(); assertEquals(start, controller.voice.value)
        controller.start(Viewer.ELIAS, 20)
        clock.now = 900_000; controller.pulse()
        val five = requireNotNull(controller.voice.value)
        assertEquals(VoicePurpose.FIVE_MINUTES, five.purpose)
        assertEquals(300_000L, five.remainingMs)
    }

    @Test fun restoredThresholdWarningsAreNotReplayed() = runTest {
        val clock = Clock(); val store = Store(); val first = ready(store, clock)
        first.start(Viewer.ELIAS, 20)
        clock.now = 920_000; first.pulse()
        val second = ready(store, clock)
        assertNull(second.voice.value)
        second.select(Viewer.ELIAS)
        val resume = second.voice.value
        second.pulse(); assertEquals(resume, second.voice.value)
    }

    @Test fun offlineSelectionRejectsNetworkMissingDataAndMissingSupportedLanguage() {
        val english = OfflineVoice("en", Locale.US, false, emptySet())
        val dutch = OfflineVoice("nl", Locale.forLanguageTag("nl-NL"), false, emptySet())
        val network = OfflineVoice("online", Locale.FRANCE, true, emptySet())
        val missing = OfflineVoice("missing", Locale.FRANCE, false, setOf("notInstalled"))
        assertEquals(dutch, VoicePolicy.select(Locale.forLanguageTag("nl-BE"), listOf(english, dutch)))
        assertNull(VoicePolicy.select(Locale.FRANCE, listOf(english, network, missing)))
        assertEquals(english, VoicePolicy.select(Locale.GERMAN, listOf(english, dutch)))
        assertNull(VoicePolicy.select(Locale.GERMAN, listOf(dutch)))
    }

    @Test fun phrasesUseFullLocalizedSentencesAndRoundedBalance() {
        assertEquals("Robin, you have 2 minutes left.", VoicePhrases.text("Robin", VoicePurpose.START, 60_001, "en"))
        assertEquals("Robin, you have 1 minute left.", VoicePhrases.text("Robin", VoicePurpose.RESUME, 60_000, "en"))
        assertEquals("Robin, je hebt nog 20 minuten.", VoicePhrases.text("Robin", VoicePurpose.START, 1_200_000, "nl"))
        assertEquals("Robin, il te reste 1 minute.", VoicePhrases.text("Robin", VoicePurpose.RESUME, 59_000, "fr"))
        assertEquals("Robin, you have 5 minutes left.", VoicePhrases.text("Robin", VoicePurpose.FIVE_MINUTES, 300_000, "en"))
        assertEquals("Robin, you have less than 5 minutes left.", VoicePhrases.text("Robin", VoicePurpose.FIVE_MINUTES, 299_999, "en"))
        assertEquals("Robin, je hebt minder dan 1 minuut over.", VoicePhrases.text("Robin", VoicePurpose.ONE_MINUTE, 59_999, "nl"))
        assertEquals("Robin, ton temps est écoulé.", VoicePhrases.text("Robin", VoicePurpose.EXPIRY, 0, "fr"))
    }
}
