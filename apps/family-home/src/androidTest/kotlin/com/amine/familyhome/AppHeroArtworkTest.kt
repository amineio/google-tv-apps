package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppHeroArtworkTest {
    @get:Rule val screen = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val fixture get() = instrumentation.context
    private val manager get() = fixture.packageManager
    private fun reference(name: String) = AppHeroArtworkReference(fixture.packageName,
        fixture.resources.getIdentifier(name, "drawable", fixture.packageName).also { assertTrue(it != 0) })
    private fun app(pkg: String, hero: AppHeroArtworkReference? = null) = LauncherApp(pkg, "Test app",
        ComponentName(pkg, "$pkg.Main"), Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(70, 45, 90))
        }, heroArtwork = hero)

    @Test fun applicationMetadataLoadsAReferenceWithoutRemovingTheTile() {
        val expected = reference("hero_wide")
        assertEquals(expected, readAppHeroArtwork(manager, fixture.packageName))
        val tile = loadLauncherCatalog(manager, instrumentation.targetContext.packageName)
            .single { it.pkg == fixture.packageName }
        assertEquals(expected, tile.heroArtwork)
        assertEquals(fixture.packageName, tile.component.packageName)
        assertNull(readAppHeroArtwork(manager, instrumentation.targetContext.packageName))
        assertNull(readAppHeroArtwork(manager, "example.uninstalled.hero"))
    }

    @Test fun decodeBoundsWideAndTallImagesWithoutUpscalingSmallImages() = runBlocking {
        val wide = requireNotNull(decodeAppHeroArtwork(manager, reference("hero_wide")))
        assertEquals(1920, wide.width)
        assertEquals(1080, wide.height)
        assertEquals(Bitmap.Config.ARGB_8888, wide.config)
        val tall = requireNotNull(decodeAppHeroArtwork(manager, reference("hero_tall")))
        assertEquals(540, tall.width)
        assertEquals(1080, tall.height)
        val small = requireNotNull(decodeAppHeroArtwork(manager, reference("hero_small")))
        assertEquals(128, small.width)
        assertEquals(64, small.height)
        assertEquals(android.graphics.Color.rgb(0, 216, 232), small.getPixel(64, 32))
    }

    @Test fun missingPackagesBadResourcesAndUnsupportedDrawablesFallBack() = runBlocking {
        assertNull(decodeAppHeroArtwork(manager, AppHeroArtworkReference("example.uninstalled.hero", 1)))
        assertNull(decodeAppHeroArtwork(manager, AppHeroArtworkReference(fixture.packageName, 0)))
        assertNull(decodeAppHeroArtwork(manager, AppHeroArtworkReference(fixture.packageName, 0x7fffffff)))
        assertNull(decodeAppHeroArtwork(manager, reference("hero_unsupported")))
    }

    @Test fun directionalSelectionChangesHeroAndFallbackWhileKeepingFocus() {
        var resume by mutableIntStateOf(0)
        val first = app("example.artwork", reference("hero_small"))
        val fallback = app("example.fallback")
        val invalid = app("example.invalid", reference("hero_unsupported"))
        screen.setContent {
            FamilyTheme {
                var selected by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.AMINE, null, listOf(first, fallback, invalid), selected, resume,
                    { selected = it }, {}, {}, {}, displayName = "Parent", portrait = Portrait.CHILD)
            }
        }
        screen.waitUntil(10_000) { heroIsCyan() }
        screen.onNodeWithTag("app:example.artwork").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithTag("app:example.fallback").assertIsFocused()
        assertFalse("The previous app artwork remains behind the fallback", heroIsCyan())
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithTag("app:example.invalid").assertIsFocused()
        assertFalse("Unsupported artwork must use the gradient", heroIsCyan())
        screen.onRoot().performKeyInput { pressKey(Key.DirectionLeft); pressKey(Key.DirectionLeft) }
        screen.waitUntil(10_000) { heroIsCyan() }
        screen.runOnIdle { resume++ }
        screen.waitUntil(10_000) { heroIsCyan() }
        screen.onNodeWithTag("app:example.artwork").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithTag("home-profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithTag("app:example.artwork").assertIsFocused()
        if (InstrumentationRegistry.getArguments().getString("captureHero") == "true") {
            val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
            directory.resolve("home-selected-hero.png").outputStream().use {
                screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun changedReferenceClearsOldArtAndChessKeepsItsBundledHero() {
        var catalog by mutableStateOf(listOf(app("example.artwork", reference("hero_small"))))
        screen.setContent {
            FamilyTheme {
                var selected by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.AMINE, null, catalog, selected, 0, { selected = it }, {}, {}, {},
                    displayName = "Parent", portrait = Portrait.CHILD)
            }
        }
        screen.waitUntil(10_000) { heroIsCyan() }
        screen.runOnIdle { catalog = listOf(app("example.artwork", reference("hero_unsupported"))) }
        screen.waitForIdle()
        assertFalse("A changed invalid reference must clear the old bitmap", heroIsCyan())
        screen.onNodeWithTag("app:example.artwork").assertIsFocused()
        screen.runOnIdle { catalog = listOf(app("com.amine.chessmath")) }
        val chess = sampleHeroPixel()
        screen.runOnIdle { catalog = listOf(app("com.amine.chessmath", reference("hero_small"))) }
        screen.waitForIdle()
        assertEquals("Bundled Chess artwork has precedence", chess, sampleHeroPixel())
        screen.onNodeWithTag("app:com.amine.chessmath").assertIsFocused()
    }

    private fun sampleHeroPixel(): Int {
        val image = screen.onRoot().captureToImage().asAndroidBitmap()
        return image.getPixel((image.width * .94f).toInt(), (image.height * .4f).toInt())
    }
    private fun heroIsCyan(): Boolean {
        val pixel = sampleHeroPixel()
        return android.graphics.Color.green(pixel) > android.graphics.Color.red(pixel) + 40 &&
            android.graphics.Color.blue(pixel) > android.graphics.Color.red(pixel) + 40
    }
}
