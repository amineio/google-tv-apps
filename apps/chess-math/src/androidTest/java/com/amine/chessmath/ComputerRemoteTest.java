package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ComputerRemoteTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private SessionController controller;
    private MainActivity activity;
    @Test public void settingsKeepRemoteFocusAndDoNotOfferArtificialDifficulty() throws Exception {
        withFixture(()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            for(int i=0;i<20;i++)key(KeyEvent.KEYCODE_DPAD_DOWN);
            instrumentation.runOnMainSync(()->{
                assertNull(activity.findViewById(android.R.id.content).findViewWithTag("chess-difficulty"));
                assertEquals(activity.getString(R.string.exit),activity.getCurrentFocus().getTag());
            });
        });
    }

    @Test public void computerRepliesLegallyAndRemoteRemainsResponsive() throws Exception {
        withFixture(()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            AtomicBoolean replied=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+5000;
            do {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(()->replied.set(controller.state().moves.size()==2&&controller.presentation()==null));
                if(!replied.get())SystemClock.sleep(20);
            } while(!replied.get()&&SystemClock.elapsedRealtime()<until);
            assertTrue("Computer must reply within five seconds",replied.get());
            assertEquals(2,controller.state().moves.size());
            assertEquals("e2e4",controller.state().moves.get(0));
            assertTrue(ChessGame.fromHistory(Arrays.asList("e2e4")).legalMoves().contains(controller.state().moves.get(1)));
            key(KeyEvent.KEYCODE_BACK);
            instrumentation.runOnMainSync(()->assertEquals(activity.getString(R.string.resume),activity.getCurrentFocus().getTag()));
            assertTrue("Remote Back must pause after the reply",controller.state().paused);
        });
    }

    private interface Checked {void run() throws Exception;}
    private Field field(String name) throws Exception {Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;}
    private void withFixture(Checked test) throws Exception {
        Field shared=field("sharedController"),language=field("sharedLanguage"),effects=field("sharedEffects");
        Object previous=shared.get(null),previousLanguage=language.get(null),previousEffects=effects.get(null);
        Session fixture=new Session();fixture.revision=7;fixture.phase=Session.Phase.CHESS;
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session value){saved=value.copy();}
        };
        controller=new SessionController(memory,SystemClock::elapsedRealtime,new Handler(Looper.getMainLooper())::post,null);
        shared.set(null,controller);
        effects.set(null,new EffectPreferences(new EffectPreferences.Storage(){
            public boolean read(String key,boolean fallback){return false;}
            public boolean write(String key,boolean value){return true;}
        }));
        language.set(null,new LanguagePreferences(new LanguagePreferences.Storage(){
            public String read(){return "en";}
            public boolean write(String value){return true;}
        }));
        try {
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            awaitFocus();awaitSave();test.run();
        } finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            awaitSave();controller.setListener(null);controller.close();
            shared.set(null,previous);language.set(null,previousLanguage);effects.set(null,previousEffects);
        }
    }
    private void awaitFocus() throws Exception {
        AtomicBoolean focused=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->focused.set(activity.hasWindowFocus()&&activity.getCurrentFocus()!=null));
            if(!focused.get())SystemClock.sleep(20);
        } while(!focused.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Window must receive remote focus",focused.get());
    }
    private void key(int code) throws Exception {
        instrumentation.sendKeyDownUpSync(code);instrumentation.waitForIdleSync();awaitSave();
    }
    private void awaitSave() throws Exception {
        AtomicBoolean busy=new AtomicBoolean(true);long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->busy.set(controller.busy()));
            if(busy.get())SystemClock.sleep(20);
        } while(busy.get()&&SystemClock.elapsedRealtime()<until);
        assertFalse("Session save must finish",busy.get());
    }
}
