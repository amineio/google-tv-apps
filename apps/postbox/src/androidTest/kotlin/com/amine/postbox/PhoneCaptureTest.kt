package com.amine.postbox

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.postbox.core.PostboxStore
import java.io.File
import java.util.Base64
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneCaptureTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<PhoneActivity>? = null
    private val store
        get() = PostboxStore(File(context.filesDir, "phone-captures"))

    @Before
    fun clean() {
        File(context.filesDir, "phone-captures").deleteRecursively()
        File(context.filesDir, "paired-tv.json").delete()
        File(context.cacheDir, "pairing-photos").deleteRecursively()
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun open(intent: Intent = Intent()) {
        scenario = ActivityScenario.launch(intent.setClass(context, PhoneActivity::class.java))
        compose.waitForIdle()
    }

    private fun waitForSaved(count: Int = 1) {
        compose.waitUntil(10_000) { store.list().size == count }
    }

    private fun save() {
        compose.onNodeWithTag("save-phone").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Saved on phone").fetchSemanticsNodes().isNotEmpty()
        }
        waitForSaved()
    }

    @Test
    fun sharedTextPreservesParagraphsAndDoesNotSaveOrSendOnOpen() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "First paragraph.\n\nSecond paragraph.")
        )
        compose
            .onNodeWithTag("capture-text")
            .assertTextContains("First paragraph.\n\nSecond paragraph.")
        assertTrue(store.list().isEmpty())
        save()
        assertEquals("First paragraph.\n\nSecond paragraph.", store.list().single().capture.text)
        assertFalse(store.list().single().sent)
    }

    @Test
    fun linkOnlyIsHonestAndDurableAfterRecreation() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://x.com/sample/status/123?tracking=1")
        )
        compose.onNodeWithText("Link only", substring = true).assertExists()
        compose.onNodeWithText("Add text or image", substring = true).assertExists()
        save()
        scenario!!.recreate()
        compose.onNodeWithText("Saved on phone").assertExists()
        assertEquals("", store.list().single().capture.text)
        assertEquals("https://x.com/sample/status/123", store.list().single().capture.sourceUrl)
        assertFalse(store.list().single().sent)
    }

    @Test
    fun editedSavedDraftGetsNewIdentityThroughSaveAction() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Original body")
        )
        save()
        val old = store.list().single().capture.id
        compose.onNodeWithText("Edit capture").performScrollTo().performClick()
        compose.onNodeWithTag("capture-text").performTextReplacement("Edited body")
        save()
        val edited = store.list().single()
        assertNotEquals(old, edited.capture.id)
        assertEquals("Edited body", edited.capture.text)
        assertFalse(edited.sent)
    }

    @Test
    fun manualDraftRequiresExplicitSave() {
        open()
        compose.onNodeWithText("New capture").performClick()
        compose.onNodeWithTag("capture-title").performTextInput("Local note")
        compose.onNodeWithTag("capture-text").performTextInput("Keep this note.")
        assertTrue(store.list().isEmpty())
        save()
        assertEquals("Local note", store.list().single().capture.title)
    }

    @Test
    fun sharedTextAndImageAreBothSavedAndImageIsBounded() {
        val file =
            File(context.cacheDir, "pairing-photos/shared-test.png").also {
                it.parentFile!!.mkdirs()
            }
        Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.GREEN)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        open(
            Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_TEXT, "Image caption")
                .putExtra(
                    Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.pairing-files",
                        file,
                    ),
                )
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("1 image").fetchSemanticsNodes().isNotEmpty()
        }
        save()
        val capture = store.list().single().capture
        assertEquals("Image caption", capture.text)
        val bytes = Base64.getDecoder().decode(capture.imagesJpegBase64.single())
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        assertEquals(2048, maxOf(options.outWidth, options.outHeight))
    }

    @Test
    fun fifthSharedImageShowsLimitBeforeSave() {
        val files =
            (1..5).map { index ->
                File(context.cacheDir, "pairing-photos/multi-$index.png").also { file ->
                    file.parentFile!!.mkdirs()
                    Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(android.graphics.Color.BLUE)
                        file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
                        recycle()
                    }
                }
            }
        open(
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putExtra(Intent.EXTRA_TEXT, "Multiple pictures")
                .putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    ArrayList(
                        files.map {
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.pairing-files",
                                it,
                            )
                        }
                    ),
                )
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        compose.onNodeWithText("Use up to four images.").assertExists()
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun connectionRecreationKeepsSavedDraft() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Keep the saved capture")
        )
        save()
        compose.onNodeWithText("Connect TV").performScrollTo().performClick()
        scenario!!.recreate()
        compose.onNodeWithText("Photograph TV code").assertExists()
        assertEquals("Keep the saved capture", store.list().single().capture.text)
        assertFalse(store.list().single().sent)
    }

    @Test
    fun unavailableTvKeepsSavedUnsentCaptureForRetryAfterRecreation() {
        val paired =
            com.amine.postbox.core.PairedTv(
                "10.0.2.2",
                1,
                "a".repeat(64),
                "a".repeat(43),
                "Postbox TV",
            )
        File(context.filesDir, "paired-tv.json")
            .writeText(
                kotlinx.serialization.json.Json.encodeToString(
                    com.amine.postbox.core.PairedTv.serializer(),
                    paired,
                )
            )
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Retry this capture")
        )
        save()
        compose.onNodeWithText("Send to TV").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("TV unavailable.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertFalse(store.list().single().sent)
        scenario!!.recreate()
        compose.onNodeWithText("Send to TV").assertExists()
        assertEquals("Retry this capture", store.list().single().capture.text)
        assertFalse(store.list().single().sent)
    }

    @Test
    fun failedEditedSavePreservesOriginalDraftAtCapacity() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Original at capacity")
        )
        save()
        val original = store.list().single().capture
        repeat(49) { index ->
            store.save(
                com.amine.postbox.core.PostCapture(
                    java.util.UUID.randomUUID().toString(),
                    "Other $index",
                    "Other saved capture",
                    "",
                    emptyList(),
                    index.toLong(),
                )
            )
        }
        compose.onNodeWithText("Edit capture").performScrollTo().performClick()
        compose.onNodeWithTag("capture-text").performTextReplacement("Changed at capacity")
        compose.onNodeWithTag("save-phone").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose
                .onAllNodesWithText("Library full.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(50, store.list().size)
        assertEquals(original, store.list().first { it.capture.id == original.id }.capture)
        compose.onNodeWithTag("capture-text").assertTextContains("Changed at capacity")
    }

    @Test
    fun delegatedCameraCancellationDeletesTemporaryPhotoAndKeepsDraft() {
        androidx.test.espresso.intent.Intents.init()
        try {
            androidx.test.espresso.intent.Intents.intending(
                    androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    )
                )
                .respondWith(
                    android.app.Instrumentation.ActivityResult(
                        android.app.Activity.RESULT_CANCELED,
                        null,
                    )
                )
            open(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Camera cancellation keeps this")
            )
            save()
            compose.onNodeWithText("Connect TV").performScrollTo().performClick()
            compose.onNodeWithText("Photograph TV code").performClick()
            compose.waitForIdle()
            compose.waitUntil(10_000) {
                androidx.test.espresso.intent.Intents.getIntents().any {
                    it.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                }
            }
            compose.waitUntil(10_000) {
                File(context.cacheDir, "pairing-photos").listFiles().orEmpty().isEmpty()
            }
            compose.onNodeWithText("Camera unavailable.", substring = true).assertDoesNotExist()
            compose.onNodeWithText("Photograph TV code").assertExists()
            assertEquals("Camera cancellation keeps this", store.list().single().capture.text)
            assertFalse(store.list().single().sent)
        } finally {
            androidx.test.espresso.intent.Intents.release()
        }
    }

    @Test
    fun fullResolutionPairingPhotoDecodesAndMalformedPhotoIsRejected() {
        val link =
            "postbox://pair?host=10.0.2.2&port=18765&pin=${"a".repeat(64)}&token=${"a".repeat(43)}"
        val matrix =
            com.google.zxing
                .MultiFormatWriter()
                .encode(link, com.google.zxing.BarcodeFormat.QR_CODE, 768, 768)
        val pixels =
            IntArray(768 * 768) { index ->
                if (matrix[index % 768, index / 768]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            }
        val file = File(context.cacheDir, "qr-test.jpg")
        val bitmap = Bitmap.createBitmap(pixels, 768, 768, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            assertEquals(link, PairingPhotoReader.read(file))
            file.writeText("damaged image")
            assertTrue(runCatching { PairingPhotoReader.read(file) }.isFailure)
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    @Test
    fun shareArrivingDuringSaveKeepsNewContentInEditor() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "First saved capture")
        )
        lateinit var model: PostboxViewModel
        scenario!!.onActivity { activity ->
            model = androidx.lifecycle.ViewModelProvider(activity)[PostboxViewModel::class.java]
            model.savePhone()
            model.receiveIntent(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "New incoming share")
            )
        }
        waitForSaved()
        compose.waitUntil(10_000) { !model.busy }
        compose.waitUntil(10_000) {
            compose
                .onAllNodes(hasTestTag("capture-text") and hasText("New incoming share"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("capture-text").assertTextContains("New incoming share")
        compose.onNodeWithText("Save a post").assertExists()
        assertEquals("First saved capture", store.list().single().capture.text)
    }

    @Test
    fun multipleSharedImagesAndCaptionAreSavedTogether() {
        val uris =
            (1..2).map { index ->
                val file =
                    File(context.cacheDir, "pairing-photos/accepted-$index.png").also {
                        it.parentFile!!.mkdirs()
                    }
                Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(android.graphics.Color.BLUE)
                    file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
                    recycle()
                }
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.pairing-files",
                    file,
                )
            }
        open(
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putExtra(Intent.EXTRA_TEXT, "Two captured images")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("2 images").fetchSemanticsNodes().isNotEmpty()
        }
        save()
        assertEquals("Two captured images", store.list().single().capture.text)
        assertEquals(2, store.list().single().capture.imagesJpegBase64.size)
    }

    @Test
    fun pairingAndExplicitSendShowSuccessOnlyAfterTvStorage() {
        val receiverDirectory =
            File(context.cacheDir, "phone-flow-tv").also { it.deleteRecursively() }
        val tvStore = PostboxStore(receiverDirectory)
        val receiver = com.amine.postbox.core.CaptureReceiver(context, tvStore) {}
        try {
            val session = receiver.start()
            open(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Actual phone transfer")
            )
            save()
            compose.onNodeWithText("Connect TV").performScrollTo().performClick()
            compose.onNodeWithText("Paste pairing link").performClick()
            compose.onNodeWithTag("pairing-input").performTextInput(session.pairingLink)
            compose.onNodeWithText("Pair TV").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Send to TV").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(tvStore.list().isEmpty())
            assertFalse(store.list().single().sent)
            compose.onNodeWithText("Send to TV").performScrollTo().performClick()
            compose.waitUntil(15_000) { store.list().single().sent }
            compose.onNodeWithText("Received on TV").assertExists()
            assertEquals(store.list().single().capture, tvStore.list().single().capture)
        } finally {
            receiver.close()
            receiverDirectory.deleteRecursively()
        }
    }

    @Test
    fun malformedPairLinkDoesNotRemoveSavedDraft() {
        open(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Keep my draft")
        )
        save()
        compose.onNodeWithText("Connect TV").performScrollTo().performClick()
        compose.onNodeWithText("Paste pairing link").performClick()
        compose.onNodeWithTag("pairing-input").performTextInput("postbox://pair?host=example.com")
        compose.onNodeWithText("Pair TV", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Invalid pairing link.").assertExists()
        assertEquals("Keep my draft", store.list().single().capture.text)
        assertFalse(store.list().single().sent)
    }
}
