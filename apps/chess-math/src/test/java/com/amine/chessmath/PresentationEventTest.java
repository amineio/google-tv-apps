package com.amine.chessmath;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class PresentationEventTest {
    private PresentationEvent event(String history,String move) {
        Session before=new Session();
        if(!history.isEmpty())before.moves.addAll(Arrays.asList(history.split(" ")));
        Session after=before.copy();after.moves.add(move);after.revision=7;
        return PresentationEvent.committed(2,before,after);
    }
    @Test public void enPassantRetainsCapturedPawnOnItsActualSquare() {
        PresentationEvent e=event("e2e4 a7a6 e4e5 d7d5","e5d6");
        assertEquals(27,e.capturedSquare);assertEquals('p',e.capturedPiece);
        assertEquals('.',e.beforePiece(e.to));assertEquals(PresentationEvent.Cue.CAPTURE,e.cue);
    }
    @Test public void castleSlidesBothPiecesForOneDuration() {
        PresentationEvent e=event("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6","e1g1");
        assertEquals(63,e.rookFrom);assertEquals(61,e.rookTo);assertEquals(620,e.durationMs);
        assertEquals('K',e.movingPiece);assertEquals(PresentationEvent.Cue.MOVE,e.cue);
    }
    @Test public void shortAndLongMovesAllowTimeForGentleAccelerationAndDeceleration() {
        assertEquals(480,event("","e2e4").durationMs);
        assertEquals(560,event("e2e4 e7e5","f1c4").durationMs);
    }
    @Test public void allCapturePromotionsTravelAsPawnAndLandAsChosenPiece() {
        for(char p:new char[]{'q','r','b','n'}) {
            PresentationEvent e=event("a2a4 h7h5 a4a5 h5h4 a5a6 h4h3 a6b7 h3g2","b7a8"+p);
            assertEquals('P',e.movingPiece);assertEquals(Character.toUpperCase(p),e.promotionPiece);
            assertEquals('r',e.capturedPiece);assertEquals(0,e.capturedSquare);
        }
    }
    @Test public void nonCaptureCheckKeepsItsCueAndIdentitiesDeduplicate() {
        PresentationEvent e=event("e2e4 f7f6","d1h5");
        assertEquals(PresentationEvent.Cue.CHECK,e.cue);
        PresentationEvent.Consumption consumed=new PresentationEvent.Consumption();
        assertTrue(consumed.take(e));assertFalse(consumed.take(e));
        assertFalse(consumed.take(event("","d2d4")));
    }
    @Test public void checkingCaptureStillPlaysTheCaptureCue() {
        PresentationEvent e=event("e2e4 d7d5 f1b5 c7c6","b5c6");
        assertEquals('p',e.capturedPiece);
        assertTrue(e.afterCheck>=0);
        assertEquals(PresentationEvent.Cue.CAPTURE,e.cue);
    }
    @Test public void selectionAndRestoredSolvedSnapshotsAreSilent() {
        Session before=new Session(), after=before.copy();after.selected=52;after.revision=1;
        assertNull(PresentationEvent.committed(1,before,after));
        before.phase=Session.Phase.MATH;before.exercise=new Exercise(1,7,0,Exercise.ADD);before.solved=true;
        after=before.copy();after.paused=false;
        assertNull(PresentationEvent.committed(1,before,after));
    }
}
