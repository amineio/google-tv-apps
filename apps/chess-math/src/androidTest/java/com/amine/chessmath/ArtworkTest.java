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

    @Test public void legalMarkersUseTealOnBothSquareColorsAndSelectionUsesSageGold() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ChessBoardView board = new ChessBoardView(context);
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(810,
                    android.view.View.MeasureSpec.EXACTLY);
            board.measure(spec, spec);
            board.layout(0, 0, 810, 810);
            board.bind(new ChessGame(), 52, 52, "");
            Bitmap rendered = Bitmap.createBitmap(810, 810, Bitmap.Config.ARGB_8888);
            board.draw(new Canvas(rendered));
            assertEquals("e4 marker uses muted teal on the ivory square", 0xff95c4b5,
                    rendered.getPixel(455, 455));
            assertEquals("e3 marker uses muted teal on the green square", 0xff95c4b5,
                    rendered.getPixel(455, 555));
            assertEquals("Legal disc keeps a forest outline against ivory", TvStyle.FOREST,
                    rendered.getPixel(467, 455));
            assertEquals("Legal disc keeps a forest outline against green", TvStyle.FOREST,
                    rendered.getPixel(467, 555));
            assertEquals("Selected e2 keeps a continuous sage-gold border", 0xffc9c58a,
                    rendered.getPixel(455, 619));
            assertTrue(board.getContentDescription().toString().contains("e2"));
            rendered.recycle();
        });
    }

    @Test public void honeyFocusAndSageSelectionRemainDistinctOnBothSquareColors() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int source : new int[]{52, 51}) {
                ChessBoardView board = new ChessBoardView(context);
                int spec = android.view.View.MeasureSpec.makeMeasureSpec(810,
                        android.view.View.MeasureSpec.EXACTLY);
                board.measure(spec, spec);
                board.layout(0, 0, 810, 810);
                assertTrue(board.requestFocus());
                assertTrue(board.hasFocus());
                board.bind(new ChessGame(), source, source, "");
                Bitmap rendered = Bitmap.createBitmap(810, 810, Bitmap.Config.ARGB_8888);
                board.draw(new Canvas(rendered));
                int x = 5 + source % 8 * 100, y = 5 + source / 8 * 100;
                assertEquals("Focus has a continuous honey outline", 0xffffcf70,
                        rendered.getPixel(x + 50, y + 5));
                assertEquals("Selected piece has a separate inner sage-gold border", 0xffc9c58a,
                        rendered.getPixel(x + 50, y + 14));
                assertEquals("Forest understroke separates focus from either square", TvStyle.FOREST,
                        rendered.getPixel(x + 50, y + 8));
                rendered.recycle();
            }
        });
    }

    @Test public void buttonFocusUsesTheSameHoneyAsTheBoard() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            android.graphics.drawable.Drawable background = TvStyle.button(context, "OK").getBackground();
            float density = context.getResources().getDisplayMetrics().density;
            int width = Math.round(200 * density), height = Math.round(60 * density);
            background.setBounds(0, 0, width, height);
            background.setState(new int[]{android.R.attr.state_focused});
            Bitmap rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            background.draw(new Canvas(rendered));
            assertEquals("Button focus shares the honey board outline", 0xffffcf70,
                    rendered.getPixel(width / 2, Math.round(3 * density)));
            rendered.recycle();
        });
    }

    @Test public void legalCaptureUsesAContinuousTealRing() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ChessBoardView board = new ChessBoardView(context);
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(810,
                    android.view.View.MeasureSpec.EXACTLY);
            board.measure(spec, spec);
            board.layout(0, 0, 810, 810);
            for (boolean light : new boolean[]{true, false}) {
                ChessGame game = new ChessGame();
                game.play(light ? "e2e4" : "d2d4");
                String reply = light ? "d7d5" : "e7e5";
                game.play(reply);
                int source = light ? 36 : 35;
                int centerX = light ? 355 : 455;
                board.bind(game, source, source, reply);
                Bitmap rendered = Bitmap.createBitmap(810, 810, Bitmap.Config.ARGB_8888);
                board.draw(new Canvas(rendered));
                assertEquals("Capture ring crosses its top midpoint on either square color", 0xff95c4b5,
                        rendered.getPixel(centerX, 313));
                assertEquals("Capture ring crosses its left midpoint on either square color", 0xff95c4b5,
                        rendered.getPixel(centerX - 42, 355));
                rendered.recycle();
            }
        });
    }
}
