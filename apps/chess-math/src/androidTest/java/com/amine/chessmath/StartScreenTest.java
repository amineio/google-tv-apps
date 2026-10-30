package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class StartScreenTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private SessionController controller;
    private MainActivity activity;

    @Test public void savedGameCanStartNewOnlyAfterConfirmation() throws Exception {
        withSavedGame(()->{
            String savedId=controller.state().id;
            instrumentation.runOnMainSync(()->{
                assertEquals(activity.getString(R.string.continue_game),activity.getCurrentFocus().getTag());
                View startNew=activity.findViewById(android.R.id.content).findViewWithTag(activity.getString(R.string.new_game));
                assertNotNull("Saved game must offer New game",startNew);
            });
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            instrumentation.runOnMainSync(()->assertEquals(activity.getString(R.string.new_game),activity.getCurrentFocus().getTag()));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_CENTER); // Select the computer before final replacement consent.
            awaitWindowFocus(false);
            assertEquals(savedId,controller.state().id);
            assertEquals(Arrays.asList("e2e4","e7e5"),controller.state().moves);
            key(KeyEvent.KEYCODE_DPAD_CENTER); // Cancel has the initial dialog focus.
            awaitWindowFocus(true);
            assertEquals(savedId,controller.state().id);
            assertEquals(Arrays.asList("e2e4","e7e5"),controller.state().moves);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            awaitWindowFocus(false);
            key(KeyEvent.KEYCODE_DPAD_RIGHT);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            awaitWindowFocus(true);
            assertNotEquals(savedId,controller.state().id);
            assertTrue(controller.state().moves.isEmpty());
            assertEquals(0,controller.state().correct);
            assertEquals(Session.Phase.MATH,controller.state().phase);
            assertNotNull(controller.state().exercise);
            assertFalse("New game must start immediately",controller.state().paused);
            instrumentation.runOnMainSync(()->assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("math-keypad")));
        });
    }

    @Test public void continueKeepsSavedGame() throws Exception {
        withSavedGame(()->{
            String savedId=controller.state().id;
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(savedId,controller.state().id);
            assertEquals(Arrays.asList("e2e4","e7e5"),controller.state().moves);
            assertFalse(controller.state().paused);
        });
    }
    private void awaitWindowFocus(boolean expected) throws Exception {
        AtomicBoolean ready=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->ready.set(activity.hasWindowFocus()==expected));
            if(!ready.get())SystemClock.sleep(20);
        }while(!ready.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Dialog focus transition must finish before the next remote press",ready.get());
    }

    @Test public void titleSettingsReturnWithoutStartingGameAndPauseHasNoMute() throws Exception {
        withSavedGame(()->{
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);
            instrumentation.runOnMainSync(()->assertEquals(activity.getString(R.string.parents),activity.getCurrentFocus().getTag()));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            instrumentation.runOnMainSync(()->assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("sound-effects")));
            assertTrue("Opening title Settings must keep the game paused",controller.state().paused);
            key(KeyEvent.KEYCODE_BACK);
            instrumentation.runOnMainSync(()->assertEquals(activity.getString(R.string.continue_game),activity.getCurrentFocus().getTag()));
            assertTrue(controller.state().paused);
            assertEquals(Arrays.asList("e2e4","e7e5"),controller.state().moves);
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_BACK);
            instrumentation.runOnMainSync(()->assertNull(activity.findViewById(android.R.id.content).findViewWithTag("pause-sound")));
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            instrumentation.runOnMainSync(()->assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("sound-effects")));
        });
    }

    @Test public void settingsUseNativeSwitchesForBinaryChoices() throws Exception {
        withSavedGame(()->{
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            instrumentation.runOnMainSync(()->{
                assertTrue("Sound must use a switch",activity.findViewById(android.R.id.content).findViewWithTag("sound-effects") instanceof android.widget.Switch);
                assertTrue("Movement must use a switch",activity.findViewById(android.R.id.content).findViewWithTag("piece-motion") instanceof android.widget.Switch);
            });
        });
    }

    @Test public void settingsGroupsAndAboutShowInstalledVersionInBothLanguages() throws Exception {
        Field language=MainActivity.class.getDeclaredField("sharedLanguage");language.setAccessible(true);
        Object previous=language.get(null);
        try {
            for(String locale:new String[]{"nl","en"}){
                language.set(null,new LanguagePreferences(new LanguagePreferences.Storage(){
                    public String read(){return locale;}
                    public boolean write(String value){return true;}
                }));
                withSavedGame(()->{
                    key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
                    instrumentation.runOnMainSync(()->{
                        View root=activity.findViewById(android.R.id.content);
                        assertNull("Version belongs in About",root.findViewWithTag("app-version"));
                        for(String tag:new String[]{"settings-play","settings-app","settings-session","settings-information"}){
                            View heading=root.findViewWithTag(tag);assertNotNull(tag,heading);assertFalse(heading.isFocusable());
                        }
                    });
                    capture("settings-"+locale);
                    for(int i=0;i<20;i++){
                        AtomicBoolean found=new AtomicBoolean();
                        instrumentation.runOnMainSync(()->found.set(activity.getString(R.string.about_game).equals(activity.getCurrentFocus().getTag())));
                        if(found.get())break;
                        key(KeyEvent.KEYCODE_DPAD_DOWN);
                    }
                    key(KeyEvent.KEYCODE_DPAD_CENTER);
                    android.content.pm.PackageInfo installed=instrumentation.getTargetContext().getPackageManager()
                            .getPackageInfo(instrumentation.getTargetContext().getPackageName(),0);
                    instrumentation.runOnMainSync(()->{
                        View root=activity.findViewById(android.R.id.content);
                        android.widget.TextView version=root.findViewWithTag("app-version"),build=root.findViewWithTag("app-build");
                        assertNotNull(version);assertNotNull(build);assertTrue(version.isShown());assertTrue(build.isShown());
                        assertEquals(activity.getString(R.string.app_version,installed.versionName),version.getText().toString());
                        assertEquals(activity.getString(R.string.app_build,installed.versionCode),build.getText().toString());
                        assertNotNull(root.findViewWithTag("about-app-name"));
                    });
                    capture("about-"+locale);
                    key(KeyEvent.KEYCODE_BACK);
                    instrumentation.runOnMainSync(()->assertNotNull(activity.findViewById(android.R.id.content).findViewWithTag("maths-practice")));
                });
            }
        } finally {language.set(null,previous);}
    }

    private void capture(String name) throws Exception {
        java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);
        instrumentation.runOnMainSync(()->{
            View decor=activity.getWindow().getDecorView();
            if(android.os.Build.VERSION.SDK_INT>=29){decor.getViewTreeObserver().registerFrameCommitCallback(frame::countDown);decor.invalidate();}
            else decor.postOnAnimation(()->decor.postOnAnimation(frame::countDown));
        });
        assertTrue(frame.await(3,java.util.concurrent.TimeUnit.SECONDS));
        instrumentation.getUiAutomation().waitForIdle(250,5000);
        android.graphics.Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        java.io.File folder=new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"release-v09");
        assertTrue(folder.isDirectory()||folder.mkdirs());
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        } finally {bitmap.recycle();}
    }

    @Test public void largeTextKeepsEveryStartActionReachable() throws Exception {
        withSavedGame(()->{
            android.content.res.Configuration previous=new android.content.res.Configuration(activity.getResources().getConfiguration());
            try {
                instrumentation.runOnMainSync(()->{
                    android.content.res.Configuration large=new android.content.res.Configuration(previous);large.fontScale=2f;
                    activity.getResources().updateConfiguration(large,null);
                    try {
                        java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);
                    } catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
                for(int label:new int[]{R.string.continue_game,R.string.new_game,R.string.parents,R.string.exit}){
                    instrumentation.runOnMainSync(()->{
                        View focused=activity.getCurrentFocus();
                        assertEquals(activity.getString(label),focused.getTag());
                        android.graphics.Rect visible=new android.graphics.Rect();
                        assertTrue("Focused action must be visible",focused.getGlobalVisibleRect(visible));
                        assertEquals("Focused action must not be clipped",focused.getHeight(),visible.height());
                    });
                    key(KeyEvent.KEYCODE_DPAD_DOWN);
                }
            } finally {
                instrumentation.runOnMainSync(()->activity.getResources().updateConfiguration(previous,null));
            }
        });
    }

    private interface Checked {void run() throws Exception;}
    private void withSavedGame(Checked test) throws Exception {
        Field shared=MainActivity.class.getDeclaredField("sharedController");shared.setAccessible(true);
        Object previous=shared.get(null);
        Session fixture=new Session();fixture.revision=7;fixture.phase=Session.Phase.CHESS;
        fixture.moves=Arrays.asList("e2e4","e7e5");fixture.correct=3;
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session value){saved=value.copy();}
        };
        controller=new SessionController(memory,()->1000L,new Handler(Looper.getMainLooper())::post,null);
        shared.set(null,controller);
        try {
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            AtomicBoolean focused=new AtomicBoolean();
            long until=SystemClock.elapsedRealtime()+5000;
            do {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(()->focused.set(activity.hasWindowFocus()&&activity.getCurrentFocus()!=null));
                if(!focused.get())SystemClock.sleep(20);
            } while(!focused.get()&&SystemClock.elapsedRealtime()<until);
            assertTrue("Start screen must receive remote focus",focused.get());
            awaitSave();
            test.run();
        } finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            awaitSave();controller.setListener(null);controller.close();shared.set(null,previous);
        }
    }
    private void key(int code) throws Exception {
        instrumentation.sendKeyDownUpSync(code);instrumentation.waitForIdleSync();awaitSave();
    }
    private void awaitSave() throws Exception {
        AtomicBoolean busy=new AtomicBoolean(true);
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->busy.set(controller.busy()));
            if(busy.get())SystemClock.sleep(20);
        } while(busy.get()&&SystemClock.elapsedRealtime()<until);
        assertFalse("Session save must finish",busy.get());
    }
}
