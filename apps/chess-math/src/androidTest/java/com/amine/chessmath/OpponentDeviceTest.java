package com.amine.chessmath;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercise the production search budgets on the device, away from the UI thread. */
@RunWith(AndroidJUnit4.class)
public final class OpponentDeviceTest {
    @Test public void strongestSearchFindsTacticsWithinDeviceBudget(){
        assertNotSame("Search must run outside the UI thread",Looper.getMainLooper(),Looper.myLooper());
        ChessGame mate=ChessGame.fromHistory(Arrays.asList("f2f3","e7e5","g2g4"));
        assertEquals("d8h4",choose(mate,"mate"));
        ChessGame queen=ChessGame.fromHistory(Arrays.asList("e2e4","d7d5","e4d5","d8d5","b1c3"));
        String queenMove=choose(queen,"attacked queen");
        assertTrue("Attacked queen must move",queenMove.startsWith("d5"));
        ChessGame child=queen.copy();child.play(queenMove);
        for(String reply:child.legalMoves())assertFalse("Queen must not be lost to "+reply,
                reply.substring(2,4).equals(queenMove.substring(2,4)));
        ChessGame opening=ChessGame.fromHistory(Arrays.asList("e2e4"));
        String before=opening.fen();
        assertTrue(opening.legalMoves().contains(choose(opening,"opening")));
        assertEquals("Search must preserve the input position",before,opening.fen());
        assertEquals(Arrays.asList("e2e4"),opening.history());
    }
    private String choose(ChessGame game,String fixture){
        long start=SystemClock.elapsedRealtime();
        String move=ChessOpponent.choose(game);
        long elapsed=SystemClock.elapsedRealtime()-start;
        Log.i("OpponentDeviceTest",fixture+": "+move+" in "+elapsed+" ms");
        assertTrue("Production search must return within five seconds; elapsed="+elapsed,elapsed<=5000);
        assertTrue("Production search must return a legal move",game.legalMoves().contains(move));
        return move;
    }
}
