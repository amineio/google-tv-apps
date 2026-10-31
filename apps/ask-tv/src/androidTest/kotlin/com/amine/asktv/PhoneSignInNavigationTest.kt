package com.amine.asktv

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PhoneSignInNavigationTest {
    @get:Rule val rule = createComposeRule()
    private var fixture: TvFlowFixture? = null
    @After fun close() { fixture?.close() }
    private fun start(record: CredentialRecord = CredentialRecord(TvFlowFixture.HOST)): TvFlowFixture =
        TvFlowFixture(record).also { fixture = it; it.render(rule); rule.waitForIdle() }
    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }
    private fun oneFocus() { rule.onAllNodes(isFocused()).assertCountEquals(1) }
    private fun phoneRoute() {
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        rule.onNodeWithText("Use Android phone").assertExists()
        rule.onNodeWithText("Use Mac helper").assertExists()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
    }
    private fun qrVisible() = rule.waitUntil(5_000) {
        rule.onAllNodesWithTag("phone-sign-in-qr").fetchSemanticsNodes().size == 1
    }
    private fun waitTag(tag: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1
    }

    @Test fun initialRouteChoiceAndWaitingCancelUseDirectionalFocus() {
        start()
        phoneRoute()
        qrVisible()
        rule.onNodeWithTag("phone-helper-install-instructions").assertIsDisplayed()
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
        oneFocus()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("phone-sign-in-mac").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        rule.onNodeWithText("Sign-in canceled").assertExists()
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        oneFocus()
    }

    @Test fun setupGuideUsesDpadAndStartsNoRelayBeforePairing() {
        val f=start()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("phone-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("phone-setup-back").assertIsFocused()
        rule.onNodeWithTag("phone-setup-guide-qr").assertIsDisplayed()
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        f.workerAction {}
        assertTrue(f.hosts.isEmpty())
        assertNull(f.pendingPermissionGeneration)
        oneFocus()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("phone-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        qrVisible()
        assertEquals(1,f.hosts.size)
        oneFocus()
    }

    @Test fun activePairingGuideReturnsByBackWithoutReplacingOrCancelingTheAttempt() {
        val f=start()
        phoneRoute(); qrVisible()
        val host=f.hosts.single()
        val deadline=f.model.ui.phonePairing?.expiresAtElapsedMs
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("phone-sign-in-mac").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("phone-sign-in-setup").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("phone-setup-back").assertIsFocused()
        rule.onNodeWithText("Back to pairing").assertExists()
        rule.onNodeWithTag("phone-setup-guide-qr").assertIsDisplayed()
        assertEquals(1,f.hosts.size)
        assertFalse(host.closed)
        assertEquals(deadline,f.model.ui.phonePairing?.expiresAtElapsedMs)
        oneFocus()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("phone-sign-in-setup").assertIsFocused()
        qrVisible()
        assertSame(host,f.hosts.single())
        assertFalse(host.closed)
        assertEquals(deadline,f.model.ui.phonePairing?.expiresAtElapsedMs)
        oneFocus()
    }

    @Test fun guideReturnFollowsAnExternalMacRouteAndItsCanceledState() {
        val f=start()
        fun openGuideFromReady() {
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            rule.onNodeWithTag("phone-setup-back").assertIsFocused()
        }
        openGuideFromReady()
        // Same public route entry used by the existing Mac helper intent.
        rule.runOnIdle { f.model.signIn(SignInRoute.MAC) }
        rule.onNodeWithText("Back to sign-in status").assertExists()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("mac-sign-in-cancel").assertIsFocused()
        oneFocus()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("sign-in-retry").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
        openGuideFromReady()
        rule.runOnIdle { f.model.signIn(SignInRoute.MAC) }
        rule.runOnIdle { f.model.cancelSignIn() }
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("sign-in-retry").assertIsFocused()
        oneFocus()
    }

    @Test fun displayedQrDecodesToTheSyntheticAttemptAndKeepsAWhiteQuietZone() {
        val f = start()
        phoneRoute(); qrVisible()
        val qrNode=rule.onNodeWithTag("phone-sign-in-qr")
        val bounds=qrNode.fetchSemanticsNode().boundsInRoot
        val image = qrNode.captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        assertEquals(image.width, image.height)
        assertEquals("The QR must render at 512 physical pixels, not just use a 512px source",512,image.width)
        val source = com.google.zxing.RGBLuminanceSource(image.width, image.height, pixels)
        val result = com.google.zxing.qrcode.QRCodeReader().decode(
            com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source)))
        assertEquals(f.hosts.last().link.encode(), result.text)
        val bitmap=phoneQrBitmap(f.hosts.last().link.encode())
        val sourceCorner=bitmap.getPixel(0,0)
        val modules=com.google.zxing.qrcode.encoder.Encoder.encode(f.hosts.last().link.encode(),
            com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L).matrix.width
        val sourceQuiet=4*(bitmap.width/(modules+8))
        assertTrue("Source quiet zone includes four complete QR modules", sourceQuiet>=4)
        for(y in 0 until bitmap.height) for(x in 0 until bitmap.width) {
            if(x<sourceQuiet || y<sourceQuiet || x>=bitmap.width-sourceQuiet || y>=bitmap.height-sourceQuiet) {
                assertEquals("Source quiet zone at $x,$y",android.graphics.Color.WHITE,bitmap.getPixel(x,y))
            }
        }
        bitmap.recycle()
        fun rgb(x:Int,y:Int)=(pixels[y*image.width+x] and 0xFFFFFF).toString(16).padStart(6,'0')
        val diagnostics="bounds=$bounds size=${image.width}x${image.height} colorSpace=${image.colorSpace} sourceCorner=${(sourceCorner and 0xFFFFFF).toString(16)} " +
            "top=${(0..4).map { rgb(it,0) }} left=${(0..4).map { rgb(0,it) }} inner=${(1..4).map { rgb(it,it) }} " +
            "edgeMids=${listOf(rgb(image.width/2,0),rgb(0,image.height/2),rgb(image.width-1,image.height/2),rgb(image.width/2,image.height-1))}"
        // At bounds [104,285,552,733], node capture returned only the outer left/bottom
        // raster edge as F9/FDF gray; source pixels and every inner sample were FFFFFF.
        // Exclude that one-pixel capture fringe, not a general color tolerance. Check
        // full interior strips on all four sides and decode the rendered QR above.
        val strip = (image.width / 50).coerceAtLeast(2)
        val fringe=1
        for (y in fringe until image.height-fringe) for (x in fringe until image.width-fringe) {
            if (x < fringe+strip || y < fringe+strip || x >= image.width-fringe-strip || y >= image.height-fringe-strip) {
                assertEquals("Quiet zone at $x,$y; $diagnostics", 0xFFFFFF, pixels[y * image.width + x] and 0xFFFFFF)
            }
        }
    }

    @Test fun permissionDenialDoesNotStartTransportAndRetryGetsFreshAttempt() {
        val f = start()
        f.permissionAllowed = false
        phoneRoute()
        waitTag("phone-sign-in-retry")
        assertTrue(f.hosts.isEmpty())
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        f.permissionAllowed = true
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        qrVisible()
        assertEquals(1, f.hosts.size)
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
    }

    @Test fun permissionResultAfterBackCannotReopenTheCanceledAttempt() {
        val f = start()
        f.deferPermissionResult = true
        phoneRoute()
        val oldGeneration = checkNotNull(f.pendingPermissionGeneration)
        key(KeyEvent.KEYCODE_BACK)
        rule.runOnIdle { f.model.phonePermissionResult(oldGeneration, true) }
        f.workerAction {}
        assertTrue(f.hosts.isEmpty())
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        oneFocus()
    }

    @Test fun claimHidesQrAndKeepsCancelFocusedUntilExplicitBack() {
        val f = start()
        phoneRoute(); qrVisible()
        f.workerAction { f.claim(); f.pulse() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("phone-sign-in-qr").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
        oneFocus()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        rule.waitUntil(5_000) { f.hosts.last().closed }
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("ready-sign-in").assertIsFocused()
    }

    @Test fun retryReplacesConsumedQrAndRestoresCancelFocus() {
        val f = start()
        phoneRoute(); qrVisible()
        val old = f.hosts.last()
        val oldToken = old.link.token
        key(KeyEvent.KEYCODE_DPAD_CENTER) // Cancel is the current target.
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        qrVisible()
        rule.waitUntil(5_000) { f.hosts.size == 2 }
        assertNotEquals(oldToken, f.hosts.last().link.token)
        assertTrue(old.closed)
        rule.onNodeWithTag("phone-sign-in-cancel").assertIsFocused()
        oneFocus()
    }

    @Test fun planRequiredTerminalRetainsExplicitConsentRouteAndUsableFocus() {
        val f = start()
        f.exchangeScopes = setOf("openid", "profile", "email")
        phoneRoute(); qrVisible()
        f.workerAction { f.callback(f.claim()) }
        // Completion is another queued task; a later pulse observes its terminal result.
        f.workerAction { f.pulse() }
        waitTag("phone-sign-in-enable-plan")
        rule.onNodeWithTag("phone-sign-in-enable-plan").assertIsFocused()
        oneFocus()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        qrVisible()
        assertTrue(f.requestConsent)
    }

    @Test fun completedAccountHasNoRedundantContinueActionOrInlineModelWall() {
        start(TvFlowFixture.connected())
        rule.onNodeWithTag("ask").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("configuration").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithText("Continue with ChatGPT").assertDoesNotExist()
        rule.onNodeWithTag("change-model").assertIsFocused()
        rule.onNodeWithTag("model-option-synthetic-model").assertDoesNotExist()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitTag("model-option-synthetic-model")
        rule.onNodeWithTag("model-option-synthetic-model").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("change-model").assertIsFocused()
        oneFocus()
    }

    @Test fun connectedAccountCanReconnectByDpadAfterAuthorizationFailureWithoutSignOut() {
        val f=start(TvFlowFixture.connected())
        f.modelFailure=ApiFailure(401)
        key(KeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithTag("configuration").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitUntil(5_000) { f.model.ui.busy==null && f.model.ui.message.contains("Reconnect account") }
        rule.onNodeWithTag("change-model").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_DOWN) // Read answers aloud.
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("reconnect").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        assertTrue(f.model.ui.signedIn)
        assertTrue(f.model.ui.planAllowed)
        assertFalse(f.model.ui.requestConsent)
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("reconnect").assertIsFocused()
        oneFocus()
    }

    @Test fun successfulAccountUiKeepsPhoneReceiptUntilTheSurfaceLeaves() {
        val f = start()
        phoneRoute(); qrVisible()
        lateinit var claim: com.amine.asktv.phoneauth.RelayClaim
        f.workerAction { claim = f.claim(); f.callback(claim) }
        f.workerAction { f.pulse() }
        waitTag("phone-sign-in-done")
        rule.onNodeWithTag("phone-sign-in-done").assertIsFocused()
        assertFalse(f.hosts.last().closed)
        assertEquals(com.amine.asktv.phoneauth.RelayState.CONNECTED, f.hosts.last().relay.status(claim.relayToken))
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("ask").assertIsFocused()
        rule.waitUntil(5_000) { f.hosts.last().closed }
        rule.onNodeWithTag("ready-sign-in").assertDoesNotExist()
        oneFocus()
    }

    @Test fun acceptedCommitBeforeCancelKeepsConnectedTerminalPresentation() {
        val f = start()
        phoneRoute(); qrVisible()
        rule.runOnIdle {
            // Keep main delivery queued while the real shared grant path commits on the worker.
            f.workerAction { f.callback(f.claim()) }
            f.workerAction {} // Runs after the grant-completion task, including durable installation.
            assertEquals("waiting", f.model.ui.signInState)
            f.model.cancelSignIn()
        }
        waitTag("phone-sign-in-done")
        assertTrue(f.model.ui.signedIn)
        assertTrue(f.model.ui.planAllowed)
        rule.onNodeWithText("Sign-in canceled").assertDoesNotExist()
        rule.onNodeWithTag("phone-sign-in-done").assertIsFocused()
        oneFocus()
    }

    @Test fun queuedCommitCannotReplaceANewRouteChooser() {
        val f = start()
        phoneRoute(); qrVisible()
        rule.runOnIdle {
            f.workerAction { f.callback(f.claim()) }
            f.workerAction {}
            f.model.chooseSignInRoute()
        }
        rule.waitForIdle()
        assertTrue(f.model.ui.signedIn)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        rule.onNodeWithTag("phone-sign-in-done").assertDoesNotExist()
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        oneFocus()
    }

    @Test fun macChoiceHasDedicatedWaitingAndRestoresRouteOnBack() {
        start()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("sign-in-phone-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.onNodeWithTag("sign-in-mac-route").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        waitTag("mac-sign-in-cancel")
        rule.onNodeWithTag("mac-sign-in-cancel").assertIsFocused()
        rule.onNodeWithTag("mac-sign-in-instructions").assertIsDisplayed()
        rule.onNodeWithText("Continue with ChatGPT").assertDoesNotExist()
        key(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("sign-in-retry").assertIsFocused()
        oneFocus()
    }

    @Test fun stoppingAndReturningCannotLeaveAStaleQrOrFocusOnRemovedControl() {
        val f = start()
        phoneRoute(); qrVisible()
        // This covers the ViewModel callback contract, not the actual Activity lifecycle binding.
        rule.runOnIdle { f.model.stopped() }
        rule.waitUntil(5_000) { f.hosts.last().closed }
        rule.onNodeWithTag("phone-sign-in-qr").assertDoesNotExist()
        rule.onNodeWithTag("phone-sign-in-retry").assertIsFocused()
        oneFocus()
    }
}
