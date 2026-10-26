package com.amine.chessmath;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Production artwork stays padded, proportional, and inside the native board cells. */
@RunWith(AndroidJUnit4.class)
public final class ArtworkTest {
    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test public void allTwelvePiecesHaveValidSourcesAndDestinations() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PiecePainter painter = new PiecePainter(context);
        Object atlas = field(painter, "atlas");
        assertNotNull("The production atlas must load instead of using fallback paths", atlas);
        assertSame(atlas, field(new PiecePainter(context), "atlas"));
        Bitmap bitmap = (Bitmap) field(atlas, "bitmap");
        Rect[] sources = (Rect[]) field(atlas, "bounds");
        assertEquals(12, sources.length);
        int cellWidth = bitmap.getWidth() / 6, cellHeight = bitmap.getHeight() / 2;
        Bitmap rendered = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(rendered);
        String pieces = "PNBRQKpnbrqk";
        for (int i = 0; i < sources.length; i++) {
            Rect source = sources[i];
            int x = i % 6 * cellWidth, y = i / 6 * cellHeight;
            assertTrue(source.width() > 0 && source.height() > 0);
            assertTrue(source.left > x && source.right < x + cellWidth);
            assertTrue(source.top > y && source.bottom < y + cellHeight);
            boolean opaque = false;
            for (int yy = source.top; yy < source.bottom && !opaque; yy++) {
                for (int xx = source.left; xx < source.right; xx++) {
                    if ((bitmap.getPixel(xx, yy) >>> 24) > 8) { opaque = true; break; }
                }
            }
            assertTrue("Each source contains visible artwork", opaque);
            rendered.eraseColor(0);
            painter.draw(canvas, pieces.charAt(i), 10, 10, 100);
            RectF destination = (RectF) field(painter, "destination");
            assertTrue(destination.left >= 10 && destination.right <= 110);
            assertTrue(destination.top >= 10 && destination.bottom <= 110);
            assertTrue(destination.width() <= 76.01f);
            assertEquals(99f, destination.bottom, .01f);
            assertEquals((float) source.width() / source.height(),
                    destination.width() / destination.height(), .001f);
            boolean painted = false;
            for (int yy = 0; yy < 120 && !painted; yy++) for (int xx = 0; xx < 120; xx++) {
                if ((rendered.getPixel(xx, yy) >>> 24) > 8) { painted = true; break; }
            }
            assertTrue("Each piece draws visible pixels", painted);
        }
        rendered.recycle();
    }

    @Test public void boardKeepsAllSixtyFourEqualCells() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ChessBoardView board = new ChessBoardView(context);
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(810,
                    android.view.View.MeasureSpec.EXACTLY);
            board.measure(spec, spec);
            board.layout(0, 0, 810, 810);
            Bitmap rendered = Bitmap.createBitmap(810, 810, Bitmap.Config.ARGB_8888);
            board.draw(new Canvas(rendered));
            for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
                int expected = (row + col) % 2 == 0 ? 0xffeee3c8 : 0xff486a4d;
                assertEquals(expected, rendered.getPixel(55 + col * 100, 55 + row * 100));
            }
            assertEquals(TvStyle.IVORY, rendered.getPixel(2, 405));
            assertEquals(TvStyle.IVORY, rendered.getPixel(807, 405));
            rendered.recycle();
        });
    }
}
