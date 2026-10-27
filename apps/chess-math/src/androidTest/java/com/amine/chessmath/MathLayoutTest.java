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
import android.widget.ScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Memory-only layout fixtures never overwrite the saved child's game. */
@RunWith(AndroidJUnit4.class)
public final class MathLayoutTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    private SessionController controller;

    @Test public void hintAndPartialInputKeepThePromptAnchored() throws Exception {
        Session fixture=math(new Exercise(19,24,5,Exercise.ADD));
        withFixture(fixture,()->{
            launchAndStart();
            assertFocus("pad4");
            instrumentation.runOnMainSync(()->assertFalse(view("math-reading").isFocusable()));
            key(KeyEvent.KEYCODE_DPAD_LEFT);
            assertFocus("pad3");
            key(KeyEvent.KEYCODE_DPAD_LEFT);
            assertFocus("pad3");
            key(KeyEvent.KEYCODE_DPAD_RIGHT);
            assertFocus("pad4");
            instrumentation.runOnMainSync(()->assertNull(activity.findViewById(android.R.id.content).findViewWithTag("math-hint")));
            Rect[] before=bounds();
            instrumentation.runOnMainSync(this::assertEquationGroup);
            key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_UP);
            assertFocus("hint");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(1,controller.state().hintLevel);
            assertFocus("hint");
            assertBounds(before);
            instrumentation.runOnMainSync(()->assertTextVisible("math-hint",fixture.exercise.hint(1)));
            key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus("pad1");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals("2",controller.state().input);
            assertFalse("Partial input remains editable",controller.state().solved);
            assertFocus("pad1");
            assertBounds(before);
            instrumentation.runOnMainSync(()->{
                assertEquationGroup();
                for(int i=0;i<11;i++)assertNotNull(view("pad"+i));
                assertNull(activity.findViewById(android.R.id.content).findViewWithTag("pad11"));
                assertNull(activity.findViewById(android.R.id.content).findViewWithTag("next"));
                assertFalse(view("answer").isFocusable());
                assertTextVisible("math-title",activity.getString(R.string.math_title));

            });
        });
    }

    @Test public void widestLegalOperandsAndThreeDigitAnswerFit() throws Exception {
        Session fixture=math(new Exercise(20,100,100,Exercise.SUBTRACT));
        fixture.input="100";
        withFixture(fixture,()->{
            launchAndStart();
            instrumentation.runOnMainSync(()->{
                assertEquationGroup();
                assertTextVisible("problem",fixture.exercise.expression()+" =");
                assertTextVisible("answer","100");
                TextView equation=(TextView)view("problem");
                String[] operations={Exercise.ADD,Exercise.SUBTRACT,Exercise.MULTIPLY};
                for(String operation:operations)for(int a=0;a<=100;a++)for(int b=0;b<=100;b++){
                    int result=Exercise.ADD.equals(operation)?a+b:Exercise.SUBTRACT.equals(operation)?a-b:a*b;
                    if(result<0||result>100)continue;
                    String expression=new Exercise(20,a,b,operation).expression()+" =";
                    assertTrue(expression,equation.getPaint().measureText(expression)<=equation.getWidth());
                }
            });
        });
    }

    @Test public void centeredLongEquationFitsAtLargerFontScales() throws Exception {
        Session fixture=math(new Exercise(20,100,100,Exercise.SUBTRACT));
        fixture.input="100";
        withFixture(fixture,()->{
            launchAndStart();
            Configuration previous=new Configuration(activity.getResources().getConfiguration());
            try {
                for(float scale:new float[]{1.6f,2.0f}){
                    instrumentation.runOnMainSync(()->{
                        Configuration larger=new Configuration(previous);
                        larger.fontScale=scale;
                        activity.getResources().updateConfiguration(larger,null);
                        controller.hint();
                    });
                    awaitSave();
                    instrumentation.runOnMainSync(()->{
                        assertEquationGroup();
                        assertTextVisible("problem",fixture.exercise.expression()+" =");
                        assertTextVisible("answer","100");
                        TextView equation=(TextView)view("problem");
                        assertTrue(equation.getPaint().measureText(equation.getText().toString())<=equation.getWidth());
                    });
                }
            } finally {
                instrumentation.runOnMainSync(()->activity.getResources().updateConfiguration(previous,null));
            }
        });
    }

    @Test public void remoteCanReadOverflowAtLargerAppFontScale() throws Exception {
        Session fixture=math(new Exercise(21,99,48,Exercise.SUBTRACT));
        withFixture(fixture,()->{
            launchAndStart();
            Configuration previous=new Configuration(activity.getResources().getConfiguration());
            try {
                instrumentation.runOnMainSync(()->{
                    Configuration larger=new Configuration(previous);
                    larger.fontScale=1.6f;
                    // Change only this app's resources. Never change the TV setting.
                    activity.getResources().updateConfiguration(larger,null);
                    controller.hint();
                });
                awaitSave();
                instrumentation.runOnMainSync(()->{
                    ScrollView reading=(ScrollView)view("math-reading");
                    assertFalse("Compact content fits at font scale 1.6",reading.canScrollVertically(1));
                    assertFalse("A reading pane that fits needs no remote focus",reading.isFocusable());
                    assertTextVisible("math-hint",fixture.exercise.hint(1));
                    Configuration largest=new Configuration(previous);
                    largest.fontScale=2.0f;
                    activity.getResources().updateConfiguration(largest,null);
                    // Rebind the current question at the new scale without changing its hint.
                    controller.deleteDigit();
                });
                awaitSave();
                instrumentation.runOnMainSync(()->{
                    ScrollView reading=(ScrollView)view("math-reading");
                    reading.setSmoothScrollingEnabled(false);
                    assertTrue("Long text must overflow at font scale 2.0",reading.canScrollVertically(1));
                    assertTrue(reading.isFocusable());
                });
                assertFocus("pad4");
                key(KeyEvent.KEYCODE_DPAD_LEFT);
                assertFocus("pad3");
                key(KeyEvent.KEYCODE_DPAD_LEFT);
                assertFocus("math-reading");
                key(KeyEvent.KEYCODE_DPAD_DOWN);
                instrumentation.runOnMainSync(()->assertTrue(view("math-reading").getScrollY()>0));
                for(int n=0;n<20;n++)key(KeyEvent.KEYCODE_DPAD_DOWN);
                assertFocus("math-reading");
                instrumentation.runOnMainSync(()->{
                    assertTextVisible("math-hint",fixture.exercise.hint(1));
                });
                key(KeyEvent.KEYCODE_DPAD_RIGHT);
                assertFocus("pad3");
                key(KeyEvent.KEYCODE_DPAD_UP);
                key(KeyEvent.KEYCODE_DPAD_UP);
                assertFocus("hint");
                key(KeyEvent.KEYCODE_DPAD_LEFT);
                assertFocus("math-reading");
                for(int n=0;n<20;n++)key(KeyEvent.KEYCODE_DPAD_UP);
                instrumentation.runOnMainSync(()->assertEquals(0,view("math-reading").getScrollY()));
                key(KeyEvent.KEYCODE_DPAD_RIGHT);
                assertFocus("hint");
                key(KeyEvent.KEYCODE_DPAD_DOWN);
                assertFocus("pad0");
            } finally {
                instrumentation.runOnMainSync(()->activity.getResources().updateConfiguration(previous,null));
            }
        });
    }

    private Session math(Exercise exercise){
        Session fixture=new Session();
        fixture.phase=Session.Phase.MATH;
        fixture.exercise=exercise;
        fixture.exerciseConfig=fixture.config.copy();
        return fixture;
    }

    private View view(String tag){
        View view=activity.findViewById(android.R.id.content).findViewWithTag(tag);
        assertNotNull(tag,view);
        return view;
    }

    private Rect[] bounds(){
        Rect[] result=new Rect[4];
        instrumentation.runOnMainSync(()->{
            String[] tags={"math-title","math-count","problem","answer"};
            for(int i=0;i<tags.length;i++){
                View view=view(tags[i]);
                int[] point=new int[2];
                view.getLocationOnScreen(point);
                result[i]=new Rect(point[0],point[1],point[0]+view.getWidth(),point[1]+view.getHeight());
            }
        });
        return result;
    }

    private void assertBounds(Rect[] before){
        assertArrayEquals("Hint and feedback must not move the prompt",before,bounds());
    }

    private void assertEquationGroup(){
        View problem=view("problem"),answer=view("answer"),equation=view("math-equation");
        int gap=answer.getLeft()-problem.getRight();
        int expected=Math.round(12*activity.getResources().getDisplayMetrics().density);
        assertEquals("Expression and answer need one compact gap",expected,gap);
        assertTrue("The combined expression and answer must be centered",
                Math.abs(problem.getLeft()+answer.getRight()-equation.getWidth())<=1);
        assertTrue(problem.getLeft()>=0);
        assertTrue(answer.getRight()<=equation.getWidth());
    }

    private void assertTextVisible(String tag,String expected){
        TextView text=(TextView)view(tag);
        assertEquals(expected,text.getText().toString());
        assertTrue(tag,text.isShown());
        assertNotNull(tag,text.getLayout());
        assertTrue(tag,text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        for(int line=0;line<text.getLineCount();line++)assertEquals(tag,0,text.getLayout().getEllipsisCount(line));
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
        key(KeyEvent.KEYCODE_DPAD_CENTER);
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

}
