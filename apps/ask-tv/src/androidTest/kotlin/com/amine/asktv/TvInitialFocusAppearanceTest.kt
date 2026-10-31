package com.amine.asktv

import android.view.KeyEvent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Fresh route focus must be both actionable and visible, without corrective input. */
class TvInitialFocusAppearanceTest {
    @get:Rule val rule=createComposeRule()
    private var fixture:TvFlowFixture?=null
    @After fun close() { fixture?.close() }
    private fun start()=TvFlowFixture().also { fixture=it; it.render(rule) }
    private fun key(code:Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }
    private fun visibleFocus(tag:String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size==1 }
        rule.onNodeWithTag(tag).assertIsFocused()
        rule.onAllNodes(isFocused()).assertCountEquals(1)
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        val node=rule.onNodeWithTag(tag).assertIsFocused()
        val image=node.captureToImage()
        val pixels=IntArray(image.width*image.height)
        image.readPixels(pixels)
        // This 5x5 patch is centered horizontally in the top content padding.
        // At the native 96px button height, y=12..16 is below the 6px focused
        // border and above the text. It excludes corners and all raster edges.
        val centerX=image.width/2
        val insetY=image.height/8
        for(y in insetY until insetY+5) for(x in centerX-2..centerX+2) {
            assertEquals("$tag is semantically focused but its visible container is not gold at $x,$y",
                Gold.toArgb() and 0xFFFFFF,pixels[y*image.width+x] and 0xFFFFFF)
        }
    }

    @Test fun freshReadyFocusHasGoldContainer() {
        start()
        visibleFocus("ready-sign-in")
    }

    @Test fun freshSetupGuideFocusHasGoldContainer() {
        start()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("phone-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        visibleFocus("phone-setup-back")
    }

    @Test fun freshMacWaitingFocusHasGoldContainer() {
        start()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("sign-in-mac-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        visibleFocus("mac-sign-in-cancel")
    }

    @Test fun freshConnectedTerminalFocusHasGoldContainer() {
        val f=start()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5_000) { f.hosts.isNotEmpty() && rule.onAllNodesWithTag("phone-sign-in-qr").fetchSemanticsNodes().size==1 }
        f.workerAction { f.callback(f.claim()) }
        f.workerAction { f.pulse() }
        visibleFocus("phone-sign-in-done")
    }
}
