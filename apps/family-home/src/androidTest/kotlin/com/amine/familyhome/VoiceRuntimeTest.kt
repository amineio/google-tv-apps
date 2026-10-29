package com.amine.familyhome

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.UiAutomation
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real engine and bound-helper coverage, without Compose/Espresso reconnecting UiAutomation. */
@RunWith(AndroidJUnit4::class)
class VoiceRuntimeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun waitFor(label: String, timeout: Long = 20_000, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        assertTrue(label, condition())
    }
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync(action) }
    @Suppress("DEPRECATION")
    private fun speechRunning() = context.getSystemService(ActivityManager::class.java).getRunningServices(100)
        .any { it.service.className == FamilySpeechService::class.java.name }
    private fun stopped() {
        waitFor("Speech service must stop") { !speechRunning() }
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id == 1901 })
    }
    private fun openHome() = onMain {
        context.startActivity(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
    private fun shell(automation: UiAutomation, command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    }
    private fun writeServices(automation: UiAutomation, components: String?) {
        // executeShellCommand tokenizes directly; shell quotes would become part of the setting.
        if (components.isNullOrEmpty()) shell(automation, "settings delete secure enabled_accessibility_services")
        else {
            require(components.matches(Regex("[A-Za-z0-9_.$/:]+"))) { "Invalid fixture accessibility components" }
            shell(automation, "settings put secure enabled_accessibility_services $components")
        }
    }

    @Test fun realEngineInventoriesOfflineVoicesAndSynthesizesOnlyWhenInstalled() {
        waitFor("Application state must load") { context.family.ui.value.ready }
        runBlocking { context.family.chooseProfiles() }
        stopped()
        val initialized = CountDownLatch(1)
        val initStatus = AtomicInteger(TextToSpeech.ERROR)
        val tts = TextToSpeech(context) { status -> initStatus.set(status); initialized.countDown() }
        val audio = File.createTempFile("offline-voice-probe-", ".wav", context.cacheDir)
        try {
            assertTrue("Real TTS initialization timed out", initialized.await(10, TimeUnit.SECONDS))
            assertEquals("Installed engine must initialize", TextToSpeech.SUCCESS, initStatus.get())
            val voices = tts.voices.orEmpty()
            val locale = context.resources.configuration.locales[0]
            val selected = VoicePolicy.select(locale, voices.map {
                OfflineVoice(it.name, it.locale, it.isNetworkConnectionRequired, it.features.orEmpty())
            })
            Log.i("FamilyHome.VoiceRuntime", "engine=${tts.defaultEngine} locale=${locale.toLanguageTag()} voices=${voices.size} eligible=${if (selected == null) 0 else 1}")
            if (selected == null) {
                Log.i("FamilyHome.VoiceRuntime", "synthesis=unavailable:no_installed_offline_voice")
                return
            }
            val voice = voices.single { it.name == selected.name }
            assertFalse(voice.isNetworkConnectionRequired)
            assertFalse("notInstalled" in voice.features.orEmpty())
            assertEquals(TextToSpeech.SUCCESS, tts.setVoice(voice))
            assertEquals(voice.name, tts.voice?.name)
            val finished = CountDownLatch(1)
            val result = AtomicInteger(TextToSpeech.ERROR)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { result.set(TextToSpeech.SUCCESS); finished.countDown() }
                @Deprecated("Android callback") override fun onError(id: String?) { finished.countDown() }
                override fun onError(id: String?, errorCode: Int) { finished.countDown() }
            })
            val phrase = VoicePhrases.text("Robin", VoicePurpose.START, 1_200_000, voice.locale.language)
            assertEquals(TextToSpeech.SUCCESS, tts.synthesizeToFile(phrase, Bundle(), audio, "offline-probe"))
            assertTrue("Offline synthesis timed out", finished.await(15, TimeUnit.SECONDS))
            assertEquals("Offline synthesis failed", TextToSpeech.SUCCESS, result.get())
            assertTrue("Synthesis must produce a nonempty audio file", audio.length() > 44)
            Log.i("FamilyHome.VoiceRuntime", "synthesis=completed bytes=${audio.length()}")
        } finally {
            tts.stop(); tts.shutdown(); audio.delete()
        }
    }

    @Test fun boundHelperAttemptsBackgroundSpeechAndCancelsWithoutRetainingService() {
        // UiAutomation's connection flag must precede window work and stay set throughout this test.
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val originalServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val originalEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        val helper = ComponentName(context, FamilyAccessService::class.java)
        val others = originalServices.orEmpty().split(':').filter { it.isNotBlank() && ComponentName.unflattenFromString(it) != helper }
        val store = DataStoreFamilyRepository(context)
        var original: FamilyState? = null
        try {
            waitFor("Application state must load") { context.family.ui.value.ready }
            val home = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
            Log.i("FamilyHome.VoiceRuntime", "before_rebind role_home=$home helper_active=${FamilyAccessService.active} helper_ui=${context.family.ui.value.helper}")
            assertTrue("Family Home must hold HOME for actual helper dispatch", home)
            // A runner force-stop can leave the real binding marked crashed. Rebind only this helper.
            writeServices(automation, others.joinToString(":"))
            SystemClock.sleep(300)
            writeServices(automation, (others + helper.flattenToString()).joinToString(":"))
            shell(automation, "settings put secure accessibility_enabled 1")
            waitFor("Enabled real helper must connect with HOME") {
                var active = false
                onMain { active = FamilyAccessService.active && context.family.ui.value.helper }
                active
            }
            Log.i("FamilyHome.VoiceRuntime", "helper_bound=true role_home=true")
            openHome()
            val settings = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val settingsPackage = requireNotNull(settings.resolveActivity(context.packageManager)).packageName
            val child = Viewer("child_49d15187-5685-4722-bfb0-f340176fd340")
            runBlocking {
                original = store.load()
                context.family.chooseProfiles()
                store.save(FamilyState(profiles = defaultProfiles() + FamilyProfile(child, "Robin"),
                    sessions = mapOf(child to ChildSession(1_200_000, 1_200_000)),
                    allowed = mapOf(child to setOf(settingsPackage))))
                assertTrue(context.family.load())
                assertTrue(context.family.select(Viewer.AMINE))
            }
            onMain { context.startActivity(settings) }
            waitFor("Permitted external settings app must be foreground", 5_000) {
                automation.rootInActiveWindow?.packageName?.toString() == settingsPackage
            }
            context.family.speechStatus(SpeechStatus.NOT_CHECKED)
            runBlocking { assertTrue(context.family.select(child)) }
            waitFor("Actual helper must attempt background speech") {
                context.family.ui.value.speechStatus !in setOf(SpeechStatus.NOT_CHECKED, SpeechStatus.CHECKING)
            }
            stopped()
            val status = context.family.ui.value.speechStatus
            Log.i("FamilyHome.VoiceRuntime", "background_status_after_cleanup=$status")
            assertTrue(status in setOf(SpeechStatus.READY, SpeechStatus.UNAVAILABLE, SpeechStatus.FAILED, SpeechStatus.FOCUS_DENIED))
            runBlocking { assertTrue(context.family.start(child, 20)) }
            instrumentation.waitForIdleSync()
            val cancellationObservedActiveService = speechRunning()
            val pending = requireNotNull(context.family.voice.value)
            runBlocking { assertTrue(context.family.chooseProfiles()) }
            assertFalse(context.family.isVoiceCurrent(pending))
            stopped()
            assertNotEquals(SpeechStatus.CHECKING, context.family.ui.value.speechStatus)
            Log.i("FamilyHome.VoiceRuntime", "cancel_active=$cancellationObservedActiveService service_stopped=true")
        } finally {
            runBlocking {
                context.family.chooseProfiles()
                original?.let { store.save(it); assertTrue(context.family.load()) }
            }
            openHome()
            writeServices(automation, originalServices)
            shell(automation, "settings put secure accessibility_enabled $originalEnabled")
        }
    }
}
