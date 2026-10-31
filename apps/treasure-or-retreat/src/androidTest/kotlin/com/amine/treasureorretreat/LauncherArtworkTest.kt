package com.amine.treasureorretreat

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class LauncherArtworkTest {
    @Test fun artworkHasCorrectDimensionsAndManifestReferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
        assertEquals(R.drawable.app_icon, info.icon)
        assertEquals(R.drawable.app_banner, info.banner)
        assertEquals(R.drawable.app_hero, info.metaData.getInt("com.amine.familyhome.HERO_IMAGE"))
        for ((id, expected) in listOf(R.drawable.app_icon to (512 to 512), R.drawable.app_banner to (640 to 360), R.drawable.app_hero to (1920 to 1080))) {
            val drawable = context.getDrawable(id) as BitmapDrawable
            assertEquals(expected.first, drawable.bitmap.width)
            assertEquals(expected.second, drawable.bitmap.height)
        }
    }
    @Test fun artworkRendersAtLauncherSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for ((id, sizes) in listOf(R.drawable.app_icon to listOf(64 to 64, 128 to 128), R.drawable.app_banner to listOf(320 to 180, 384 to 216))) {
            for ((width, height) in sizes) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val drawable = context.getDrawable(id)!!
                drawable.setBounds(0, 0, width, height)
                drawable.draw(Canvas(bitmap))
                assertTrue(bitmap.getPixel(width/2, height/2) ushr 24 != 0)
                assertTrue(bitmap.getPixel(width/4, height/4) ushr 24 != 0)
                bitmap.recycle()
            }
        }
    }
}
