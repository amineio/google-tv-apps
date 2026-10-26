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
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** These layout fixtures use memory persistence and preserve the real saved game. */
@RunWith(AndroidJUnit4.class)
public final class BoardLayoutTest {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;

    @Test public void boardIsAtTheTvCenterWithCompactStatus() throws Exception {
        Session fresh=new Session();fresh.phase=Session.Phase.CHESS;
        withFixture(fresh,()->{
            launchAndStart();
            instrumentation.runOnMainSync(()->{
                assertGeometry();
                assertEquals(View.GONE,view("chess-feedback").getVisibility());
                assertNotNull(view("board").getContentDescription());
            });
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(52,controller.state().selected);
            instrumentation.runOnMainSync(()->{
                assertGeometry();
                assertEquals(activity.getString(R.string.choose_target),((TextView)view("chess-prompt")).getText().toString());
            });
            // A new Activity must bind the same in-memory session and geometry.
            finishAndAwaitPause();
            launchAndStart();
            assertEquals(52,controller.state().selected);
            instrumentation.runOnMainSync(this::assertGeometry);
        });
    }

    @Test public void largerFontsWrapBesideTheBoardWithoutOverlap() throws Exception {
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
                        assertTextFits("chess-turn");
                        assertTextFits("chess-prompt");
                        assertTextFits("chess-feedback");
                    });
                }
            } finally {
                instrumentation.runOnMainSync(()->activity.getResources().updateConfiguration(previous,null));
            }
        });
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
        Rect status=bounds(view("chess-status"));
        assertTrue("Board must be horizontally centered within one pixel",Math.abs(board.exactCenterX()-screen.exactCenterX())<=1f);
        assertEquals("Board must stay square",board.width(),board.height());
        assertTrue("Status must remain beyond the board",status.left>=board.right);
        assertTrue("Status must remain on screen",status.right<=screen.right);
        assertFalse("Status must not overlap the board",Rect.intersects(board,status));
        assertSame(view("board"),activity.getCurrentFocus());
    }

    private void assertTextFits(String tag){
        TextView text=(TextView)view(tag);
        assertNotNull(text.getLayout());
        assertTrue(tag,text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        for(int line=0;line<text.getLineCount();line++){
            assertEquals(tag,0,text.getLayout().getEllipsisCount(line));
            assertTrue(tag,text.getLayout().getLineWidth(line)<=text.getWidth());
        }
        Rect visible=new Rect();
        assertTrue(tag,text.getGlobalVisibleRect(visible));
        assertEquals(tag,text.getHeight(),visible.height());
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
