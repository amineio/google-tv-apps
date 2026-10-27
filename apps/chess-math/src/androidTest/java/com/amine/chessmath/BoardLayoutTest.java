package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** These layout fixtures use memory persistence and preserve the real saved game. */
@RunWith(AndroidJUnit4.class)
public final class BoardLayoutTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;

    @Test public void boardIsAtTheTvCenterWithoutSideMessages() throws Exception {
        Session fresh=new Session();fresh.phase=Session.Phase.CHESS;
        withFixture(fresh,()->{
            launchAndStart();
            instrumentation.runOnMainSync(()->{
                assertGeometry();
                assertNotNull(view("board").getContentDescription());
            });
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(52,controller.state().selected);
            instrumentation.runOnMainSync(()->{
                assertGeometry();
            });
            // A new Activity must bind the same in-memory session and geometry.
            finishAndAwaitPause();
            launchAndStart();
            assertEquals(52,controller.state().selected);
            instrumentation.runOnMainSync(this::assertGeometry);
        });
    }

    @Test public void largerFontsKeepTheBoardCenteredWithoutSideMessages() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.CHESS;
        fixture.selected=52;
        fixture.feedback="Deze zet kan niet.";
        withFixture(fixture,()->{
            launchAndStart();
            Configuration previous=new Configuration(activity.getResources().getConfiguration());
            Field screen=MainActivity.class.getDeclaredField("screen");
            screen.setAccessible(true);
            Method render=MainActivity.class.getDeclaredMethod("render");
            render.setAccessible(true);
            try {
                for(float scale:new float[]{1.6f,2.0f}){
                    instrumentation.runOnMainSync(()->{
                        Configuration larger=new Configuration(previous);
                        larger.fontScale=scale;
                        activity.getResources().updateConfiguration(larger,null);
                        try {
                            // Recreate only the board views under this app-local font scale.
                            screen.set(activity,"");
                            render.invoke(activity);
                        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
                    });
                    instrumentation.waitForIdleSync();
                    instrumentation.runOnMainSync(()->{
                        assertGeometry();
                    });
                }
            } finally {
                instrumentation.runOnMainSync(()->activity.getResources().updateConfiguration(previous,null));
            }
        });
    }

    @Test public void chessSaveStaysQuietAndPersistsSelectionWithoutLosingFocus() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.CHESS;
        DelayedPersistence memory=new DelayedPersistence(fixture);
        // Keep this fixture below the separate slow-save warning threshold.
        withFixture(memory,()->0L,()->{
            launchAndStart();
            View board=view("board");
            Field savingField=MainActivity.class.getDeclaredField("savingText");
            savingField.setAccessible(true);
            View saving=(View)savingField.get(activity);
            long revision=memory.saved.revision;
            memory.delayNextSave=true;
            try {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                assertTrue("Selection must start a real save",memory.entered.await(5,TimeUnit.SECONDS));
                CountDownLatch refreshed=new CountDownLatch(1);
                // The Activity updates its Saving indicator on its 250 ms UI timer.
                MAIN.postDelayed(refreshed::countDown,350);
                assertTrue("UI timer must run while persistence is blocked",refreshed.await(2,TimeUnit.SECONDS));
                instrumentation.runOnMainSync(()->{
                    assertTrue("Save must remain pending until released",controller.busy());
                    assertNull("Pending save must not report an error",controller.error());
                    assertEquals("Routine Saving message must stay hidden",View.GONE,saving.getVisibility());
                    assertSame(board,activity.getCurrentFocus());
                    assertEquals(52,controller.state().focus);
                    assertEquals("Pending selection must not publish before persistence",-1,controller.state().selected);
                });
                assertEquals("Blocked save must not commit early",revision,memory.saved.revision);
            } finally {
                memory.release.countDown();
            }
            awaitSave();
            instrumentation.runOnMainSync(()->{
                assertEquals(52,controller.state().selected);
                assertEquals(52,controller.state().focus);
                assertSame("Save must preserve the board view",board,view("board"));
                assertSame(board,activity.getCurrentFocus());
                assertEquals(View.GONE,saving.getVisibility());
                assertGeometry();
            });
            assertEquals("Selection must reach persistence",52,memory.saved.selected);
            assertEquals(52,memory.saved.focus);
            assertTrue("Save must advance the persisted revision",memory.saved.revision>revision);
        });
    }

    @Test public void mathSaveStaysQuietWhileAnswerIsPersisted() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.MATH;
        fixture.exercise=new Exercise(123,7,0,Exercise.ADD);fixture.exerciseConfig=fixture.config.copy();
        DelayedPersistence memory=new DelayedPersistence(fixture);
        withFixture(memory,()->0L,()->{
            launchAndStart();
            Field savingField=MainActivity.class.getDeclaredField("savingText");savingField.setAccessible(true);
            memory.delayNextSave=true;
            try {
                instrumentation.runOnMainSync(()->controller.digit(1));
                assertTrue(memory.entered.await(5,TimeUnit.SECONDS));
                CountDownLatch refreshed=new CountDownLatch(1);MAIN.postDelayed(refreshed::countDown,350);
                assertTrue(refreshed.await(2,TimeUnit.SECONDS));
                instrumentation.runOnMainSync(()->{
                    assertTrue(controller.busy());assertNull(controller.error());
                    try {assertEquals("Routine Saving must stay hidden during maths",View.GONE,((View)savingField.get(activity)).getVisibility());}
                    catch(IllegalAccessException e){throw new AssertionError(e);}
                    assertNotNull(view("math-keypad"));
                });
            } finally {memory.release.countDown();}
            awaitSave();assertEquals("1",memory.saved.input);
        });
    }

    @Test public void mathsSaveFailureStillOffersVisibleRetry() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.MATH;
        fixture.exercise=new Exercise(123,7,0,Exercise.ADD);fixture.exerciseConfig=fixture.config.copy();
        DelayedPersistence memory=new DelayedPersistence(fixture);
        withFixture(memory,()->0L,()->{
            launchAndStart();memory.failNextSave=true;
            instrumentation.runOnMainSync(()->controller.digit(1));awaitSave();
            assertNotNull("Storage failure must remain visible",controller.error());
            instrumentation.runOnMainSync(()->assertTrue(view(activity.getString(R.string.retry)).isShown()));
        });
    }

    @Test public void chessStaysPlayableAcrossFiveMinuteMarkAndLater() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.CHESS;fixture.elapsedMs=299900;
        java.util.concurrent.atomic.AtomicLong now=new java.util.concurrent.atomic.AtomicLong(0);
        DelayedPersistence memory=new DelayedPersistence(fixture);
        withFixture(memory,()->SystemClock.elapsedRealtime()+now.get(),()->{
            launchAndStart();
            instrumentation.runOnMainSync(()->{now.addAndGet(200);controller.tick();});awaitSave();
            assertEquals(Session.Phase.CHESS,controller.state().phase);assertFalse(controller.state().paused);
            assertFalse(controller.state().chessPending);
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            AtomicBoolean replied=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+6000;
            do {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(()->replied.set(controller.state().moves.size()==2&&controller.presentation()==null&&!controller.busy()));
                if(!replied.get())SystemClock.sleep(20);
            } while(!replied.get()&&SystemClock.elapsedRealtime()<until);
            assertTrue("Black must still reply after five minutes",replied.get());
            assertEquals("e2e4",controller.state().moves.get(0));
            instrumentation.runOnMainSync(()->{now.addAndGet(900000);controller.tick();});awaitSave();
            assertEquals(Session.Phase.CHESS,controller.state().phase);assertFalse(controller.state().paused);
            assertTrue(controller.state().result.isEmpty());assertEquals(2,controller.state().moves.size());
            instrumentation.runOnMainSync(()->assertTrue(view("board").isShown()));
        });
    }

    @Test public void checkmateBoardStaysVisibleUntilExplicitContinue() throws Exception {
        Session fixture=new Session();fixture.phase=Session.Phase.CHESS;
        fixture.moves.addAll(java.util.Arrays.asList("f2f3","e7e5","g2g4"));
        withFixture(fixture,()->{
            launchAndStart();
            AtomicBoolean ended=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+7000;
            do {
                instrumentation.waitForIdleSync();
                instrumentation.runOnMainSync(()->ended.set(!controller.state().result.isEmpty()
                        &&controller.presentation()==null&&!controller.busy()));
                if(!ended.get())SystemClock.sleep(20);
            } while(!ended.get()&&SystemClock.elapsedRealtime()<until);
            assertTrue("Mate and its landing must finish",ended.get());
            assertFinalBoard();
            key(KeyEvent.KEYCODE_BACK);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertFinalBoard();
            finishAndAwaitPause();launchAndStart();assertFinalBoard();
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(Session.Phase.MATH,controller.state().phase);
        });
    }

    private void assertFinalBoard() {
        instrumentation.runOnMainSync(()->{
            assertEquals(Session.Phase.CHESS,controller.state().phase);
            assertEquals("d8h4",controller.state().moves.get(3));
            View board=view("board"),result=view("game-result"),next=view("game-continue");
            assertTrue(board.isShown());assertTrue(result.isShown());assertTrue(next.isShown());
            assertTrue("Final board must remain large enough to study",board.getWidth()>=TvStyle.dp(activity,360));
            assertEquals(board.getWidth(),board.getHeight());
            assertFalse("Result must not cover any squares",Rect.intersects(bounds(board),bounds(result)));
            assertFalse("Continue must not cover any squares",Rect.intersects(bounds(board),bounds(next)));
            assertSame(next,activity.getCurrentFocus());
        });
    }

    private static final class DelayedPersistence implements SessionController.Persistence {
        volatile Session saved;
        volatile boolean delayNextSave,failNextSave;
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        DelayedPersistence(Session fixture){saved=fixture.copy();}
        public Session load(){return saved.copy();}
        public void save(Session state) throws Exception {
            if(delayNextSave){
                delayNextSave=false;
                entered.countDown();
                if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Delayed save was not released");
            }
            if(failNextSave){failNextSave=false;throw new java.io.IOException("Test storage failure");}
            saved=state.copy();
        }
    }

    private View view(String tag){
        View result=activity.findViewById(android.R.id.content).findViewWithTag(tag);
        assertNotNull(tag,result);
        return result;
    }

    private Rect bounds(View view){
        int[] point=new int[2];
        view.getLocationOnScreen(point);
        return new Rect(point[0],point[1],point[0]+view.getWidth(),point[1]+view.getHeight());
    }

    private void assertGeometry(){
        Rect board=bounds(view("board"));
        Rect screen=bounds(activity.getWindow().getDecorView());
        assertTrue("Board must be horizontally centered within one pixel",Math.abs(board.exactCenterX()-screen.exactCenterX())<=1f);
        assertEquals("Board must stay square",board.width(),board.height());
        for(String tag:new String[]{"chess-status","chess-turn","chess-prompt","chess-feedback"}){
            assertNull(tag,activity.findViewById(android.R.id.content).findViewWithTag(tag));
        }
        assertSame(view("board"),activity.getCurrentFocus());
    }

    private interface Checked { void run() throws Exception; }
    private void withFixture(Session fixture,Checked test) throws Exception {
        SessionController.Persistence memory=new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session state){saved=state.copy();}
        };
        withFixture(memory,SystemClock::elapsedRealtime,test);
    }

    private void withFixture(SessionController.Persistence memory,LongSupplier clock,Checked test) throws Exception {
        Field shared=MainActivity.class.getDeclaredField("sharedController");
        shared.setAccessible(true);
        SessionController previous=(SessionController)shared.get(null);
        controller=new SessionController(memory,clock,MAIN::post,null);
        shared.set(null,controller);
        try { test.run(); }
        finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
            controller.setListener(null);
            awaitSave();
            controller.close();
            shared.set(null,previous);
            activity=null;
        }
    }

    private void launchAndStart() throws Exception {
        awaitSave();
        Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=(MainActivity)instrumentation.startActivitySync(intent);
        AtomicBoolean focused=new AtomicBoolean();
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{
                View focus=activity.getCurrentFocus();
                String start=activity.getString(controller.state().revision==0?R.string.start:R.string.continue_game);
                focused.set(activity.hasWindowFocus()&&!controller.busy()&&focus!=null&&start.equals(focus.getTag()));
            });
            if(!focused.get())SystemClock.sleep(20);
        } while(!focused.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Saved state and Start control must be ready before remote activation",focused.get());
        key(KeyEvent.KEYCODE_DPAD_CENTER);
    }

    private void finishAndAwaitPause() throws Exception {
        MainActivity previous=activity;
        instrumentation.runOnMainSync(previous::finish);
        AtomicBoolean destroyed=new AtomicBoolean();
        long until=SystemClock.elapsedRealtime()+5000;
        do {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->destroyed.set(previous.isDestroyed()));
            if(!destroyed.get())SystemClock.sleep(20);
        } while(!destroyed.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Previous Activity must finish before relaunch",destroyed.get());
        // onPause saves asynchronously. Start is deliberately ignored while that save is active.
        awaitSave();
        assertTrue("The finished Activity must persist its pause before relaunch",controller.state().paused);
    }

    private void key(int code) throws Exception {
        instrumentation.sendKeyDownUpSync(code);
        instrumentation.waitForIdleSync();
        awaitSave();
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

}
