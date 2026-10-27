package com.amine.chessmath;

import android.animation.ValueAnimator;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.FrameMetrics;
import android.view.Window;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Physical-TV integration with memory storage. Never opens the family's snapshot or preferences. */
@RunWith(AndroidJUnit4.class)
public final class LiveEffectsTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private static final String TAG="ChessMathEffectsTest";
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;
    private Observer observer;
    private FrameStats frameStats;
    private interface Checked {void run() throws Exception;}
    private static Object field(Object owner,String name){
        try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private void await(String message,BooleanSupplier condition) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+6000;AtomicBoolean ready=new AtomicBoolean();
        do{
            instrumentation.runOnMainSync(()->ready.set(condition.getAsBoolean()));
            if(ready.get())return;
            SystemClock.sleep(10);
        }while(SystemClock.elapsedRealtime()<deadline);
        fail(message);
    }
    private ChessBoardView board(){
        return (ChessBoardView)activity.findViewById(android.R.id.content).findViewWithTag("board");
    }
    private void key(int code){instrumentation.sendKeyDownUpSync(code);}
    private void fixture(Checked test) throws Exception {
        Field shared=MainActivity.class.getDeclaredField("sharedController");shared.setAccessible(true);
        Field effect=MainActivity.class.getDeclaredField("sharedEffects");effect.setAccessible(true);
        Object prior=shared.get(null),priorEffects=effect.get(null);
        controller=new SessionController(new SessionController.Persistence(){
            private Session saved=new Session();
            {saved.phase=Session.Phase.CHESS;}
            public Session load(){return saved.copy();}
            public void save(Session state){saved=state.copy();}
        },SystemClock::elapsedRealtime,MAIN::post,null);
        effect.set(null,new EffectPreferences(new EffectPreferences.Storage(){
            private final java.util.Map<String,Boolean> values=new java.util.HashMap<>();
            public boolean read(String key,boolean fallback){return values.getOrDefault(key,fallback);}
            public boolean write(String key,boolean value){values.put(key,value);return true;}
        }));
        shared.set(null,controller);
        try{
            activity=(MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            await("Foreground window must be ready",()->activity.hasWindowFocus()&&activity.getCurrentFocus()!=null);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            await("Start must show active board",()->!controller.busy()&&!controller.state().paused&&board()!=null);
            test.run();
        }finally{
            if(activity!=null)instrumentation.runOnMainSync(()->{
                if(frameStats!=null)frameStats.stopAndLog();
                if(observer!=null)activity.getWindow().getDecorView().getViewTreeObserver().removeOnDrawListener(observer);
                activity.finish();
            });
            await("Memory save must settle before fixture closes",()->!controller.busy());
            instrumentation.waitForIdleSync();controller.close();shared.set(null,prior);effect.set(null,priorEffects);
            activity=null;observer=null;frameStats=null;
        }
    }
    private void requireMotion(){
        AtomicBoolean enabled=new AtomicBoolean();instrumentation.runOnMainSync(()->enabled.set(ValueAnimator.areAnimatorsEnabled()));
        assumeTrue("Live slide checks require system animation enabled",enabled.get());
    }
    private void startE2E4(Runnable afterWhiteFrame) throws Exception {
        startE2E4(afterWhiteFrame,true);
    }
    private void startE2E4(Runnable afterWhiteFrame,boolean captureDuringSlide) throws Exception {
        requireMotion();
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        await("e2 selection must commit",()->!controller.busy()&&controller.state().selected==52);
        key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);
        instrumentation.runOnMainSync(()->{
            observer=new Observer(afterWhiteFrame);
            activity.getWindow().getDecorView().getViewTreeObserver().addOnDrawListener(observer);
        });
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        assertTrue("A moving White board frame must reach the renderer",observer.whiteFrame.await(4,TimeUnit.SECONDS));
        assertNull(observer.failure);
        // Screenshot capture can perturb frame timings; the submitted-frame latch stays active.
        if(captureDuringSlide&&afterWhiteFrame==null&&frameStats==null)capture("live-effects-white-slide");
    }
    private final class Observer implements ViewTreeObserver.OnDrawListener {
        final CountDownLatch whiteFrame=new CountDownLatch(1),blackFrame=new CountDownLatch(1);
        final List<PresentationEvent.Id> events=new ArrayList<>();
        final Runnable afterWhite;
        volatile String failure;
        Observer(Runnable afterWhite){this.afterWhite=afterWhite;}
        @Override public void onDraw(){
            PresentationEvent event=controller.presentation();ChessBoardView view=board();
            if(event==null||view==null||!view.moving()||events.contains(event.id))return;
            events.add(event.id);
            int size=controller.state().moves.size();
            for(int i=0;i<3;i++){
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));
            }
            if(controller.state().moves.size()!=size||controller.state().selected!=-1)
                failure="Repeated OK changed a moving position";
            if(!event.id.equals(controller.presentation().id))failure="Repeated OK replaced the presentation gate";
            Log.i(TAG,"moving ply="+event.id.subject+" move="+event.move+" savedMoves="+size);
            Runnable submitted=()->{
                if(event.beforeWhiteToMove){
                    whiteFrame.countDown();
                    if(afterWhite!=null)MAIN.post(afterWhite);
                }else blackFrame.countDown();
            };
            View decor=activity.getWindow().getDecorView();
            if(android.os.Build.VERSION.SDK_INT>=29)decor.getViewTreeObserver().registerFrameCommitCallback(submitted);
            else decor.postOnAnimation(submitted);
        }
    }
    /** Window durations sampled while the board is moving; no device-dependent pass threshold. */
    private final class FrameStats implements Window.OnFrameMetricsAvailableListener {
        private final List<Long> durations=new ArrayList<>();
        private final Window window=activity.getWindow();
        private boolean collecting;
        private int dropped;
        void start(){collecting=true;window.addOnFrameMetricsAvailableListener(this,MAIN);}
        @Override public void onFrameMetricsAvailable(Window source,FrameMetrics metrics,int droppedSinceLast){
            if(!collecting||activity==null)return;
            ChessBoardView current=board();
            if(current==null||!current.moving())return;
            long duration=metrics.getMetric(FrameMetrics.TOTAL_DURATION);
            if(duration>=0)durations.add(duration);
            dropped+=droppedSinceLast;
        }
        // Called on the same queue as callbacks. Queued callbacks discard after removal.
        void stopAndLog(){
            if(!collecting)return;
            collecting=false;window.removeOnFrameMetricsAvailableListener(this);
            java.util.Collections.sort(durations);
            int count=durations.size();
            if(count==0){Log.i(TAG,"slideWindowFrames count=0 dropped="+dropped);return;}
            double median=count%2==0?(durations.get(count/2-1)/2.0+durations.get(count/2)/2.0):durations.get(count/2);
            long p95=durations.get(Math.max(0,(int)Math.ceil(count*.95)-1));
            Log.i(TAG,String.format(java.util.Locale.ROOT,
                    "slideWindowFrames count=%d medianMs=%.3f p95Ms=%.3f maxMs=%.3f dropped=%d metric=TOTAL_DURATION filter=board.moving",
                    count,median/1_000_000.0,p95/1_000_000.0,durations.get(count-1)/1_000_000.0,dropped));
        }
    }
    @Test public void remoteMoveAndReplyEachSlideOnceAndIgnoreRepeatedOk() throws Exception {
        fixture(()->{
            instrumentation.runOnMainSync(()->{frameStats=new FrameStats();frameStats.start();});
            startE2E4(null);
            assertTrue("A moving Black reply frame must reach the renderer",observer.blackFrame.await(5,TimeUnit.SECONDS));
            await("Black must land and release White input",()->controller.state().moves.size()==2&&controller.presentation()==null
                    &&board()!=null&&!board().moving()&&!controller.busy());
            instrumentation.runOnMainSync(()->{
                assertEquals(2,observer.events.size());assertEquals(1,observer.events.get(0).subject);
                assertEquals(2,observer.events.get(1).subject);assertNull(observer.failure);
                assertEquals("e2e4",controller.state().moves.get(0));assertTrue(controller.game().whiteToMove());
                assertEquals(-1,controller.state().selected);
            });
            // Allow a further scheduler tick: no extra turn or replay may appear.
            SystemClock.sleep(350);
            instrumentation.runOnMainSync(()->{assertEquals(2,controller.state().moves.size());assertEquals(2,observer.events.size());});
            instrumentation.runOnMainSync(()->frameStats.stopAndLog());
            capture("live-effects-after-reply");
        });
    }
    @Test public void backDuringSlideCancelsAndResumePresentsOnlyOwedBlackReply() throws Exception {
        interrupted(false);
    }
    @Test public void foregroundLossDuringSlideCancelsAndResumePresentsOnlyOwedBlackReply() throws Exception {
        interrupted(true);
    }
    @Test public void actualHomeDuringSlideStopsEffectsAndSameTaskResumesOwedReply() throws Exception {
        fixture(()->{
            // Avoid screenshot latency between the first submitted moving frame and real Home.
            startE2E4(null,false);
            instrumentation.runOnMainSync(()->{
                assertNotNull(board());assertTrue("Home must start while the White slide is active",board().moving());
                assertNotNull(controller.presentation());assertEquals(1,controller.state().moves.size());
            });
            key(KeyEvent.KEYCODE_HOME);
            await("OS Home must hide and pause this Activity",()->!activity.hasWindowFocus()
                    &&controller.state().paused&&!controller.busy()&&controller.presentation()==null);
            instrumentation.runOnMainSync(()->{
                assertEquals(1,controller.state().moves.size());assertEquals(1,observer.events.size());
                assertEquals(0,((Number)field(field(activity,"player"),"stream")).intValue());
                ChessBoardView shown=board();if(shown!=null)assertFalse(shown.moving());
                Log.i(TAG,"actualHome hidden=true paused=true gate=false stream=0 savedMoves=1");
            });
            SystemClock.sleep(350);
            instrumentation.runOnMainSync(()->{
                assertFalse("Home must retain the fixture Activity for task reorder",activity.isDestroyed());
                assertEquals(1,controller.state().moves.size());
                instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NEW_TASK));
            });
            await("The same Activity must return focused on Resume",()->activity.hasWindowFocus()
                    &&activity.getCurrentFocus()!=null&&activity.getString(R.string.resume).equals(activity.getCurrentFocus().getTag()));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue("The owed Black reply must present after actual Home",observer.blackFrame.await(5,TimeUnit.SECONDS));
            await("Returned Black reply must land once",()->controller.state().moves.size()==2
                    &&controller.presentation()==null&&!controller.busy());
            SystemClock.sleep(350);
            instrumentation.runOnMainSync(()->{
                assertEquals(2,controller.state().moves.size());assertEquals(2,observer.events.size());
                assertEquals(1,observer.events.get(0).subject);assertEquals(2,observer.events.get(1).subject);
                assertTrue(controller.game().whiteToMove());assertNull(observer.failure);
                Log.i(TAG,"actualHome resumed=true savedMoves=2 presentationEvents=2 whiteReplayed=false");
            });
        });
    }
    private void interrupted(boolean loseFocus) throws Exception {
        fixture(()->{
            startE2E4(()->{if(loseFocus)activity.onWindowFocusChanged(false);else activity.onBackPressed();});
            await("Interruption must clear gate and pause",()->!controller.busy()&&controller.state().paused&&controller.presentation()==null);
            instrumentation.runOnMainSync(()->{
                assertEquals(1,controller.state().moves.size());assertEquals(1,observer.events.size());
                assertFalse(controller.game().whiteToMove());
                assertEquals(0,((Number)field(field(activity,"player"),"stream")).intValue());
                ChessBoardView shown=board();if(shown!=null)assertFalse(shown.moving());
            });
            SystemClock.sleep(350);
            instrumentation.runOnMainSync(()->{
                assertEquals(1,controller.state().moves.size());
                if(loseFocus)activity.onWindowFocusChanged(true);
            });
            await("Pause Resume action must be available",()->activity.getCurrentFocus()!=null);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertTrue("Resume must present the owed Black reply",observer.blackFrame.await(5,TimeUnit.SECONDS));
            await("Resume reply must land once",()->controller.state().moves.size()==2&&controller.presentation()==null&&!controller.busy());
            instrumentation.runOnMainSync(()->{
                assertEquals(2,observer.events.size());assertEquals(2,observer.events.get(1).subject);
                assertTrue(controller.game().whiteToMove());assertNull(observer.failure);
            });
        });
    }
    @Test public void allFourSoundsDecodePlayStopAndReleaseOnForegroundTv() throws Exception {
        fixture(()->{
            final EffectPlayer[] tested={null};
            instrumentation.runOnMainSync(()->tested[0]=new EffectPlayer(activity));
            try{
                await("All four bundled PCM resources must decode",()->((Set<?>)field(tested[0],"ready")).size()==4);
                for(PresentationEvent.Cue cue:PresentationEvent.Cue.values()){
                    instrumentation.runOnMainSync(()->{
                        assertTrue(activity.hasWindowFocus());tested[0].play(cue);
                        int stream=((Number)field(tested[0],"stream")).intValue();
                        boolean focus=field(tested[0],"focus")!=null;
                        Log.i(TAG,"cue="+cue+" decoded=4 stream="+stream+" focus="+focus);
                        assertTrue("SoundPool must start "+cue+" with granted transient focus",stream>0);
                        assertTrue(focus);
                    });
                    SystemClock.sleep(cue==PresentationEvent.Cue.MOVE?120:cue==PresentationEvent.Cue.CAPTURE?160:280);
                    instrumentation.runOnMainSync(()->{
                        tested[0].stop();assertEquals(0,((Number)field(tested[0],"stream")).intValue());assertNull(field(tested[0],"focus"));
                    });
                }
                instrumentation.runOnMainSync(()->{
                    tested[0].play(PresentationEvent.Cue.CHECK);tested[0].stop();
                    assertEquals(0,((Number)field(tested[0],"stream")).intValue());assertNull(field(tested[0],"focus"));
                    tested[0].close();assertEquals(Boolean.TRUE,field(tested[0],"closed"));
                    assertTrue(((Set<?>)field(tested[0],"ready")).isEmpty());
                    tested[0].play(PresentationEvent.Cue.MOVE);assertEquals(0,((Number)field(tested[0],"stream")).intValue());
                });
            }finally{instrumentation.runOnMainSync(()->tested[0].close());}
        });
    }
    private void capture(String name) throws Exception {
        Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(image);
        File directory=new File(instrumentation.getTargetContext().getExternalFilesDir(null),"evidence");
        assertTrue(directory.isDirectory()||directory.mkdirs());
        try(FileOutputStream output=new FileOutputStream(new File(directory,name+".png"))){
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));
        }finally{image.recycle();}
    }
}
