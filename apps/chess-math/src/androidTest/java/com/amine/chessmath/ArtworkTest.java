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
    @Test public void tvBannerFillsRectangularLauncherBoundsAtDifferentSizes() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.graphics.drawable.Drawable banner=context.getPackageManager().getApplicationBanner(context.getPackageName());
        assertNotNull("The launcher must receive the app's rectangular banner",banner);
        assertEquals("Banner intrinsic dimensions must keep the 16:9 ratio",banner.getIntrinsicWidth()*9,banner.getIntrinsicHeight()*16);
        for(int width:new int[]{320,384,640}){
            int height=width*9/16;
            Bitmap rendered=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            banner.setBounds(0,0,width,height);banner.draw(new Canvas(rendered));
            for(int x:new int[]{0,width/2,width-1})for(int y:new int[]{0,height/2,height-1}){
                assertEquals("Banner must fill the complete launcher rectangle at "+width+"x"+height+" ("+x+","+y+")",
                        255,android.graphics.Color.alpha(rendered.getPixel(x,y)));
            }
            rendered.recycle();
        }
    }

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

    @Test public void legalDestinationsAndSelectedSourceUseCellBackgrounds() {
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
            for (int y : new int[]{455, 555}) {
                assertEquals("A legal square has one wash, without a central dot",
                        rendered.getPixel(417, y), rendered.getPixel(455, y));
                assertEquals("The wash extends across the empty destination",
                        rendered.getPixel(455, y), rendered.getPixel(493, y));
            }
            assertNotEquals("e4 differs from an ordinary ivory square", 0xffeee3c8,
                    rendered.getPixel(455, 455));
            assertNotEquals("e3 differs from an ordinary green square", 0xff486a4d,
                    rendered.getPixel(455, 555));
            assertEquals("Selected source has no inner border",
                    rendered.getPixel(417, 619), rendered.getPixel(435, 619));
            assertTrue(board.getContentDescription().toString().contains("e2"));
            rendered.recycle();
        });
    }

    @Test public void remoteFocusUsesOneOutlineAboveTheSelectedCellWash() {
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
                int focus = source == 52 ? 0xff173d2a : 0xfffff1d0;
                assertEquals("Focus adapts to the original square color", focus,
                        rendered.getPixel(x + 50, y + 5));
                assertEquals("Below focus there is only the source wash, without an understroke",
                        rendered.getPixel(x + 12, y + 14), rendered.getPixel(x + 50, y + 14));
                assertEquals("No second inner selection frame remains",
                        rendered.getPixel(x + 12, y + 14), rendered.getPixel(x + 30, y + 14));
                rendered.recycle();
            }
        });
    }

    @Test public void previousMoveAndCheckStayBehindThePieces() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ChessBoardView board = new ChessBoardView(context);
            int spec = android.view.View.MeasureSpec.makeMeasureSpec(810,
                    android.view.View.MeasureSpec.EXACTLY);
            board.measure(spec, spec);
            board.layout(0, 0, 810, 810);
            Bitmap rendered = Bitmap.createBitmap(810, 810, Bitmap.Config.ARGB_8888);
            ChessGame game = new ChessGame();
            game.play("e2e4");
            board.bind(game, 0, -1, "e2e4");
            board.draw(new Canvas(rendered));
            assertNotEquals("The previous source has a background wash", 0xffeee3c8,
                    rendered.getPixel(417, 619));
            assertNotEquals("The previous target has a background wash", 0xffeee3c8,
                    rendered.getPixel(417, 419));
            game = new ChessGame();
            game.play("f2f3");
            game.play("e7e5");
            game.play("g2g4");
            game.play("d8h4");
            board.bind(game, 0, -1, "");
            board.draw(new Canvas(rendered));
            int checkWash = rendered.getPixel(417, 717);
            assertTrue("Check uses a brick background on the green king square",
                    android.graphics.Color.red(checkWash) > android.graphics.Color.green(checkWash));
            assertNotEquals("The king remains visible above check", checkWash,
                    rendered.getPixel(455, 755));
            board.bind(game, 0, -1, "e1e2");
            board.draw(new Canvas(rendered));
            assertEquals("Check replaces previous-move wash instead of stacking it", checkWash,
                    rendered.getPixel(417, 717));
            rendered.recycle();
        });
    }

    @Test public void buttonFocusRetainsItsHoneyOutline() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            android.graphics.drawable.Drawable background = TvStyle.button(context, "OK").getBackground();
            float density = context.getResources().getDisplayMetrics().density;
            int width = Math.round(200 * density), height = Math.round(60 * density);
            background.setBounds(0, 0, width, height);
            background.setState(new int[]{android.R.attr.state_focused});
            Bitmap rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            background.draw(new Canvas(rendered));
            assertEquals("Button focus retains its honey outline", 0xffffcf70,
                    rendered.getPixel(width / 2, Math.round(3 * density)));
            rendered.recycle();
        });
    }

    @Test public void legalCaptureUsesAFullHoneyBackgroundBehindThePiece() {
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
                int wash = rendered.getPixel(centerX - 38, 317);
                assertEquals("The capture background fills the cell instead of drawing a ring",
                        wash, rendered.getPixel(centerX, 317));
                assertEquals("No ring crosses the left side of the target piece",
                        wash, rendered.getPixel(centerX - 42, 355));
                assertNotEquals("A capture differs from the underlying square",
                        light ? 0xffeee3c8 : 0xff486a4d, wash);
                assertNotEquals("The target piece remains visible above the wash",
                        wash, rendered.getPixel(centerX, 355));
                rendered.recycle();
            }
        });
    }
}
