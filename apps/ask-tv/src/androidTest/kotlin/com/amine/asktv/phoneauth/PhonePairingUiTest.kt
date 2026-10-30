package com.amine.asktv.phoneauth

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import android.app.Activity
import android.app.Instrumentation
import android.provider.MediaStore
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhonePairingUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val link = PhonePairingLink("192.168.1.2", 8443, "ab".repeat(32), "a".repeat(43))
    private fun photo(content: String): File {
        val file = File(context.cacheDir, "phone_pairing").apply { mkdirs() }.resolve("fixture-${System.nanoTime()}.png")
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 900, 900)
        val bitmap = Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888)
        try {
            for (y in 0 until 900) for (x in 0 until 900) bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        return file
    }
    @Test fun genuineQrPhotoDecodesAndPrivatePhotoIsDeleted() {
        val file = photo(link.encode())
        assertEquals(link, PhonePairingPhotoReader.read(file))
        assertFalse(file.exists())
    }
    @Test fun malformedQrAndInvalidImageAreDeletedAfterFailure() {
        for (file in listOf(photo("aichat://pair?bad=1"), File(context.cacheDir, "invalid-${System.nanoTime()}.png").apply { writeText("invalid") })) {
            assertThrows(Exception::class.java) { PhonePairingPhotoReader.read(file) }
            assertFalse(file.exists())
        }
    }
    @Test fun validDeepLinkIsClearedAndRequiresVisibleContinueBeforeNetwork() {
        ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java).apply { action = Intent.ACTION_VIEW; data = Uri.parse(link.encode()) }).use { scenario ->
            compose.onNodeWithText("Continue with ChatGPT").performScrollTo().assertIsDisplayed()
            scenario.onActivity { assertNull(it.intent.data) }
            compose.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
            // No claim occurred: the unreachable fixture leaves the visible confirmation screen usable.
            compose.onNodeWithText("TV code ready").performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun malformedDeepLinkCannotContinue() {
        ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java).apply { action = Intent.ACTION_VIEW; data = Uri.parse("aichat://pair?token=bad") }).use {
            compose.onNodeWithText("Continue with ChatGPT").assertIsNotEnabled()
            compose.onNodeWithText("Photograph TV code").assertIsDisplayed()
        }
    }
    @Test fun delegatedCameraCancellationDeletesThePrivatePhotograph() {
        Intents.init()
        try {
            Intents.intending(IntentMatchers.hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java)).use {
                compose.onNodeWithText("Photograph TV code").performScrollTo().performClick()
                compose.waitUntil(5_000) { Intents.getIntents().any { it.action == MediaStore.ACTION_IMAGE_CAPTURE } }
                compose.waitUntil(5_000) { File(context.cacheDir, "phone_pairing").listFiles().orEmpty().isEmpty() }
                compose.onNodeWithText("Continue with ChatGPT").assertIsNotEnabled()
            }
        } finally { Intents.release() }
    }
    @Test fun helperExplainsPurposeAndUsesReadablePhoneContrast() {
        ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java)).use {
            compose.onNodeWithText("Sign in on your TV").assertIsDisplayed()
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            var dark = 0; var light = 0
            for (y in 0 until pixels.height step 3) for (x in 0 until pixels.width step 3) {
                val p = pixels[x, y]
                val brightness = (p.red + p.green + p.blue) / 3
                if (brightness < 0.15f) dark++
                if (brightness > 0.85f) light++
            }
            assertTrue("Phone screen must contain a dark surface", dark > 1000)
            assertTrue("Phone screen must contain bright readable text", light > 100)
            val text = compose.onNodeWithText("Your account stays on your TV. This helper does not save your sign-in details.")
                .performScrollTo().captureToImage().toPixelMap()
            fun channel(value: Float): Double = if (value <= 0.04045f) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
            var darkest = 1.0; var brightest = 0.0
            for (y in 0 until text.height) for (x in 0 until text.width) {
                val pixel = text[x, y]
                val luminance = 0.2126 * channel(pixel.red) + 0.7152 * channel(pixel.green) + 0.0722 * channel(pixel.blue)
                darkest = minOf(darkest, luminance); brightest = maxOf(brightest, luminance)
            }
            assertTrue("Rendered secondary text must reach a 4.5:1 contrast ratio", (brightest + 0.05) / (darkest + 0.05) >= 4.5)
        }
    }
}
