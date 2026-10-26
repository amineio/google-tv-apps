package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Inject remote events through the window, including its default focus navigation. */
@RunWith(AndroidJUnit4.class)
public final class MathRemoteTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;

    @After public void tearDown() {
        if(activity!=null)instrumentation.runOnMainSync(activity::finish);
    }

    @Test public void testStartCanBeReachedWithRemoteOnly() throws Exception {
        launch();
        assertStartFocus();
        key(KeyEvent.KEYCODE_DPAD_UP);
        assertStartFocus();
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        assertFocus(activity.getString(R.string.exit));
    }

    @Test public void testKeypadEdgesAndHintReturnPreserveFocus() throws Exception {
        launch();
        Field field=MainActivity.class.getDeclaredField("controller");
        field.setAccessible(true);
        SessionController controller=(SessionController)field.get(activity);
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        awaitSave(controller);
        instrumentation.runOnMainSync(controller::skipToMath);
        awaitSave(controller);
        instrumentation.runOnMainSync(controller::resume);
        awaitSave(controller);
        assertFocus("pad4");
        instrumentation.runOnMainSync(()->{
            View five=activity.findViewById(android.R.id.content).findViewWithTag("pad4");
            assertNotNull("Math keypad must be visible",five);
            five.requestFocus();
        });
        key(KeyEvent.KEYCODE_DPAD_UP);
        assertFocus("pad1");
        key(KeyEvent.KEYCODE_DPAD_UP);
        assertFocus("hint");
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        assertFocus("pad1");
        for(int n=0;n<10;n++)key(KeyEvent.KEYCODE_DPAD_LEFT);
        assertFocus("pad0");
        for(int n=0;n<10;n++)key(KeyEvent.KEYCODE_DPAD_DOWN);
        assertFocus("pad9");
        for(int n=0;n<10;n++)key(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertFocus("pad11");
    }

    private void launch() throws Exception {
        Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=(MainActivity)instrumentation.startActivitySync(intent);
        AtomicBoolean ready=new AtomicBoolean();
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->ready.set(activity.hasWindowFocus()&&activity.getCurrentFocus()!=null));
            if(!ready.get())SystemClock.sleep(20);
        } while(!ready.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Activity window must have focus before remote input",ready.get());
    }

    private void key(int code) {
        instrumentation.sendKeyDownUpSync(code);
        instrumentation.waitForIdleSync();
    }

    private void assertFocus(String expected) {
        instrumentation.runOnMainSync(()->{
            assertNotNull(activity.getCurrentFocus());
            assertEquals(expected,activity.getCurrentFocus().getTag());
        });
    }

    private void assertStartFocus() {
        instrumentation.runOnMainSync(()->{
            assertNotNull(activity.getCurrentFocus());
            Object tag=activity.getCurrentFocus().getTag();
            assertTrue(activity.getString(R.string.start).equals(tag)
                    ||activity.getString(R.string.continue_game).equals(tag));
        });
    }

    private void awaitSave(SessionController controller) throws Exception {
        AtomicBoolean busy=new AtomicBoolean(true);
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->busy.set(controller.busy()));
            if(busy.get())SystemClock.sleep(20);
        } while(busy.get()&&SystemClock.elapsedRealtime()<until);
        assertFalse("Save should finish",busy.get());
    }
}
