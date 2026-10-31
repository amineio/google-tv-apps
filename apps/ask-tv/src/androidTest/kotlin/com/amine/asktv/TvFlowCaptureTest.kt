package com.amine.asktv

import android.graphics.Bitmap
import android.app.UiAutomation
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inspector.WindowInspector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
        // Semantics focus can settle before its animated color reaches the display.
        // Advance animations, then await actual hardware frame submission for every app window.
        // This never requests focus or changes production state to improve a capture.
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var committed:CountDownLatch
        instrumentation.runOnMainSync {
            val windows=WindowInspector.getGlobalWindowViews().filter { it.isAttachedToWindow && it.isShown && it.isHardwareAccelerated }
            check(windows.isNotEmpty()) { "No rendered app window to capture." }
            committed=CountDownLatch(windows.size)
            windows.forEach { view ->
                view.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
                view.postInvalidateOnAnimation()
            }
        }
        check(committed.await(5,TimeUnit.SECONDS)) { "App window did not submit its capture frame." }
        instrumentation.waitForIdleSync()
        val output = requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")) {
            "Set the runner argument additionalTestOutputDir so AGP retains captures before uninstall."
        }
        require(output.isNotBlank()) { "additionalTestOutputDir must not be blank." }
        val directory = File(output, captureRun)
        check(directory.mkdirs() || directory.isDirectory)
        val bitmap = checkNotNull(instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot())
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
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("phone-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("phone-setup-back").assertIsFocused()
        capture("02b-phone-setup-guide")
        check(f.hosts.isEmpty())
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("phone-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
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
        rule.onNodeWithTag("mac-sign-in-cancel").assertIsFocused()
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
        repeat(4) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithTag("settings-back").assertIsFocused().assertIsDisplayed()
        capture("14b-configuration-final-control")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
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
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("answer-scroll").assertIsFocused()
        capture("16b-answer-scrolled-top-edge")
        var moves=0
        while(rule.onAllNodes(hasTestTag("answer-scroll") and isFocused()).fetchSemanticsNodes().isNotEmpty() && moves++<60) {
            key(KeyEvent.KEYCODE_DPAD_DOWN)
        }
        rule.onNodeWithTag("follow-up").assertIsFocused()
        capture("17-answer-end-actions")
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("answer-scroll").assertIsFocused()
        capture("18-answer-return-focus")
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("ask").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("type").assertIsFocused()
        val automation=InstrumentationRegistry.getInstrumentation().getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val serviceInfo=automation.serviceInfo
        val originalFlags=serviceInfo.flags
        serviceInfo.flags=originalFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo=serviceInfo
        try {
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            rule.onNodeWithTag("question-input").assertIsFocused()
            waitForIme(automation,true)
            capture("19-type-ime")
            rule.onNodeWithTag("question-input").performTextInput("What is two plus two?")
            key(KeyEvent.KEYCODE_BACK)
            waitForIme(automation,false)
            rule.onNodeWithTag("question-input").assertIsFocused()
            capture("20-type-keyboard-dismissed")
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            rule.onNodeWithTag("question-submit").assertIsFocused()
            capture("21-type-ask-focused")
            key(KeyEvent.KEYCODE_BACK)
            rule.onNodeWithTag("type").assertIsFocused()
            capture("22-type-return")
        } finally {
            serviceInfo.flags=originalFlags
            automation.serviceInfo=serviceInfo
        }
    }
    private fun waitForIme(automation:UiAutomation,visible:Boolean) {
        rule.waitUntil(5_000) { automation.windows.any { it.type==AccessibilityWindowInfo.TYPE_INPUT_METHOD }==visible }
        automation.waitForIdle(200,5_000)
        rule.waitForIdle()
    }
}
