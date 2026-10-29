package com.amine.familyhome

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Native settings and real durable commands; no speech engine is replaced by a fake. */
class VoiceAnnouncementsPresentationTest {
    @get:Rule val screen = createComposeRule()
    private class Store : FamilyRepository {
        var text = FamilyStateCodec.encode(FamilyState())
        override suspend fun load() = FamilyStateCodec.decode(text)
        override suspend fun save(state: FamilyState) { text = FamilyStateCodec.encode(state) }
    }
    private fun key(key: Key) {
        screen.onRoot().performKeyInput { pressKey(key) }; screen.waitForIdle()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }
    private fun capture() {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("voice-settings-native-20261029.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun remoteMutePersistsAndSetupReturnAndRecreationRestoreFocus() {
        val store = Store()
        val controller = FamilyController(store) { 0L }
        runBlocking { controller.load(); controller.select(Viewer.AMINE) }
        var setup = 0
        var resume by mutableIntStateOf(0)
        val restore = StateRestorationTester(screen)
        restore.setContent {
            val ui by controller.ui.collectAsState()
            val scope = rememberCoroutineScope()
            FamilyTheme { FamilyBackdrop {
                SettingsScreen(ui, emptyList(), true, true, resume, onClose = {}, onPin = {},
                    onAccess = { _, _, _ -> }, onAddChild = controller::addChild, onSetup = {}, onHome = {},
                    onVoiceEnabled = { enabled -> scope.launch { controller.setVoiceAnnouncementsEnabled(enabled) } },
                    onSpeechSetup = { setup++ })
            } }
        }
        screen.onNodeWithTag("setting:profiles").assertIsFocused()
        repeat(4) { key(Key.DirectionDown) }
        screen.onNodeWithTag("setting:voice").assertIsFocused().assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:voice-enabled").assertIsFocused().assertIsOn()
        key(Key.DirectionCenter)
        screen.waitUntil { !controller.ui.value.state.voiceAnnouncementsEnabled }
        screen.onNodeWithTag("setting:voice-enabled").assertIsOff()
        runBlocking { assertFalse(store.load().voiceAnnouncementsEnabled) }
        screen.onNodeWithText("Muted").assertIsDisplayed()
        capture()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        assertEquals(1, setup)
        screen.runOnIdle { resume++ }
        screen.onNodeWithTag("setting:speech-setup").assertIsFocused()
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("setting:speech-setup").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:voice").assertIsFocused().assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:speech-setup").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionCenter)
        screen.waitUntil { controller.ui.value.state.voiceAnnouncementsEnabled }
        screen.onNodeWithTag("setting:voice-enabled").assertIsOn()
    }
}
