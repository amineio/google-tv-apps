package com.amine.tvclock;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ArtworkDeviceTest {
    @Test public void installedLauncherArtworkSupportsLauncherDecoder() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                context.getPackageName(), PackageManager.GET_META_DATA);
        Resources resources = context.getPackageManager().getResourcesForApplication(info);
        assertNotNull("Launcher background metadata is required", info.metaData);
        int backgroundId = info.metaData.getInt("com.amine.familyhome.HERO_IMAGE", 0);
        assertTrue("Launcher background resource is required", backgroundId != 0);
        assertEquals("drawable", resources.getResourceTypeName(backgroundId));
        assertTrue("Icon and banner must be distinct", info.icon != info.banner);
        assertTrue("Background and icon must be distinct", backgroundId != info.icon);
        assertTrue("Background and banner must be distinct", backgroundId != info.banner);

        Bitmap background = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, backgroundId));
        assertFalse(background.isRecycled());
        assertEquals(1920, background.getWidth());
        assertEquals(1080, background.getHeight());
        assertEquals(16.0 / 9.0, (double) background.getWidth() / background.getHeight(), 0.001);
        save(context, background, "clock-art-background.png");
        render(context, resources.getDrawable(info.icon, context.getTheme()), 512, 512, "clock-art-icon.png");
        render(context, resources.getDrawable(info.banner, context.getTheme()), 640, 360, "clock-art-banner.png");
    }

    private static void render(Context context, Drawable drawable, int width, int height, String name)
            throws Exception {
        assertNotNull(drawable);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));
        save(context, bitmap, name);
    }

    private static void save(Context context, Bitmap bitmap, String name) throws Exception {
        File directory = context.getExternalFilesDir(null);
        assertNotNull("Artwork capture directory is required", directory);
        try (FileOutputStream stream = new FileOutputStream(new File(directory, name), false)) {
            assertTrue("Artwork PNG must be encoded", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
    }
}
