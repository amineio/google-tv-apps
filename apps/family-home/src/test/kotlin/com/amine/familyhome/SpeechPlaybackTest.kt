package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test

class SpeechPlaybackTest {
    private class Output : SpeechOutput {
        lateinit var initialized: (Boolean) -> Unit
        lateinit var lost: () -> Unit
        lateinit var done: () -> Unit
        var focus = true
        var accepted = true
        var spoken: String? = null
        var stops = 0
        var releases = 0
        var closes = 0
        override fun initialize(ready: (Boolean) -> Unit) { initialized = ready }
        override fun requestFocus(onLoss: () -> Unit): Boolean { lost = onLoss; return focus }
        override fun speak(text: String, finished: () -> Unit): Boolean { spoken = text; done = finished; return accepted }
        override fun stop() { stops++ }
        override fun abandonFocus() { releases++ }
        override fun close() { closes++ }
    }
    @Test fun cancellationRejectsDelayedInitializationAndCleansOnce() {
        val output = Output(); var finished = 0
        val playback = SpeechPlayback(output, { true }, { "Robin, your time is up." }) { finished++ }
        playback.start(); playback.cancel(); output.initialized(true)
        assertNull(output.spoken)
        assertEquals(1, output.closes); assertEquals(1, output.releases); assertEquals(1, finished)
    }
    @Test fun invalidRequestAfterInitializationCannotRequestSpeech() {
        val output = Output(); var valid = true
        val playback = SpeechPlayback(output, { valid }, { "pending" }) {}
        playback.start(); valid = false; output.initialized(true)
        assertNull(output.spoken); assertEquals(1, output.closes)
    }
    @Test fun deniedFocusNeverSpeaksAndReleasesResources() {
        val output = Output().apply { focus = false }
        val playback = SpeechPlayback(output, { true }, { "pending" }) {}
        playback.start(); output.initialized(true)
        assertNull(output.spoken); assertEquals(1, output.closes); assertEquals(1, output.releases)
    }
    @Test fun focusLossStopsSpeechAndLateCompletionDoesNotCleanTwice() {
        val output = Output(); var finished = 0
        val playback = SpeechPlayback(output, { true }, { "Robin, your time is up." }) { finished++ }
        playback.start(); output.initialized(true)
        assertEquals("Robin, your time is up.", output.spoken)
        output.lost(); output.done()
        assertEquals(1, output.stops); assertEquals(1, output.closes); assertEquals(1, finished)
    }
    @Test fun initializationAndSynthesisFailureFinishWithoutEscaping() {
        for (failure in listOf("init", "speak")) {
            val output = Output().apply { accepted = failure != "speak" }
            val playback = SpeechPlayback(output, { true }, { "pending" }) {}
            playback.start(); output.initialized(failure != "init")
            assertEquals(1, output.closes)
        }
    }
}
