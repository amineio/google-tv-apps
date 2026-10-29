package com.amine.asktv

import android.view.KeyEvent
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
        rule.onNodeWithTag("ask").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("type").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("example-0").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("example-1").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithText("Example preview · not connected").assertExists()
    }

    @Test fun configurationAndBackRestoreFocus() {
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("configuration").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithText("Continue with ChatGPT").assertExists()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("configuration").assertIsFocused()
    }

    @Test fun typedDialogAndBackRestoreFocus() {
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("type").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("question-input").assertExists()
        // The first Back closes the Android keyboard while keeping the dialog open.
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("question-input").assertExists()
        rule.onNodeWithTag("question-input").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("question-input").assertDoesNotExist()
        rule.onNodeWithTag("type").assertIsFocused()
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
