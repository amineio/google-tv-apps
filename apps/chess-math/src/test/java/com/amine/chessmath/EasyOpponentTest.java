package com.amine.chessmath;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class EasyOpponentTest {
    @Test public void choicesAreLegalRepeatableAndDoNotMutateTheGame() {
        ChessGame g = ChessGame.fromHistory(Arrays.asList("e2e4"));
        String before = g.fen(); Set<String> choices = new HashSet<>();
        for (long seed = 0; seed < 100; seed++) {
            String move = EasyOpponent.choose(g, seed);
            assertTrue(g.legalMoves().contains(move));
            assertEquals(move, EasyOpponent.choose(g, seed)); choices.add(move);
        }
        assertTrue(choices.size() > 8); assertEquals(before, g.fen());
    }
    @Test public void interruptionReturnsLegalFallbackWithoutClearingInterrupt() {
        Thread.currentThread().interrupt();
        try { assertTrue(new ChessGame().legalMoves().contains(EasyOpponent.choose(new ChessGame(), 5))); assertTrue(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }
}
