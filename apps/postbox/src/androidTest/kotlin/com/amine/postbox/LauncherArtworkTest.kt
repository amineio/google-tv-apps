package com.amine.postbox

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherArtworkTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun launcherBackgroundIsAnAppOwnedDecodableResource() {
        val app =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
        val resource = app.metaData?.getInt("com.amine.familyhome.HERO_IMAGE", 0) ?: 0
        assertTrue(
            "Postbox must declare its launcher background as a drawable resource",
            resource != 0,
        )
        val bitmap =
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.resources, resource))
        assertEquals(1920, bitmap.width)
        assertEquals(1080, bitmap.height)
        assertTrue(bitmap.width > bitmap.height)
    }

    @Test
    fun launcherBannerFillsAllRequestedLandscapeBounds() {
        for ((width, height) in listOf(320 to 180, 384 to 216, 640 to 360)) {
            val banner =
                requireNotNull(context.packageManager.getApplicationBanner(context.packageName))
            assertEquals(
                "Banner must retain 16:9 proportions",
                16f / 9f,
                banner.intrinsicWidth.toFloat() / banner.intrinsicHeight,
                .001f,
            )
            assertOpaqueEdges(banner, width, height)
        }
    }

    @Test
    fun launcherIconKeepsSquareOpaqueBoundsAtSmallSizes() {
        for (side in listOf(64, 128, 256)) {
            val icon = context.packageManager.getApplicationIcon(context.packageName)
            assertEquals(icon.intrinsicWidth, icon.intrinsicHeight)
            assertOpaqueEdges(icon, side, side)
        }
    }

    private fun assertOpaqueEdges(drawable: Drawable, width: Int, height: Int) {
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(image))
        for (x in listOf(0, width / 2, width - 1)) for (y in listOf(0, height / 2, height - 1)) {
            assertEquals(
                "Artwork must fill ($x, $y) at $width x $height",
                255,
                image.getPixel(x, y) ushr 24,
            )
        }
    }
}
