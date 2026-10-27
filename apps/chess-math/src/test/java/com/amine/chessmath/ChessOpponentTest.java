package com.amine.chessmath;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ChessOpponentTest {
    @Test public void exhaustedBudgetChoosesTheSameLegalFallbackOnEveryCall() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("e2e4"));
        String first=ChessOpponent.choose(game,0,()->0L);
        assertTrue(game.legalMoves().contains(first));
        for(int repeat=0;repeat<20;repeat++)
            assertEquals(first,ChessOpponent.choose(game,0,()->0L));
    }
    @Test public void takesImmediateMateRatherThanMaterial() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("f2f3","e7e5","g2g4"));
        assertEquals("d8h4",ChessOpponent.choose(game,20000,()->0L));
    }
    @Test public void movesAttackedQueenRatherThanLosingItOnTheReply() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("e2e4","d7d5","e4d5","d8d5","b1c3"));
        assertQueenSafe(game,ChessOpponent.choose(game,20000,()->0L));
    }
    @Test public void incompleteSearchFallbackDoesNotLeaveTheQueenCapturable(){
        ChessGame game=ChessGame.fromHistory(Arrays.asList("e2e4","d7d5","e4d5","d8d5","b1c3"));
        for(int nodes:new int[]{0,1,2,10,50,100})
            assertQueenSafe(game,ChessOpponent.choose(game,nodes,()->0L));
    }
    @Test public void fixedBudgetIsRepeatableAndPreservesPositionAndHistory() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("e2e4"));
        String fen=game.fen();java.util.List<String> history=game.history();
        String first=ChessOpponent.choose(game,1000,()->0L);
        assertEquals(first,ChessOpponent.choose(game,1000,()->0L));
        assertTrue(game.legalMoves().contains(first));assertEquals(fen,game.fen());assertEquals(history,game.history());
    }
    @Test public void exhaustedNodeAndTimeBudgetsReturnLegalMoves() {
        ChessGame game=new ChessGame();
        assertTrue(game.legalMoves().contains(ChessOpponent.choose(game,0,()->0L)));
        long[] time={0};
        assertTrue(game.legalMoves().contains(ChessOpponent.choose(game,20000,()->{time[0]+=1_000_000_000L;return time[0];})));
    }
    @Test public void interruptionPreservesFlagAndReturnsLegalFallback() {
        ChessGame game=new ChessGame();Thread.currentThread().interrupt();
        try { assertTrue(game.legalMoves().contains(ChessOpponent.choose(game)));assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }
    @Test public void productionBudgetFindsMateAndSavesAttackedQueen() {
        ChessGame mate=ChessGame.fromHistory(Arrays.asList("f2f3","e7e5","g2g4"));
        ChessGame queen=ChessGame.fromHistory(Arrays.asList("e2e4","d7d5","e4d5","d8d5","b1c3"));
        assertEquals("d8h4",ChessOpponent.choose(mate));
        assertQueenSafe(queen,ChessOpponent.choose(queen));
    }
    @Test public void productionBudgetReturnsLegalReplyWithoutMutatingOpening() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("e2e4"));String before=game.fen();
        assertTrue(game.legalMoves().contains(ChessOpponent.choose(game)));
        assertEquals(before,game.fen());
    }
    @Test public void finishedGameReturnsNoMove() {
        ChessGame game=ChessGame.fromHistory(Arrays.asList("f2f3","e7e5","g2g4","d8h4"));
        assertNull(ChessOpponent.choose(game));
    }
    private static void assertQueenSafe(ChessGame game,String move){
        assertTrue("Attacked queen must move",move.startsWith("d5"));
        ChessGame child=game.copy();child.play(move);
        for(String reply:child.legalMoves())assertFalse("Queen must not be lost to "+reply,
                reply.substring(2,4).equals(move.substring(2,4)));
    }

}
