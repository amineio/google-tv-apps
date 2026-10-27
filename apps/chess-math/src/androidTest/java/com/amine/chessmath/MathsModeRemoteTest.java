package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Switch;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real preference writes are restored after each memory-backed game fixture. */
@RunWith(AndroidJUnit4.class)
public final class MathsModeRemoteTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;

    @Test public void disabledMathsSurvivesRecreationAndNewSessionAndKeepsUnfinishedChess() throws Exception {
        Session fixture=new Session();fixture.revision=7;
        fixture.moves=Arrays.asList("e2e4","e7e5");
        withFixture(fixture,true,false,()->{
            openTitleSettings();assertMathsSwitch(true);
            key(KeyEvent.KEYCODE_DPAD_CENTER);assertMathsSwitch(false);
            assertTrue("Settings must not start a game",controller.state().paused);
            Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
            try {
                field("sharedPlay").set(null,null);
                instrumentation.runOnMainSync(activity::recreate);
                activity=(MainActivity)monitor.waitForActivityWithTimeout(5000);assertNotNull(activity);awaitReady();
                assertMathsSwitch(false);assertFalse(controller.mathsEnabled());
            } finally {instrumentation.removeMonitor(monitor);}
            key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(Session.Phase.CHESS,controller.state().phase);
            assertEquals(Arrays.asList("e2e4","e7e5"),controller.state().moves);
            instrumentation.runOnMainSync(controller::resetSession);awaitSave();
            assertEquals(Session.Phase.CHESS,controller.state().phase);
            assertFalse(((PlayPreferences)field("sharedPlay").get(null)).mathsEnabled());
            instrumentation.runOnMainSync(controller::resume);awaitSave();
            assertTrue(controller.state().moves.isEmpty());assertEquals(Session.Phase.CHESS,controller.state().phase);
        });
    }

    @Test public void finishedChessOnlyGameOffersAnotherMatchWithoutPracticeOrBreak() throws Exception {
        Session fixture=new Session();fixture.revision=7;fixture.phase=Session.Phase.CHESS;
        fixture.moves=Arrays.asList("f2f3","e7e5","g2g4","d8h4");
        fixture.result=ChessGame.fromHistory(fixture.moves).result();
        withFixture(fixture,false,false,()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            instrumentation.runOnMainSync(()->{
                android.widget.Button next=activity.findViewById(android.R.id.content).findViewWithTag("game-continue");
                assertEquals(activity.getString(R.string.new_game),next.getText().toString());
                assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("board"));
            });
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(Session.Phase.CHESS,controller.state().phase);assertFalse(controller.state().paused);
            assertTrue(controller.state().moves.isEmpty());assertTrue(controller.state().result.isEmpty());
        });
    }

    @Test public void rejectedPreferenceWriteRestoresSwitchAndKeepsMathsEnabled() throws Exception {
        withFixture(new Session(),true,true,()->{
            openTitleSettings();key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue(controller.mathsEnabled());assertTrue(((PlayPreferences)field("sharedPlay").get(null)).mathsEnabled());
            key(KeyEvent.KEYCODE_BACK);assertMathsSwitch(true);
            assertEquals(Session.Phase.MATH,controller.state().phase);
        });
    }

    private void openTitleSettings() throws Exception {
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        if(controller.state().revision!=0)key(KeyEvent.KEYCODE_DPAD_DOWN);
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        instrumentation.runOnMainSync(()->assertEquals("maths-practice",activity.getCurrentFocus().getTag()));
    }
    private void assertMathsSwitch(boolean enabled){
        instrumentation.runOnMainSync(()->{
            Switch control=activity.findViewById(android.R.id.content).findViewWithTag("maths-practice");
            assertNotNull(control);assertEquals(enabled,control.isChecked());
        });
    }
    private interface Checked {void run() throws Exception;}
    private Field field(String name) throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private void withFixture(Session fixture,boolean enabled,boolean failWrites,Checked test) throws Exception {
        Context context=instrumentation.getTargetContext();
        SharedPreferences preferences=context.getSharedPreferences("play",Context.MODE_PRIVATE);
        boolean existed=preferences.contains("maths_enabled"),priorValue=preferences.getBoolean("maths_enabled",true);
        Field shared=field("sharedController"),play=field("sharedPlay");
        Object previous=shared.get(null),previousPlay=play.get(null);
        assertTrue(preferences.edit().putBoolean("maths_enabled",enabled).commit());
        PlayPreferences choice=failWrites?new PlayPreferences(new PlayPreferences.Storage(){
            public boolean read(String key,boolean fallback){return enabled;}
            public boolean write(String key,boolean value){return false;}
        }):new PlayPreferences(context);
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session value){saved=value.copy();}
        };
        controller=new SessionController(memory,SystemClock::elapsedRealtime,new Handler(Looper.getMainLooper())::post,null);
        shared.set(null,controller);play.set(null,choice);
        try {
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            awaitReady();test.run();
        } finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            awaitSave();controller.setListener(null);controller.close();
            shared.set(null,previous);play.set(null,previousPlay);
            SharedPreferences.Editor editor=preferences.edit();
            if(existed)editor.putBoolean("maths_enabled",priorValue);else editor.remove("maths_enabled");
            assertTrue(editor.commit());
        }
    }
    private void key(int code) throws Exception {instrumentation.sendKeyDownUpSync(code);instrumentation.waitForIdleSync();awaitSave();}
    private void awaitReady() throws Exception {
        AtomicBoolean ready=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->ready.set(activity.hasWindowFocus()&&activity.getCurrentFocus()!=null&&!controller.busy()));
            if(!ready.get())SystemClock.sleep(20);
        } while(!ready.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Activity must be ready for remote input",ready.get());
    }
    private void awaitSave() throws Exception {
        AtomicBoolean busy=new AtomicBoolean(true);long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->busy.set(controller.busy()));
            if(busy.get())SystemClock.sleep(20);
        } while(busy.get()&&SystemClock.elapsedRealtime()<until);
        assertFalse("Save must finish",busy.get());
    }
}
