package com.amine.chessmath;

import android.animation.ValueAnimator;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Presentation-only views and memory snapshots. Never opens or changes saved progress. */
@RunWith(AndroidJUnit4.class)
public class BoardMotionTest {
    @Test public void movingPieceStartsAndEndsGentlyAndCrossesCenterHalfway() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Session before=new Session(),after=before.copy();after.moves.add("e2e4");after.revision=2;
            ChessBoardView view=new ChessBoardView(context);
            view.bind(ChessGame.fromHistory(after.moves),52,-1,"e2e4");
            if(!ValueAnimator.areAnimatorsEnabled())return;
            view.slide(PresentationEvent.committed(1,before,after),true,()->{},()->{});
            try {
                java.lang.reflect.Field field=ChessBoardView.class.getDeclaredField("animator");
                field.setAccessible(true);
                android.animation.TimeInterpolator easing=((ValueAnimator)field.get(view)).getInterpolator();
                assertEquals(0f,easing.getInterpolation(0f),.00001f);
                assertEquals(1f,easing.getInterpolation(1f),.00001f);
                assertEquals(.5f,easing.getInterpolation(.5f),.00001f);
                // Gentle endpoint acceleration avoids a visible initial or final jump.
                assertTrue(easing.getInterpolation(.1f)<.015f);
                assertTrue(easing.getInterpolation(.9f)>.985f);
                float previous=0;
                for(int i=1;i<=100;i++) {
                    float current=easing.getInterpolation(i/100f);
                    assertTrue(current>=previous&&current<=1);
                    previous=current;
                }
            } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
            finally {view.cancelMotion();}
        });
    }
    @Test public void disabledMotionLandsSynchronouslyWithoutChangingCommittedFacts(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Session before=new Session(),after=before.copy();after.moves.add("e2e4");after.revision=2;
            PresentationEvent event=PresentationEvent.committed(1,before,after);
            ChessBoardView view=new ChessBoardView(context);
            view.bind(ChessGame.fromHistory(after.moves),52,-1,"e2e4");
            int[] landed={0};view.slide(event,false,()->landed[0]++,()->fail("No active motion to cancel"));
            assertFalse(view.moving());assertEquals(1,landed[0]);assertEquals(1,after.moves.size());
            assertEquals(context.getString(R.string.square_description,"e2",view.pieceName('.')),view.getContentDescription());
        });
    }
    @Test public void detachSnapsCommittedPositionAndCancelsWithoutLandingCue(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            if(!ValueAnimator.areAnimatorsEnabled())return;
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Session before=new Session(),after=before.copy();after.moves.add("e2e4");after.revision=2;
            ChessBoardView view=new ChessBoardView(context);
            view.bind(ChessGame.fromHistory(after.moves),52,-1,"e2e4");
            int[] cancelled={0},landed={0};
            view.slide(PresentationEvent.committed(1,before,after),true,()->landed[0]++,()->cancelled[0]++);
            assertTrue(view.moving());
            assertEquals(context.getString(R.string.square_description,"e2",view.pieceName('P')),view.getContentDescription());
            view.onDetachedFromWindow();
            assertFalse(view.moving());assertEquals(0,landed[0]);assertEquals(1,cancelled[0]);
            assertEquals(context.getString(R.string.square_description,"e2",view.pieceName('.')),view.getContentDescription());
            assertEquals("e2e4",after.moves.get(0));
        });
    }
}
