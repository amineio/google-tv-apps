package com.amine.chessmath;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import android.view.KeyEvent;

/** Remote navigation must never wrap around a board edge. */
@RunWith(AndroidJUnit4.class)
public final class RemoteFocusTest {
    @Test public void testBoardArrowsClampAtEveryCorner() throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ChessBoardView board = new ChessBoardView(InstrumentationRegistry.getInstrumentation().getTargetContext());
            board.setSquare(0);
            board.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, new KeyEvent(0, KeyEvent.KEYCODE_DPAD_UP));
            board.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0, KeyEvent.KEYCODE_DPAD_LEFT));
            assertEquals(0, board.square());
            for (int n = 0; n < 20; n++) board.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0, KeyEvent.KEYCODE_DPAD_RIGHT));
            assertEquals(7, board.square());
            for (int n = 0; n < 20; n++) board.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, new KeyEvent(0, KeyEvent.KEYCODE_DPAD_DOWN));
            assertEquals(63, board.square());
            for (int n = 0; n < 20; n++) board.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0, KeyEvent.KEYCODE_DPAD_LEFT));
            assertEquals(56, board.square());
        });
    }
}
