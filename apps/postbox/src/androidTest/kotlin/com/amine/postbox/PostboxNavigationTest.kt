package com.amine.postbox

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.postbox.core.PostCapture
import com.amine.postbox.core.PostboxStore
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostboxNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store
        get() = PostboxStore(File(context.filesDir, "tv-captures"))

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun clean() {
        File(context.filesDir, "tv-captures").deleteRecursively()
    }

    @After
    fun close() {
        scenario?.close()
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun seed(
        text: String = "A useful habit starts small.\n\n".repeat(60),
        source: String = "https://x.com/sample/status/123",
    ) =
        PostCapture(
                UUID.randomUUID().toString(),
                "Small habits",
                text,
                source,
                emptyList(),
                1000,
            )
            .also { store.save(it) }

    @Test
    fun emptyLibraryStartsOnAddAndExampleDoesNotChangeLibrary() {
        launch()
        compose.onNodeWithTag("empty-add").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("example").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("Example", substring = true).assertExists()
        key(KeyEvent.KEYCODE_BACK)
        assertTrue(store.list().isEmpty())
        compose.onNodeWithText("Saved · 0").assertExists()
    }

    @Test
    fun distantCardReturnsFromReaderWithFocusAndScrollRestored() {
        repeat(20) { index ->
            store.save(
                PostCapture(
                    UUID.randomUUID().toString(),
                    "Capture ${index + 1}",
                    "A saved capture for directional navigation.",
                    "",
                    emptyList(),
                    index.toLong(),
                )
            )
        }
        val ordered = store.list()
        launch()
        compose.onNodeWithTag("capture-${ordered.first().capture.id}").assertIsFocused()
        repeat(14) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        val target = ordered[14].capture.id
        compose.onNodeWithTag("capture-$target").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("reader-archive").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("capture-$target").assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("capture-${ordered[15].capture.id}").assertIsFocused()
    }

    @Test
    fun firstCardReaderScrollAndBackRestoreFocus() {
        val capture = seed()
        launch()
        compose.onNodeWithTag("capture-${capture.id}").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("reader-archive").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("reader-body").assertIsFocused()
        repeat(7) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        val before =
            compose
                .onNodeWithTag("reader-body")
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertTrue(before > 0)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("reader-archive").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        val after =
            compose
                .onNodeWithTag("reader-body")
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
                .value()
        assertEquals(before, after, 1f)
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("capture-${capture.id}").assertIsFocused()
    }

    @Test
    fun archiveAndRestoreFollowActualDirectionalControls() {
        val capture = seed()
        launch()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5000) { store.list().single().archived }
        key(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithTag("tab-saved").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("tab-archive").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("capture-${capture.id}").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("reader-archive").assertTextContains("Restore")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(5000) { !store.list().single().archived }
    }

    @Test
    fun deletionStartsOnCancelAndBackKeepsCapture() {
        seed(source = "")
        launch()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("reader-delete").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("delete-cancel").assertIsFocused()
        key(KeyEvent.KEYCODE_BACK)
        assertEquals(1, store.list().size)
        compose.onNodeWithTag("reader-delete").assertIsFocused()
    }

    @Test
    fun tallImageFitsReaderWidthScrollsAndBackRestoresImageAction() {
        val image =
            android.graphics.Bitmap.createBitmap(
                256,
                2048,
                android.graphics.Bitmap.Config.ARGB_8888,
            )
        image.eraseColor(android.graphics.Color.LTGRAY)
        val bytes =
            java.io
                .ByteArrayOutputStream()
                .also { image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }
                .toByteArray()
        image.recycle()
        val capture =
            PostCapture(
                UUID.randomUUID().toString(),
                "Tall screenshot",
                "",
                "",
                listOf(java.util.Base64.getEncoder().encodeToString(bytes)),
                1000,
            )
        store.save(capture)
        launch()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("reader-image").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("next-image").assertDoesNotExist()
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("image-body").assertIsFocused()
        repeat(5) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        val range =
            compose
                .onNodeWithTag("image-body")
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.value() > 0)
        assertTrue(range.maxValue() > 1000)
        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("reader-image").assertIsFocused()
    }

    @Test
    fun libraryControlsStayWithinSixPercentMarginsAndResumeRestoresCard() {
        val capture = seed()
        launch()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val frame = compose.onNodeWithTag("tv-safe-frame").fetchSemanticsNode().boundsInRoot
        assertTrue(frame.left >= root.width * .059f)
        assertTrue(frame.top >= root.height * .059f)
        assertTrue(frame.right <= root.width * .941f)
        assertTrue(frame.bottom <= root.height * .941f)
        scenario!!.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        scenario!!.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithTag("capture-${capture.id}").assertIsFocused()
    }

    @Test
    fun receiverClosesWhenAddPostsActivityStops() {
        launch()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(15_000) {
            compose
                .onAllNodesWithText("Ready to receive", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val host =
            java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
                .flatMap { java.util.Collections.list(it.inetAddresses) }
                .filterIsInstance<java.net.Inet4Address>()
                .first { !it.isLoopbackAddress && it.isSiteLocalAddress }
                .hostAddress!!
        val port = context.getSharedPreferences("postbox-receiver", 0).getInt("port", 18765)
        val accepted = java.net.Socket(host, port).apply { soTimeout = 3000 }
        try {
            scenario!!.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.waitUntil(5000) {
                runCatching {
                        java.net.Socket().use {
                            it.connect(java.net.InetSocketAddress(host, port), 100)
                            false
                        }
                    }
                    .getOrElse { true }
            }
            assertEquals(-1, accepted.getInputStream().read())
        } finally {
            accepted.close()
        }
    }

    @Test
    fun linkOnlyShowsDetailsWithoutInventedBody() {
        seed(text = "")
        launch()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithText("Post details").assertExists()
        compose.onNodeWithText("Only the link was shared.", substring = true).assertExists()
        compose.onNodeWithTag("reader-original").assertExists()
    }
}
