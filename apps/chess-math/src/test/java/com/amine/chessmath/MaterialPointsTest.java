package com.amine.chessmath;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MaterialPointsTest {
    private ChessGame game(String moves){return moves.isEmpty()?new ChessGame():ChessGame.fromHistory(Arrays.asList(moves.split(" ")));}
    private PresentationEvent event(String history,String move){
        Session previous=new Session();previous.moves=game(history).history();
        Session next=previous.copy();next.moves.add(move);next.revision=1;
        return PresentationEvent.committed(1,previous,next);
    }
    @Test public void initialSidesEachHaveThirtyNineAndOrdinaryMovesAndCastlingKeepThem(){
        for(String history:new String[]{"","e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 e1g1"}){
            assertEquals(39,MaterialPoints.total(game(history),true));
            assertEquals(39,MaterialPoints.total(game(history),false));
        }
    }
    @Test public void capturesReduceOnlyTheCapturedSide(){
        PresentationEvent e=event("e2e4 d7d5","e4d5");
        assertEquals(39,MaterialPoints.before(e,true));assertEquals(39,MaterialPoints.after(e,true));
        assertEquals(39,MaterialPoints.before(e,false));assertEquals(38,MaterialPoints.after(e,false));
        assertEquals(38,MaterialPoints.total(game("e2e4 d7d5 e4d5 d8d5"),true));
    }
    @Test public void enPassantRemovesOnePointFromItsActualSide(){
        PresentationEvent e=event("e2e4 a7a6 e4e5 d7d5","e5d6");
        assertEquals(39,MaterialPoints.before(e,false));assertEquals(38,MaterialPoints.after(e,false));
        assertEquals(39,MaterialPoints.after(e,true));
    }
    @Test public void everyCapturePromotionGainsOnlyTheReplacementValueAndRemovesTheRook(){
        String history="a2a4 h7h5 a4a5 h5h4 a5a6 h4h3 a6b7 h3g2";
        char[] promotions={'q','r','b','n'};int[] expected={46,42,40,40};
        for(int i=0;i<4;i++){
            PresentationEvent e=event(history,"b7a8"+promotions[i]);
            assertEquals(38,MaterialPoints.before(e,true));assertEquals(expected[i],MaterialPoints.after(e,true));
            assertEquals(38,MaterialPoints.before(e,false));assertEquals(33,MaterialPoints.after(e,false));
        }
    }
    @Test public void knightAndBishopCapturesCostThreeAndQueenCaptureCostsNine(){
        assertEquals(36,MaterialPoints.total(game("e2e4 d7d5 f1c4 d5c4"),true));
        assertEquals(36,MaterialPoints.total(game("g1f3 d7d5 f3e5 d5d4 e5f7 e8f7"),true));
        assertEquals(30,MaterialPoints.total(game("e2e4 e7e5 d1h5 b8c6 h5e5 c6e5"),true));
    }
    @Test public void ordinaryPromotionsKeepTheOpponentsScoreUnchanged(){
        String history="a2a4 b8c6 a4a5 h7h5 a5a6 h5h4 a6b7 h4h3";
        char[] promotions={'q','r','b','n'};int[] expected={47,43,41,41};
        for(int i=0;i<4;i++){
            PresentationEvent e=event(history,"b7b8"+promotions[i]);
            assertEquals(39,MaterialPoints.before(e,true));assertEquals(expected[i],MaterialPoints.after(e,true));
            assertEquals(38,MaterialPoints.before(e,false));assertEquals(38,MaterialPoints.after(e,false));
        }
    }
}
