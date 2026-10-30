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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Native remote navigation with memory persistence; never replaces the user's saved game. */
@RunWith(AndroidJUnit4.class)
public final class LocalChessRemoteTest {
    @org.junit.Rule public final PlayModeRule playMode=new PlayModeRule();
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;private SessionController controller;
    private ImeTransition editorTransition;
    private static final String FULL_NAME="Amber Alexandria Rowan";
    private interface Checked{void run() throws Exception;}
    private Session local(){Session s=new Session(false);s.revision=7;s.mode=Session.Mode.LOCAL;s.phase=Session.Phase.CHESS;s.whiteProfile=new PlayerProfile("fox",FULL_NAME);s.blackProfile=new PlayerProfile("owl","Robin");return s;}
    private Field field(String name) throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private void fixture(Session session,Checked body) throws Exception{
        Field shared=field("sharedController"),language=field("sharedLanguage"),effect=field("sharedEffects");
        Object prior=shared.get(null),priorLanguage=language.get(null),priorEffect=effect.get(null);
        language.set(null,new LanguagePreferences(new LanguagePreferences.Storage(){public String read(){return "en";}public boolean write(String value){return true;}}));
        effect.set(null,new EffectPreferences(new EffectPreferences.Storage(){public boolean read(String key,boolean fallback){return false;}public boolean write(String key,boolean value){return true;}}));
        controller=new SessionController(new SessionController.Persistence(){
            Session saved=session.copy();public Session load(){return saved.copy();}public void save(Session s){saved=s.copy();}
        },()->0L,new Handler(Looper.getMainLooper())::post,Runnable::run,r->{throw new AssertionError("Local mode scheduled computer work");},()->{});
        shared.set(null,controller);
        try{launch();body.run();}finally{
            if(activity!=null)finishAndAwaitDestroyed();
            await(()->!controller.busy());controller.setListener(null);controller.close();shared.set(null,prior);language.set(null,priorLanguage);effect.set(null,priorEffect);
        }
    }
    private void launch() throws Exception{
        activity=(MainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        await(()->activity.hasWindowFocus()&&activity.getCurrentFocus()!=null&&!controller.busy());
    }
    private void key(int code) throws Exception{instrumentation.sendKeyDownUpSync(code);instrumentation.waitForIdleSync();await(()->!controller.busy());}
    private void await(BooleanSupplier ready) throws Exception{await("Native UI transition",ready);}
    private void await(String stage,BooleanSupplier ready) throws Exception{
        long end=SystemClock.elapsedRealtime()+5000;AtomicBoolean yes=new AtomicBoolean();
        do{instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->yes.set(ready.getAsBoolean()));if(yes.get())return;SystemClock.sleep(20);}while(SystemClock.elapsedRealtime()<end);
        java.util.concurrent.atomic.AtomicReference<String> state=new java.util.concurrent.atomic.AtomicReference<>();
        instrumentation.runOnMainSync(()->state.set(diagnostics()+"; "+editorDiagnostics()));
        fail(stage+" timed out; "+state.get());
    }
    private void focus(String tag){focus("Remote focus",tag);}
    private void focus(String stage,String tag){
        instrumentation.runOnMainSync(()->assertEquals(stage+"; "+diagnostics(),tag,activity.getCurrentFocus()==null?null:activity.getCurrentFocus().getTag()));
    }
    private String diagnostics(){
        Session s=controller.state();
        return "mode="+s.mode+", phase="+s.phase+", paused="+s.paused+", busy="+controller.busy()+", windowFocus="+activity.hasWindowFocus()+", destroyed="+activity.isDestroyed()
                +", started="+value(MainActivity.class,activity,"started")+", menu="+value(MainActivity.class,activity,"menu")+", screen="+value(MainActivity.class,activity,"screen")
                +", activityForeground="+value(MainActivity.class,activity,"foreground")+", controllerForeground="+value(SessionController.class,controller,"foreground")+", listenerAttached="+(value(SessionController.class,controller,"listener")!=null);
    }
    private Object value(Class<?> owner,Object target,String name){
        try{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException invalid){throw new AssertionError(invalid);}
    }
    private void finishAndAwaitDestroyed() throws Exception{
        MainActivity previous=activity;
        instrumentation.runOnMainSync(previous::finish);
        await(previous::isDestroyed);await(()->!controller.busy());
    }
    private View nativeEditor(){
        if(android.os.Build.VERSION.SDK_INT<29)return null;
        for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews()){
            View editor=findNativeEditor(root);if(editor!=null)return editor;
        }
        return null;
    }
    private boolean nativeEditorAttached(){return nativeEditor()!=null;}
    private View findNativeEditor(View view){
        if(view instanceof android.widget.EditText&&view.isAttachedToWindow()&&activity.getString(R.string.local_name_hint).equals(String.valueOf(view.getContentDescription())))return view;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findNativeEditor(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
    private Boolean editorImeVisible(){
        View editor=nativeEditor();if(editor==null||android.os.Build.VERSION.SDK_INT<30)return null;
        android.view.WindowInsets insets=editor.getRootWindowInsets();
        return insets==null?null:insets.isVisible(android.view.WindowInsets.Type.ime());
    }
    private String editorDiagnostics(){
        View editor=nativeEditor();
        return "editorAttached="+(editor!=null)+", editorWindowFocus="+(editor!=null&&editor.hasWindowFocus())+", imeInsetsVisible="+editorImeVisible()+", imeAnimationCount="+(editorTransition==null?0:editorTransition.running.size());
    }
    private static final class ImeTransition extends android.view.WindowInsetsAnimation.Callback {
        final java.util.Set<android.view.WindowInsetsAnimation> running=new java.util.HashSet<>();
        ImeTransition(){super(DISPATCH_MODE_CONTINUE_ON_SUBTREE);}
        private boolean ime(android.view.WindowInsetsAnimation animation){return (animation.getTypeMask()&android.view.WindowInsets.Type.ime())!=0;}
        @Override public void onPrepare(android.view.WindowInsetsAnimation animation){if(ime(animation))running.add(animation);}
        @Override public android.view.WindowInsets onProgress(android.view.WindowInsets insets,java.util.List<android.view.WindowInsetsAnimation> animations){
            running.clear();for(android.view.WindowInsetsAnimation animation:animations)if(ime(animation))running.add(animation);return insets;
        }
        @Override public void onEnd(android.view.WindowInsetsAnimation animation){running.remove(animation);}
    }
    private View view(String tag){return activity.findViewById(android.R.id.content).findViewWithTag(tag);}
    private void goLocalSetup() throws Exception{
        key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER); // New game
        key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_CENTER); // Local opponent
        focus("portrait-fox");
    }
    private void nextParticipant() throws Exception{
        key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN); // Bottom portrait, then Edit name
        focus("edit-name");key(KeyEvent.KEYCODE_DPAD_RIGHT);focus("setup-next");key(KeyEvent.KEYCODE_DPAD_CENTER);
    }
    @Test public void chooserUsesRealDpadAndBackDiscardsEntireDraft() throws Exception{
        fixture(local(),()->{
            String id=controller.state().id;goLocalSetup();
            key(KeyEvent.KEYCODE_DPAD_RIGHT);focus("portrait-owl");key(KeyEvent.KEYCODE_DPAD_CENTER);
            nextParticipant();focus("portrait-fox");
            instrumentation.runOnMainSync(()->assertFalse(view("portrait-owl").isEnabled()));
            key(KeyEvent.KEYCODE_BACK);focus("portrait-owl");key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_BACK);
            assertEquals(id,controller.state().id);assertEquals(FULL_NAME,controller.state().whiteProfile.customName);
            focus(activity.getString(R.string.new_game));
        });
    }
    @Test public void canceledNameEditorAndReplacementKeepMatchAndRestoreFocus() throws Exception{
        fixture(local(),()->{
            String id=controller.state().id;goLocalSetup();
            key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);focus("edit-name");key(KeyEvent.KEYCODE_DPAD_CENTER);
            await("Name editor owns the input window",()->!activity.hasWindowFocus()&&nativeEditorAttached()&&nativeEditor().hasWindowFocus());
            ImeTransition ime=new ImeTransition();editorTransition=ime;
            instrumentation.runOnMainSync(()->nativeEditor().getRootView().setWindowInsetsAnimationCallback(ime));
            await("Name editor keyboard is visible and settled",()->Boolean.TRUE.equals(editorImeVisible())&&ime.running.isEmpty());
            key(KeyEvent.KEYCODE_BACK);
            await("First Back hides the keyboard or closes the editor",()->!nativeEditorAttached()||(Boolean.FALSE.equals(editorImeVisible())&&ime.running.isEmpty()));
            AtomicBoolean editorOpen=new AtomicBoolean();instrumentation.runOnMainSync(()->editorOpen.set(nativeEditorAttached()));
            if(editorOpen.get())key(KeyEvent.KEYCODE_BACK);
            await("Native editor cancellation returns to chooser",()->activity.hasWindowFocus()&&!nativeEditorAttached());focus("Canceled native name editor","edit-name");
            key(KeyEvent.KEYCODE_DPAD_RIGHT);focus("Next after native editor dismissal","setup-next");
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            await(()->"local-black".equals(value(MainActivity.class,activity,"menu")));
            focus("Black chooser after editor cancellation","portrait-owl");
            nextParticipant();focus(activity.getString(R.string.local_start));key(KeyEvent.KEYCODE_DPAD_CENTER);
            await(()->!activity.hasWindowFocus());key(KeyEvent.KEYCODE_DPAD_CENTER); // Cancel initially focused.
            await(activity::hasWindowFocus);focus(activity.getString(R.string.local_start));
            assertEquals(id,controller.state().id);assertEquals(FULL_NAME,controller.state().whiteProfile.customName);
            assertEquals("Robin",controller.state().blackProfile.customName);assertTrue(controller.state().moves.isEmpty());
        });
    }
    @Test public void heldOkCannotSelectNextSideAfterMoveCompletion() throws Exception{
        fixture(local(),()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);focus("board");
            key(KeyEvent.KEYCODE_DPAD_CENTER);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);
            long down=SystemClock.uptimeMillis();
            instrumentation.sendKeySync(new KeyEvent(down,down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
            await(()->controller.state().moves.size()==1&&controller.presentation()==null&&!controller.busy());
            // Keep OK held while moving focus onto Black's pawn, then deliver confirmation repeats.
            for(int i=0;i<3;i++)key(KeyEvent.KEYCODE_DPAD_UP);
            for(int i=1;i<4;i++)instrumentation.sendKeySync(new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,i));
            instrumentation.waitForIdleSync();assertEquals(-1,controller.state().selected);assertEquals(1,controller.state().moves.size());
            instrumentation.sendKeySync(new KeyEvent(down,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));
            key(KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(12,controller.state().selected);
        });
    }
    @Test public void blackPromotionRestoresAndAllChoicesUseDirectionalInput() throws Exception{
        for(int choice=0;choice<4;choice++){
            Session s=local();s.moves=Arrays.asList("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2","b7a8n");s.selected=54;s.promotionTo=63;s.focus=63;
            final int selected=choice;
            fixture(SessionCodec.decode(SessionCodec.encode(s)),()->{
                key(KeyEvent.KEYCODE_DPAD_CENTER);focus("promotion0");
                instrumentation.runOnMainSync(()->assertTrue(view("promotion0").getContentDescription().toString().contains("Robin")));
                for(int i=0;i<selected;i++)key(KeyEvent.KEYCODE_DPAD_RIGHT);
                focus("promotion"+selected);key(KeyEvent.KEYCODE_DPAD_CENTER);
                assertEquals("g2h1"+"qrbn".charAt(selected),controller.state().moves.get(9));focus("board");
            });
        }
    }
    @Test public void fullChosenNamesRemainInAccessibilityAndFocusSurvivesAppReturn() throws Exception{
        fixture(local(),()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);focus("Initial Continue activation","board");
            instrumentation.runOnMainSync(()->{
                assertTrue(view("board").getContentDescription().toString().contains(FULL_NAME));
                android.view.ViewGroup score=(android.view.ViewGroup)view("player-score");
                assertTrue(containsDescription(score,FULL_NAME));
            });
            key(KeyEvent.KEYCODE_DPAD_LEFT);int square=controller.state().focus;
            finishAndAwaitDestroyed();
            launch();key(KeyEvent.KEYCODE_DPAD_CENTER);focus("Continue after completed Activity destruction","board");assertEquals(square,controller.state().focus);
        });
    }
    private boolean containsDescription(android.view.ViewGroup group,String name){
        for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.getContentDescription()!=null&&child.getContentDescription().toString().contains(name))return true;}
        return false;
    }
    @Test public void restoredFinalBoardRematchReturnsToFocusableBoardAndRemovesResult() throws Exception{
        Session finished=local();finished.moves=Arrays.asList("f2f3","e7e5","g2g4","d8h4");
        finished.result=ChessGame.fromHistory(finished.moves).result();
        fixture(SessionCodec.decode(SessionCodec.encode(finished)),()->{
            key(KeyEvent.KEYCODE_DPAD_CENTER);focus("Restored result action","game-continue");
            instrumentation.runOnMainSync(()->assertNotNull(view("game-result")));
            key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(Session.Mode.LOCAL,controller.state().mode);assertTrue(controller.state().result.isEmpty());assertTrue(controller.state().moves.isEmpty());
            assertEquals(FULL_NAME,controller.state().whiteProfile.customName);assertEquals("Robin",controller.state().blackProfile.customName);
            instrumentation.runOnMainSync(()->{
                assertNull("Rematch must remove the old result footer",view("game-result"));
                assertNull("Rematch must remove the old result action",view("game-continue"));
                assertTrue("Rematch board must accept directional input",view("board").isFocusable());
            });
            focus("Restored final board rematch","board");
            key(KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(52,controller.state().selected);
            key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_CENTER);
            assertEquals(Arrays.asList("e2e4"),controller.state().moves);
        });
    }
}
