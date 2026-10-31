package com.amine.asktv

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.view.KeyEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class AskTvNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** Inject through Android so dialogs and Activity Back dispatch follow the real input route. */
    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }

    @Test fun initialFocusAndDirectionalExamples() {
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("type").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("example-0").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("example-1").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithText("Built-in example · no live model used").assertExists()
    }

    @Test fun configurationAndBackRestoreFocus() {
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("configuration").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithText("Continue with ChatGPT").assertExists()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("configuration").assertIsFocused()
    }

    private fun waitForIme(automation: UiAutomation, visible: Boolean) {
        rule.waitUntil(timeoutMillis = 5_000) {
            automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } == visible
        }
        // Compose idleness does not cover events and animations in the separate IME window.
        automation.waitForIdle(200, 5_000)
        rule.waitForIdle()
    }

    @Test fun typedDialogAndBackRestoreFocus() {
        val automation = InstrumentationRegistry.getInstrumentation()
            .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val serviceInfo = automation.serviceInfo
        val originalFlags = serviceInfo.flags
        serviceInfo.flags = originalFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = serviceInfo
        try {
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            rule.onNodeWithTag("type").assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            rule.onNodeWithTag("question-input").assertExists()
            waitForIme(automation, visible = true)

            rule.onNodeWithTag("question-input").performTextInput("What is two plus two?")
            // Wait for actual keyboard dismissal before navigating to Ask or canceling.
            key(KeyEvent.KEYCODE_BACK)
            waitForIme(automation, visible = false)
            rule.onNodeWithTag("question-input").assertExists()
            rule.onNodeWithTag("question-input").assertIsFocused()
            rule.onNodeWithText("OK  Edit · Down  Ask · Back  Cancel").assertExists()
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            rule.onNodeWithTag("question-submit").assertIsFocused()
            key(KeyEvent.KEYCODE_BACK)
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onAllNodesWithTag("question-input").fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithTag("question-input").assertDoesNotExist()
            rule.onNodeWithTag("type").assertIsFocused()
        } finally {
            serviceInfo.flags = originalFlags
            automation.serviceInfo = serviceInfo
        }
    }

    @Test fun answerScrollUsesDirectionalInput() {
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("example-0").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("answer-scroll").assertIsFocused()
        repeat(12) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithTag("new-conversation").assertExists()
    }
}
