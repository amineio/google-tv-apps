package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Memory-backed sessions and a controlled clock exercise actual remote input. */
@RunWith(AndroidJUnit4.class)
public final class PointsThinkingRemoteTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final AtomicLong now=new AtomicLong();
    private MainActivity activity;
    private SessionController controller;
    private PlayPreferences preferences;
    private boolean motionEnabled;

    @Test public void pointsAndThinkingBlockOkButKeepDirectionalFocusUntilExpiry() throws Exception {
        withFixture(chessSession(),"en",()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertText("player-points","39");assertText("computer-points","39");
            View original=view("board");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_CENTER);
            advance(100);
            await(()->controller.state().moves.size()==2&&controller.presentation()==null&&!controller.busy());
            assertEquals(20000,controller.thinkingRemainingMs());
            capture("thinking-english");
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_UP);
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(-1,controller.state().selected);assertEquals(2,controller.state().moves.size());
            advance(19999);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(1,controller.thinkingRemainingMs());assertEquals(-1,controller.state().selected);
            advance(1);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(36,controller.state().selected);
            instrumentation.runOnMainSync(()->{
                assertSame(original,view("board"));assertSame(original,activity.getCurrentFocus());
                assertGeometry();
            });
        });
    }

    @Test public void settingsDialogUsesRemoteAndOffClearsPausedDelay() throws Exception {
        Session fixture=chessSession();fixture.moves=Arrays.asList("e2e4","e7e5");
        fixture.thinkingRemainingMs=20000;fixture.thinkingTotalMs=20000;
        withFixture(fixture,"en",()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);advance(5000);
            key(KeyEvent.KEYCODE_BACK);assertEquals(15000,controller.thinkingRemainingMs());
            advance(60000);assertEquals(15000,controller.thinkingRemainingMs());
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER);
            focusDown("thinking-time");capture("thinking-settings");
            openThinkingDialog();capture("thinking-choices");
            key(KeyEvent.KEYCODE_BACK);await(()->activity.hasWindowFocus());assertFocus("thinking-time");
            openThinkingDialog();
            key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);
            capture("thinking-before-off");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            await(()->activity.hasWindowFocus());
            assertEquals(0,preferences.thinkingSeconds());assertEquals(0,controller.thinkingRemainingMs());
            assertFocus("thinking-time");
            for(int i=0;i<20;i++)key(KeyEvent.KEYCODE_DPAD_DOWN);
            assertFocus(activity.getString(R.string.exit));
        });
    }

    @Test public void restoredDutchScoresStayOutsideBoardWithoutReplayedFeedback() throws Exception {
        Session fixture=chessSession();fixture.moves=Arrays.asList("e2e4","d7d5","e4d5","d8d5");
        fixture.thinkingRemainingMs=20000;fixture.thinkingTotalMs=20000;
        withFixture(fixture,"nl",()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertText("player-points","38");assertText("computer-points","38");
            assertInvisible("player-delta");assertInvisible("computer-delta");
            capture("thinking-dutch");
            instrumentation.runOnMainSync(this::assertGeometry);
            key(KeyEvent.KEYCODE_HOME);await(()->controller.state().paused);advance(60000);
            assertEquals(20000,controller.thinkingRemainingMs());
            instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();awaitSave();
            launch();key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertInvisible("player-delta");assertInvisible("computer-delta");
            assertEquals(20000,controller.thinkingRemainingMs());
            instrumentation.runOnMainSync(()->assertSame(view("board"),activity.getCurrentFocus()));
        });
    }

    @Test public void largerSystemFontsKeepScoresAndCountdownInsideTheirGutters() throws Exception {
        String previous=shell("settings get system font_scale").trim();
        try {
            shell("settings put system font_scale 2.0");
            await(()->instrumentation.getTargetContext().getResources().getConfiguration().fontScale>=1.99f);
            SystemClock.sleep(600);
            Session fixture=chessSession();fixture.moves=Arrays.asList("e2e4","e7e5");
            fixture.thinkingRemainingMs=20000;fixture.thinkingTotalMs=20000;
            withFixture(fixture,"nl",()->{
                key(KeyEvent.KEYCODE_DPAD_CENTER);await(()->activity.hasWindowFocus()&&!controller.state().paused);
                instrumentation.runOnMainSync(()->{
                    TextView status=(TextView)view("thinking-status");
                    float expected=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,16,activity.getResources().getDisplayMetrics());
                    assertEquals("The countdown must follow Android font scaling",expected,status.getTextSize(),0.1f);
                    assertTrue("The countdown must use enlarged text: "+status.getTextSize(),status.getTextSize()>TvStyle.dp(activity,16)*1.2f);
                    assertGeometry();
                });
                capture("thinking-large-text");
            });
        } finally {
            shell("settings put system font_scale "+(previous.equals("null")?"1.0":previous));
            float restored=previous.equals("null")?1f:Float.parseFloat(previous);
            await(()->Math.abs(instrumentation.getTargetContext().getResources().getConfiguration().fontScale-restored)<0.01f);
        }
    }

    @Test public void captureShowsOneSignedChangeAndPauseCancelsFeedback() throws Exception {
        Session fixture=chessSession();fixture.moves=Arrays.asList("e2e4","d7d5");fixture.focus=36;
        withFixture(fixture,"en",()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_CENTER);
            key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_LEFT);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertText("computer-points","38");
            instrumentation.runOnMainSync(()->{
                TextView delta=(TextView)view("computer-delta");
                assertEquals(View.VISIBLE,delta.getVisibility());assertTrue(delta.getText().toString().contains("−1"));
            });
            capture("capture-points");
            key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertInvisible("computer-delta");
        });
    }

    @Test public void promotionShowsGreenRiseAndCoralFallAtLanding() throws Exception {
        motionEnabled=true;
        Session fixture=chessSession();
        fixture.moves=Arrays.asList("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2");
        fixture.selected=9;fixture.promotionTo=0;fixture.focus=0;
        withFixture(fixture,"en",()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_CENTER);
            await(()->controller.state().moves.size()==9&&controller.presentation()==null);
            assertText("player-points","46");assertText("computer-points","33");
            assertText("player-delta","+8 ↑");assertText("computer-delta","−5 ↓");
            await(()->view("player-delta").getTranslationY()<0&&view("computer-delta").getTranslationY()>0);
            capture("promotion-points-motion");
        });
    }

    private Session chessSession(){Session s=new Session();s.phase=Session.Phase.CHESS;s.revision=7;return s;}
    private interface Checked {void run() throws Exception;}
    private void withFixture(Session fixture,String locale,Checked test) throws Exception {
        Field shared=field("sharedController"),language=field("sharedLanguage"),effects=field("sharedEffects"),play=field("sharedPlay");
        Object previous=shared.get(null),oldLanguage=language.get(null),oldEffects=effects.get(null),oldPlay=play.get(null);
        now.set(0);
        controller=new SessionController(new SessionController.Persistence(){
            private Session saved=fixture.copy();
            public Session load(){return saved.copy();}
            public void save(Session value){saved=value.copy();}
        },now::get,new Handler(Looper.getMainLooper())::post,null);
        preferences=new PlayPreferences(new PlayPreferences.Storage(){
            private final Map<String,Integer> values=new HashMap<>();
            public boolean read(String key,boolean fallback){return fallback;}
            public boolean write(String key,boolean value){return true;}
            public int readInt(String key,int fallback){return values.getOrDefault(key,fallback);}
            public boolean writeInt(String key,int value){values.put(key,value);return true;}
        });
        shared.set(null,controller);play.set(null,preferences);
        language.set(null,new LanguagePreferences(new LanguagePreferences.Storage(){
            public String read(){return locale;}public boolean write(String value){return true;}
        }));
        effects.set(null,new EffectPreferences(new EffectPreferences.Storage(){
            public boolean read(String key,boolean fallback){return motionEnabled&&key.equals("motion");}public boolean write(String key,boolean value){return true;}
        }));
        try {launch();test.run();}
        finally {
            if(activity!=null){instrumentation.runOnMainSync(activity::finish);await(()->activity.isDestroyed());}
            awaitSave();controller.setListener(null);controller.close();
            shared.set(null,previous);language.set(null,oldLanguage);effects.set(null,oldEffects);play.set(null,oldPlay);
        }
    }
    private Field field(String name) throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private void launch() throws Exception {
        activity=(MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        await(()->activity.hasWindowFocus()&&activity.getCurrentFocus()!=null&&!controller.busy());
    }
    private void advance(long elapsed) throws Exception {
        instrumentation.runOnMainSync(()->{now.addAndGet(elapsed);controller.tick();});awaitSave();
        // The existing Activity timer refreshes its views every 250 ms.
        SystemClock.sleep(280);instrumentation.waitForIdleSync();
    }
    private void openThinkingDialog() throws Exception {
        key(KeyEvent.KEYCODE_DPAD_CENTER);
        await(()->{
            android.view.accessibility.AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            return root!=null&&!root.findAccessibilityNodeInfosByText("Off").isEmpty()&&!activity.hasWindowFocus();
        });
    }
    private String shell(String command) throws Exception {
        try(android.os.ParcelFileDescriptor output=instrumentation.getUiAutomation().executeShellCommand(command);
            java.io.InputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(output)){
            return new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    private void key(int code) throws Exception {instrumentation.sendKeyDownUpSync(code);instrumentation.waitForIdleSync();awaitSave();}
    private void awaitSave() throws Exception {await(()->!controller.busy());}
    private void await(BooleanSupplier condition) throws Exception {
        AtomicBoolean ready=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+8000;
        do {
            instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->ready.set(condition.getAsBoolean()));
            if(!ready.get())SystemClock.sleep(20);
        }while(!ready.get()&&SystemClock.elapsedRealtime()<until);
        assertTrue("Fixture must become ready",ready.get());
    }
    private View view(String tag){View v=activity.findViewById(android.R.id.content).findViewWithTag(tag);assertNotNull(tag,v);return v;}
    private void assertText(String tag,String value){instrumentation.runOnMainSync(()->assertEquals(value,((TextView)view(tag)).getText().toString()));}
    private void assertInvisible(String tag){instrumentation.runOnMainSync(()->assertNotEquals(View.VISIBLE,view(tag).getVisibility()));}
    private void assertFocus(String tag){instrumentation.runOnMainSync(()->assertEquals(tag,activity.getCurrentFocus().getTag()));}
    private void focusDown(String tag) throws Exception {
        for(int i=0;i<25;i++){
            AtomicBoolean found=new AtomicBoolean();instrumentation.runOnMainSync(()->found.set(tag.equals(activity.getCurrentFocus().getTag())));
            if(found.get())return;key(KeyEvent.KEYCODE_DPAD_DOWN);
        }fail("Remote focus did not reach "+tag);
    }
    private Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    private void assertGeometry(){
        Rect board=bounds(view("board")),screen=bounds(activity.getWindow().getDecorView());
        assertTrue(Math.abs(board.exactCenterX()-screen.exactCenterX())<=1);assertEquals(board.width(),board.height());
        for(String tag:new String[]{"player-score","computer-score"}){
            View score=view(tag);Rect r=bounds(score);assertFalse(Rect.intersects(board,r));
            assertTrue(screen.contains(r));assertFalse(score.isFocusable());
        }
        TextView points=(TextView)view("player-points");
        assertTrue(points.getLayout().getLineCount()==1);
        TextView status=(TextView)view("thinking-status");
        assertTrue(status.getHeight()>=status.getLayout().getHeight());
    }
    private void capture(String name) throws Exception {
        instrumentation.waitForIdleSync();SystemClock.sleep(200);
        Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File directory=new File(instrumentation.getTargetContext().getExternalFilesDir(null),"points-thinking");
        assertTrue(directory.isDirectory()||directory.mkdirs());
        try(FileOutputStream output=new FileOutputStream(new File(directory,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}
        finally {bitmap.recycle();}
    }
}
