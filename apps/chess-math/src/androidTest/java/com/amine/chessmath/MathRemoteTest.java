package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Inject remote events through the window, including its default focus navigation. */
@RunWith(AndroidJUnit4.class)
public final class MathRemoteTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());

    @Test public void testStartCanBeReachedWithRemoteOnly() throws Exception {
        withFixture(new Session(),()->{
            launch();
            assertStartFocus();
            key(KeyEvent.KEYCODE_DPAD_UP);
            assertStartFocus();
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.parents));
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.exit));
        });
    }

    @Test public void allDigitsAndDeleteRemainReachableWithoutSubmitOrNext() throws Exception {
        Session fixture=new Session();
        fixture.phase=Session.Phase.MATH;
        fixture.exercise=new Exercise(32,24,5,Exercise.ADD);
        fixture.exerciseConfig=fixture.config.copy();
        withFixture(fixture,()->{
            launch();
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            awaitSave(controller);
            assertFocus("pad4");
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
            assertFocus("pad10");
            // Walk every row through actual remote events, including the short bottom row.
            for(int n=0;n<3;n++)key(KeyEvent.KEYCODE_DPAD_UP);
            assertFocus("pad1");
            for(int row=0;row<3;row++){
                key(KeyEvent.KEYCODE_DPAD_LEFT);
                key(KeyEvent.KEYCODE_DPAD_LEFT);
                assertFocus("pad"+(row*3));
                key(KeyEvent.KEYCODE_DPAD_RIGHT);
                assertFocus("pad"+(row*3+1));
                key(KeyEvent.KEYCODE_DPAD_RIGHT);
                assertFocus("pad"+(row*3+2));
                if(row<2)key(KeyEvent.KEYCODE_DPAD_DOWN);
            }
            instrumentation.runOnMainSync(()->{
                View root=activity.findViewById(android.R.id.content);
                assertNull(root.findViewWithTag("pad11"));
                assertNull(root.findViewWithTag("next"));
                assertTrue(root.findViewWithTag("math-keypad").getWidth()
                        <root.findViewWithTag("math-reading").getWidth());
                ((Button)root.findViewWithTag("pad1")).performClick();
            });
            awaitSave(controller);
            assertEquals("2",controller.state().input);
            assertFalse(controller.state().solved);
            instrumentation.runOnMainSync(()->((Button)activity.findViewById(android.R.id.content).findViewWithTag("pad9")).performClick());
            awaitSave(controller);
            assertEquals("",controller.state().input);
            instrumentation.runOnMainSync(()->controller.digit(2));
            awaitSave(controller);
            instrumentation.runOnMainSync(()->controller.digit(9));
            awaitSave(controller);
            assertTrue(controller.state().solved);
            assertEquals(1,controller.state().correct);
            instrumentation.runOnMainSync(()->{
                View root=activity.findViewById(android.R.id.content);
                assertEquals(activity.getString(R.string.math_success),((TextView)root.findViewWithTag("math-feedback")).getText().toString());
                assertNull(root.findViewWithTag("pad0"));
                assertNull(root.findViewWithTag("next"));
            });
            long until=SystemClock.elapsedRealtime()+5000;
            while(controller.state().exercise.id==fixture.exercise.id&&SystemClock.elapsedRealtime()<until){
                SystemClock.sleep(30);
                instrumentation.waitForIdleSync();
            }
            assertNotEquals("A new question appears without another action",fixture.exercise.id,controller.state().exercise.id);
            assertFocus("pad8");
        });
    }

    private interface Checked { void run() throws Exception; }
    private void withFixture(Session fixture,Checked test) throws Exception {
        Field shared=MainActivity.class.getDeclaredField("sharedController");
        shared.setAccessible(true);
        SessionController previous=(SessionController)shared.get(null);
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session state){saved=state.copy();}
        };
        controller=new SessionController(memory,SystemClock::elapsedRealtime,MAIN::post,null);
        shared.set(null,controller);
        try { test.run(); }
        finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            controller.setListener(null);
            awaitSave(controller);
            controller.close();
            shared.set(null,previous);
            activity=null;
        }
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
