package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.graphics.Canvas;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Memory-only fixtures preserve the child's actual saved session. */
@RunWith(AndroidJUnit4.class)
public final class ParentAndPromotionRemoteTest {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;

    @Test public void settingsOpenWithOnePressAndSaveCancelUseRemoteOnly() throws Exception {
        withFixture(chessSession(),()->{
            launchAndStart();
            key(KeyEvent.KEYCODE_BACK);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.parents));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertFocus(activity.getString(R.string.settings));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertFocus(activity.getString(R.string.addition));
            screenshot("settings-default");
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus("20");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.crossing));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus("save");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(20,controller.state().config.limit);
            assertTrue(controller.state().config.crossingTen);
            assertFocus(activity.getString(R.string.settings));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            key(KeyEvent.KEYCODE_DPAD_RIGHT);
            assertFocus("50");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_BACK);
            assertEquals("Back must discard settings edits",20,controller.state().config.limit);
            assertTrue(controller.state().config.crossingTen);
            assertFocus(activity.getString(R.string.settings));
        });
    }

    @Test public void promotionCancelAndAllFourChoicesUseRemoteOnly() throws Exception {
        char[] choices={'q','r','b','n'};
        for(int choice=0;choice<choices.length;choice++){
            final int index=choice;
            Session fixture=chessSession();
            fixture.moves=Arrays.asList("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2");
            fixture.selected=9;
            fixture.focus=0;
            fixture.promotionTo=0;
            withFixture(fixture,()->{
                launchAndStart();
                assertFocus("promotion0");
                instrumentation.runOnMainSync(()->{
                    String[] names={activity.getString(R.string.queen),activity.getString(R.string.rook),activity.getString(R.string.bishop),activity.getString(R.string.knight)};
                    for(int i=0;i<4;i++){
                        Button choiceButton=(Button)activity.findViewById(android.R.id.content).findViewWithTag("promotion"+i);
                        assertEquals(names[i],choiceButton.getText().toString());
                        assertEquals(names[i],choiceButton.getContentDescription().toString());
                        android.graphics.drawable.Drawable image=choiceButton.getCompoundDrawables()[1];
                        assertNotNull("Each choice must show its matching piece",image);
                        Bitmap actual=Bitmap.createBitmap(image.getBounds().width(),image.getBounds().height(),Bitmap.Config.ARGB_8888);
                        Bitmap expected=Bitmap.createBitmap(actual.getWidth(),actual.getHeight(),Bitmap.Config.ARGB_8888);
                        image.draw(new Canvas(actual));
                        new PiecePainter(activity).draw(new Canvas(expected),Character.toUpperCase(choices[i]),0,0,actual.getWidth());
                        assertTrue("Promotion image must match the named piece",actual.sameAs(expected));
                        actual.recycle();expected.recycle();
                    }
                });
                if(index==0){
                    screenshot("promotion");
                    key(KeyEvent.KEYCODE_BACK);
                    assertEquals(8,controller.state().moves.size());
                    assertEquals(-1,controller.state().promotionTo);
                    assertEquals(9,controller.state().selected);
                    key(KeyEvent.KEYCODE_DPAD_CENTER);
                    assertFocus("promotion0");
                }
                for(int move=0;move<index;move++)key(KeyEvent.KEYCODE_DPAD_RIGHT);
                assertFocus("promotion"+index);
                key(KeyEvent.KEYCODE_DPAD_CENTER);
                assertTrue(controller.state().moves.size()>=9);
                assertEquals("b7a8"+choices[index],controller.state().moves.get(8));
            });
        }
        Session selected=chessSession();
        selected.selected=52;
        withFixture(selected,()->{
            launchAndStart();
            screenshot("chess-selection");
        });
        Session math=new Session();
        math.phase=Session.Phase.MATH;
        math.exercise=new Exercise(19,24,5,Exercise.ADD);
        math.exerciseConfig=math.config.copy();
        math.input="2";
        withFixture(math,()->{
            launchAndStart();
            screenshot("math-compact");
            instrumentation.runOnMainSync(controller::hint);
            awaitSave();
            screenshot("math-hint");
        });
        Session solved=math.copy();
        solved.input="29";
        solved.correct=1;
        solved.solved=true;
        solved.feedback="Goed!";
        withFixture(solved,()->{
            launchAndStart();
            assertTrue(controller.state().solved);
            screenshot("math-success");
        });
    }

    @Test public void remoteMoveGetsOneLegalComputerReply() throws Exception {
        withFixture(chessSession(),()->{
            launchAndStart();
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(52,controller.state().selected);
            key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            long until=SystemClock.elapsedRealtime()+5000;
            AtomicBoolean replied=new AtomicBoolean();
            do {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(()->replied.set(controller.state().moves.size()==2));
                if(!replied.get())SystemClock.sleep(20);
            } while(!replied.get()&&SystemClock.elapsedRealtime()<until);
            assertTrue("Computer must reply without freezing the TV",replied.get());
            assertEquals("e2e4",controller.state().moves.get(0));
            assertTrue(ChessGame.fromHistory(Arrays.asList("e2e4")).legalMoves().contains(controller.state().moves.get(1)));
            assertTrue(controller.game().whiteToMove());
        });
    }

    @Test public void soundAndMotionChoicesUseRemoteAndSurviveProgressReset() throws Exception {
        withFixture(chessSession(),()->{
            launchAndStart();key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);assertFocus("sound-effects");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);assertFocus("piece-motion");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            Field effectsField=MainActivity.class.getDeclaredField("sharedEffects");effectsField.setAccessible(true);
            EffectPreferences choices=(EffectPreferences)effectsField.get(null);
            assertFalse(choices.sound());assertFalse(choices.motion());
            instrumentation.runOnMainSync(controller::resetSession);awaitSave();
            assertFalse(choices.sound());assertFalse(choices.motion());assertTrue(controller.state().moves.isEmpty());
        });
    }

    @Test public void languageSwitchAndRestartKeepGameAndExercise() throws Exception {
        Session fixture=chessSession();
        fixture.moves=Arrays.asList("e2e4","e7e5");
        fixture.phase=Session.Phase.MATH;
        fixture.elapsedMs=42000;
        fixture.exercise=new Exercise(71,32,7,Exercise.SUBTRACT);
        fixture.exerciseConfig=fixture.config.copy();
        fixture.input="2";fixture.hintLevel=1;
        withFixture(fixture,()->{
            launchAndStart();key(KeyEvent.KEYCODE_BACK);
            long elapsed=controller.state().elapsedMs;
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus("language");
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals("Settings",activity.getString(R.string.parent_title));
            screenshot("settings-english");
            assertFocus("language");
            assertEquals(fixture.moves,controller.state().moves);
            assertEquals(elapsed,controller.state().elapsedMs);
            assertEquals(71,controller.state().exercise.id);
            assertEquals("2",controller.state().input);
            assertEquals(1,controller.state().hintLevel);
            assertTrue(controller.state().paused);
            finishAndAwaitDestroyed();
            awaitSave();
            launchAndStart();
            assertEquals("Time for maths",activity.getString(R.string.math_title));
            screenshot("math-english");
            assertEquals(fixture.moves,controller.state().moves);
            assertEquals(71,controller.state().exercise.id);
            instrumentation.runOnMainSync(()->{
                android.widget.TextView hint=(android.widget.TextView)activity.findViewById(android.R.id.content).findViewWithTag("math-hint");
                assertNotNull(hint);
                assertTrue(hint.getText().toString().startsWith("Split 7 into 2 and 5"));
            });
            key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals("Instellingen",activity.getString(R.string.parent_title));
            assertEquals(fixture.moves,controller.state().moves);
            assertEquals(71,controller.state().exercise.id);
        });
    }

    @Test public void newChessGameStartsDirectlyAndKeepsRoundTime() throws Exception {
        Session fixture=chessSession();fixture.moves=Arrays.asList("e2e4","e7e5");fixture.elapsedMs=73000;
        withFixture(fixture,()->{
            launchAndStart();key(KeyEvent.KEYCODE_BACK);
            long elapsed=controller.state().elapsedMs;
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            for(int i=0;i<6;i++)key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.restart_chess));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue(controller.state().moves.isEmpty());
            assertEquals(elapsed,controller.state().elapsedMs);
            instrumentation.runOnMainSync(()->assertTrue(activity.getCurrentFocus() instanceof ChessBoardView));
        });
    }

    @Test public void breakCountdownPausesAndStartsChessWithoutAnExtraAction() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.BREAK;fixture.elapsedMs=59000;
        java.util.concurrent.atomic.AtomicLong clock=new java.util.concurrent.atomic.AtomicLong(1000);
        withFixture(fixture,clock::get,()->{
            launchAndStart();
            instrumentation.runOnMainSync(()->{
                android.widget.TextView countdown=(android.widget.TextView)activity.findViewById(android.R.id.content).findViewWithTag("break-countdown");
                assertNotNull(countdown);assertEquals("00:01",countdown.getText().toString());
                assertEquals(0,activity.findViewById(android.R.id.content).getTouchables().size());
            });
            screenshot("break-countdown");
            key(KeyEvent.KEYCODE_BACK);
            assertTrue(controller.state().paused);
            clock.addAndGet(5000);
            instrumentation.runOnMainSync(controller::tick);awaitSave();
            assertEquals(1000,controller.breakRemainingMs());
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            clock.addAndGet(1000);
            instrumentation.runOnMainSync(controller::tick);awaitSave();
            assertEquals(Session.Phase.CHESS,controller.state().phase);
            assertFalse(controller.state().paused);
            instrumentation.runOnMainSync(()->assertTrue(activity.getCurrentFocus() instanceof ChessBoardView));
        });
    }

    private Session chessSession(){
        Session session=new Session();session.phase=Session.Phase.CHESS;return session;
    }

    private interface Checked { void run() throws Exception; }
    private void withFixture(Session fixture,Checked test) throws Exception {
        withFixture(fixture,SystemClock::elapsedRealtime,test);
    }
    private void withFixture(Session fixture,java.util.function.LongSupplier clock,Checked test) throws Exception {
        Field shared=MainActivity.class.getDeclaredField("sharedController");
        shared.setAccessible(true);
        SessionController previous=(SessionController)shared.get(null);
        Field effectField=MainActivity.class.getDeclaredField("sharedEffects");effectField.setAccessible(true);
        Object previousEffects=effectField.get(null);
        Field languageField=MainActivity.class.getDeclaredField("sharedLanguage");languageField.setAccessible(true);
        Object previousLanguage=languageField.get(null);
        final String[] savedLanguage={"nl"};
        languageField.set(null,new LanguagePreferences(new LanguagePreferences.Storage(){
            public String read(){return savedLanguage[0];}
            public boolean write(String value){savedLanguage[0]=value;return true;}
        }));
        java.util.Map<String,Boolean> choices=new java.util.HashMap<>();
        effectField.set(null,new EffectPreferences(new EffectPreferences.Storage(){
            public boolean read(String key,boolean fallback){return choices.getOrDefault(key,fallback);}
            public boolean write(String key,boolean value){choices.put(key,value);return true;}
        }));
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session state){saved=state.copy();}
        };
        controller=new SessionController(memory,clock,MAIN::post,null);
        shared.set(null,controller);
        try { test.run(); }
        finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            controller.setListener(null);
            awaitSave();
            controller.close();
            shared.set(null,previous);
            effectField.set(null,previousEffects);
            languageField.set(null,previousLanguage);
            activity=null;
        }
    }

    private void launchAndStart() throws Exception {
        Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=(MainActivity)instrumentation.startActivitySync(intent);
        AtomicBoolean focused=new AtomicBoolean();
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->focused.set(activity.hasWindowFocus()&&activity.getCurrentFocus()!=null));
            if(!focused.get())SystemClock.sleep(20);
        } while(!focused.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Window must have focus before Start",focused.get());
        // A previous Activity can still be saving its onPause checkpoint. The
        // Start action intentionally ignores input until that save completes.
        awaitSave();
        assertFocus(activity.getString(controller.state().revision==0?R.string.start:R.string.continue_game));
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        instrumentation.runOnMainSync(()->assertFalse("Start must resume the fixture",controller.state().paused));
    }

    private void finishAndAwaitDestroyed() throws Exception {
        MainActivity previous=activity;
        instrumentation.runOnMainSync(previous::finish);
        AtomicBoolean destroyed=new AtomicBoolean();
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->destroyed.set(previous.isDestroyed()));
            if(!destroyed.get())SystemClock.sleep(20);
        } while(!destroyed.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Previous Activity must finish teardown before relaunch",destroyed.get());
    }

    private void key(int code) throws Exception {
        instrumentation.sendKeyDownUpSync(code);
        instrumentation.waitForIdleSync();
        awaitSave();
    }

    private void assertFocus(String tag){
        instrumentation.runOnMainSync(()->{
            View focused=activity.getCurrentFocus();
            assertNotNull(focused);
            assertEquals(tag,focused.getTag());
        });
    }

    private void awaitSave() throws Exception {
        AtomicBoolean busy=new AtomicBoolean(true);
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->busy.set(controller.busy()));
            if(busy.get())SystemClock.sleep(20);
        } while(busy.get()&&SystemClock.elapsedRealtime()<until);
        assertFalse("Fixture save must finish",busy.get());
    }

    private void screenshot(String name) throws Exception {
        instrumentation.waitForIdleSync();
        // UI-idle does not mean that RenderThread submitted the new window buffer.
        CountDownLatch frame=new CountDownLatch(1);
        instrumentation.runOnMainSync(()->{
            View decor=activity.getWindow().getDecorView();
            if(android.os.Build.VERSION.SDK_INT>=29) {
                decor.getViewTreeObserver().registerFrameCommitCallback(frame::countDown);
                decor.invalidate();
            } else decor.postOnAnimation(()->decor.postOnAnimation(frame::countDown));
        });
        assertTrue("Expected screen must reach the display before capture",frame.await(3,TimeUnit.SECONDS));
        instrumentation.getUiAutomation().waitForIdle(250,5000);
        Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Screenshot should be available",bitmap);
        File directory=new File(instrumentation.getTargetContext().getExternalFilesDir(null),"evidence");
        assertTrue(directory.isDirectory()||directory.mkdirs());
        try(FileOutputStream output=new FileOutputStream(new File(directory,name+".png"))){
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));
        } finally { bitmap.recycle(); }
    }
}
