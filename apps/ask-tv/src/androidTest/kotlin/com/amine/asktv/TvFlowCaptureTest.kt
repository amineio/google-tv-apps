package com.amine.asktv

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Optional capture fixture. Synthetic screen evidence only; not MainActivity window evidence. */
class TvFlowCaptureTest {
    @get:Rule val rule = createComposeRule()
    private var fixture: TvFlowFixture? = null
    private val captureRun = "native-tv-synthetic-${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(8)}"
    @After fun close() { fixture?.close() }
    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }
    private fun tag(name: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithTag(name).fetchSemanticsNodes().isNotEmpty()
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")) {
            "Set the runner argument additionalTestOutputDir so AGP retains captures before uninstall."
        }
        require(output.isNotBlank()) { "additionalTestOutputDir must not be blank." }
        val directory = File(output, captureRun)
        check(directory.mkdirs() || directory.isDirectory)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    @Test fun captureSyntheticSignInFlowWithDirectionalInput() {
        val f = TvFlowFixture().also { fixture = it; it.render(rule) }
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
        capture("01-ready-signed-out")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        capture("02-route-choice")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        tag("phone-sign-in-qr")
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
        // The QR is synthetic: no listener is bound and the host is a fixed test address.
        capture("03-phone-synthetic-qr")
        f.workerAction { f.claim(); f.pulse() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("phone-sign-in-qr").fetchSemanticsNodes().isEmpty() }
        capture("04-phone-claimed")
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        capture("05-phone-canceled")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        tag("phone-sign-in-qr")
        capture("06-phone-retry-synthetic-qr")
        f.exchangeScopes = setOf("openid", "profile", "email")
        f.workerAction { f.callback(f.claim()) }
        f.workerAction { f.pulse() }
        tag("phone-sign-in-enable-plan")
        capture("07-plan-required")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("sign-in-mac-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        tag("mac-sign-in-cancel")
        capture("08-mac-waiting")
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("sign-in-retry").assertIsFocused()
        capture("09-mac-canceled")
    }

    @Test fun captureConnectedControlsAndAnswerWithSyntheticGrant() {
        val f=TvFlowFixture().also { fixture=it; it.render(rule) }
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        tag("phone-sign-in-qr")
        f.workerAction { f.callback(f.claim()) }
        f.workerAction { f.pulse() }
        tag("phone-sign-in-done")
        rule.onNodeWithTag("phone-sign-in-done").assertIsFocused()
        capture("10-connected-terminal")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("ask").assertIsFocused()
        capture("11-connected-ready")
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("configuration").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5_000) { f.model.ui.busy==null && f.model.ui.models.isNotEmpty() }
        rule.onNodeWithTag("change-model").assertIsFocused()
        capture("12-connected-configuration")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        tag("model-option-synthetic-model")
        rule.onNodeWithTag("model-option-synthetic-model").assertIsFocused()
        capture("13-model-chooser")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("change-model").assertIsFocused()
        capture("14-configuration-after-model")
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("configuration").assertIsFocused()
        capture("15-ready-after-configuration")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("ask").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("type").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("example-0").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("answer-scroll").assertIsFocused()
        capture("16-answer-reading-top")
        var moves=0
        while(rule.onAllNodes(hasTestTag("answer-scroll") and isFocused()).fetchSemanticsNodes().isNotEmpty() && moves++<60) {
            key(KeyEvent.KEYCODE_DPAD_DOWN)
        }
        rule.onNodeWithTag("follow-up").assertIsFocused()
        capture("17-answer-end-actions")
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("answer-scroll").assertIsFocused()
        capture("18-answer-return-focus")
    }
}
