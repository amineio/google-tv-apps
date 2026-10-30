package com.amine.asktv.phoneauth

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
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
    /** Cold deep links clear their Intent in onCreate, so matching by Intent is unsuitable here. */
    private fun withColdDeepLink(value: String, block: (PhoneAuthActivity) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val resumed = CountDownLatch(1)
        val current = AtomicReference<PhoneAuthActivity?>()
        val observer = ActivityLifecycleCallback { activity, stage ->
            if (activity is PhoneAuthActivity && stage == Stage.RESUMED) {
                current.set(activity)
                resumed.countDown()
            }
        }
        monitor.addLifecycleCallback(observer)
        try {
            context.startActivity(Intent(context, PhoneAuthActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(value)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            assertTrue("Cold helper reached RESUMED", resumed.await(8, TimeUnit.SECONDS))
            block(requireNotNull(current.get()))
        } finally {
            instrumentation.runOnMainSync { current.get()?.finish() }
            monitor.removeLifecycleCallback(observer)
        }
    }
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
    private fun framedPhoto(width: Int, height: Int, codeSize: Int, jpeg: Boolean): File {
        val file = File(context.cacheDir, "phone_pairing").apply { mkdirs() }
            .resolve("framed-${System.nanoTime()}.${if (jpeg) "jpg" else "png"}")
        val matrix = MultiFormatWriter().encode(link.encode(), BarcodeFormat.QR_CODE, codeSize, codeSize)
        val qr = Bitmap.createBitmap(codeSize, codeSize, Bitmap.Config.ARGB_8888)
        val frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val pixels = IntArray(codeSize * codeSize)
            for (y in 0 until codeSize) for (x in 0 until codeSize) pixels[y * codeSize + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            qr.setPixels(pixels, 0, codeSize, 0, 0, codeSize, codeSize)
            Canvas(frame).apply {
                drawColor(Color.rgb(220, 225, 230))
                drawBitmap(qr, (width - codeSize) / 3f, (height - codeSize) / 3f, null)
            }
            file.outputStream().use { output ->
                assertTrue(frame.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 90, output))
            }
        } finally { qr.recycle(); frame.recycle() }
        return file
    }
    @Test fun rectangularPhotoDecodesWithCorrectQuarterTurnGeometry() {
        val file = framedPhoto(1400, 1100, 900, jpeg = false)
        assertEquals(link, PhonePairingPhotoReader.read(file))
        assertFalse(file.exists())
    }
    @Test fun oversizedRectangularJpegPhotoDecodesWithinTheImageBound() {
        val file = framedPhoto(3000, 2200, 1800, jpeg = true)
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
        withColdDeepLink(link.encode()) { activity ->
            compose.onNodeWithText("Continue with ChatGPT").performScrollTo().assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { assertNull(activity.intent.data) }
            compose.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
            // No claim occurred: the unreachable fixture leaves the visible confirmation screen usable.
            compose.onNodeWithText("TV code ready").performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun malformedDeepLinkCannotContinue() {
        withColdDeepLink("aichat://pair?token=bad") {
            compose.onNodeWithText("Continue with ChatGPT").assertDoesNotExist()
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
                compose.onNodeWithText("Continue with ChatGPT").assertDoesNotExist()
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
