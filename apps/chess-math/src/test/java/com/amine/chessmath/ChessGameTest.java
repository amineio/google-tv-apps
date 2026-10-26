package com.amine.chessmath;

import org.junit.Test;
import java.util.*;
import java.lang.reflect.Field;
import com.github.bhlangonijr.chesslib.Board;
import static org.junit.Assert.*;

public class ChessGameTest {
    static ChessGame position(String fen) throws Exception {
        ChessGame g = new ChessGame();
        Field f = ChessGame.class.getDeclaredField("board");
        f.setAccessible(true);
        ((Board) f.get(g)).loadFromFen(fen);
        return g;
    }
    @Test public void legalGenerationRejectsGeometryAndKeepsState() {
        ChessGame g = new ChessGame();
        assertEquals(20, g.legalMoves().size());
        assertEquals('r', g.pieceAt(0)); assertEquals('K', g.pieceAt(60));
        String fen = g.fen();
        for (String move : Arrays.asList("e2e5", "a1a4", "e2e4q", "E2E4", "e2e4 ")) {
            assertThrows(IllegalArgumentException.class, () -> g.play(move));
            assertEquals(fen, g.fen());
        }
    }
    @Test public void standardPerftDepthThree() {
        assertEquals(8902, perft(new ChessGame(), 3));
    }
    private long perft(ChessGame game, int depth) {
        if (depth == 0) return 1;
        long nodes = 0;
        for (String move : game.legalMoves()) {
            ChessGame child = ChessGame.fromHistory(game.history()); child.play(move);
            nodes += perft(child, depth - 1);
        }
        return nodes;
    }
    @Test public void replayValidatesEveryMoveAndReturnsImmutableHistory() {
        ChessGame g = ChessGame.fromHistory(Arrays.asList("e2e4", "e7e5", "g1f3"));
        assertFalse(g.whiteToMove()); assertEquals('N', g.pieceAt(45));
        assertEquals(g.fen(), ChessGame.fromHistory(g.history()).fen());
        assertThrows(UnsupportedOperationException.class, () -> g.history().add("a7a6"));
        assertThrows(IllegalArgumentException.class, () -> ChessGame.fromHistory(Arrays.asList("e2e5")));
    }
    @Test public void pinsCastlingEnPassantAndPromotions() throws Exception {
        ChessGame pinned = position("4r1k1/8/8/8/8/8/4R3/4K3 w - - 0 1");
        assertFalse(pinned.legalMoves().contains("e2d2"));
        ChessGame castle = position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertTrue(castle.legalMoves().containsAll(Arrays.asList("e1g1", "e1c1")));
        castle.play("e1g1"); assertEquals('R', castle.pieceAt(61));
        ChessGame attacked = position("r3k2r/8/8/8/2b5/8/8/R3K2R w KQkq - 0 1");
        assertFalse(attacked.legalMoves().contains("e1g1"));
        ChessGame ep = ChessGame.fromHistory(Arrays.asList("e2e4", "a7a6", "e4e5", "d7d5"));
        ep.play("e5d6"); assertEquals('.', ep.pieceAt(27)); assertEquals('P', ep.pieceAt(19));
        ChessGame pinnedEp = position("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        assertFalse(pinnedEp.legalMoves().contains("e5d6"));
        for (String suffix : Arrays.asList("q", "r", "b", "n")) {
            ChessGame promotion = position("7k/P7/8/8/8/8/8/7K w - - 0 1");
            assertThrows(IllegalArgumentException.class, () -> promotion.play("a7a8"));
            promotion.play("a7a8" + suffix);
            assertEquals(Character.toUpperCase(suffix.charAt(0)), promotion.pieceAt(0));
        }
    }
    @Test public void repetitionIsClaimableAtThreeAndAutomaticAtFive() {
        ChessGame g = new ChessGame();
        List<String> cycle = Arrays.asList("g1f3", "g8f6", "f3g1", "f6g8");
        for (int i = 0; i < 2; i++) for (String move : cycle) g.play(move);
        assertTrue(g.claimableDraw()); assertEquals("", g.result());
        g = ChessGame.fromHistory(g.history()); assertTrue(g.claimableDraw());
        for (int i = 0; i < 2; i++) for (String move : cycle) g.play(move);
        assertFalse(g.result().isEmpty()); assertTrue(g.legalMoves().isEmpty());
    }
    @Test public void intendedClaimsArePureAndFiftyIsNotAutomatic() throws Exception {
        ChessGame g = position("7k/8/8/8/8/8/R7/K7 w - - 99 50");
        String before = g.fen();
        assertFalse(g.claimableDraw()); assertTrue(g.claimableAfter("a2b2"));
        assertFalse(g.claimableAfter("a2b3")); assertEquals(before, g.fen());
        g.play("a2b2"); assertTrue(g.claimableDraw()); assertEquals("", g.result());
        ChessGame auto = position("7k/8/8/8/8/8/R7/K7 w - - 149 75");
        auto.play("a2b2"); assertFalse(auto.result().isEmpty());
    }
    @Test public void matePrecedesSeventyFiveAndReportsCheckedKing() throws Exception {
        ChessGame g = position("7k/8/5KQ1/8/8/8/8/8 w - - 149 75");
        g.play("g6g7"); assertTrue(g.result().contains("Wit")); assertEquals(7, g.checkSquare());
        assertNull(EasyOpponent.choose(g, 1));
        ChessGame stalemate = position("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
        assertTrue(stalemate.result().contains("pat")); assertEquals(-1, stalemate.checkSquare());
    }
    @Test public void materialDrawsNeverMisclassifyPossibleHelpMates() throws Exception {
        assertFalse(position("7k/8/8/8/8/8/8/KN6 w - - 0 1").result().isEmpty());
        assertFalse(position("7k/8/8/8/8/8/8/KB6 w - - 0 1").result().isEmpty());
        assertFalse(position("7k/8/8/8/8/4b3/8/K1B5 w - - 0 1").result().isEmpty());
        assertEquals("", position("6nk/8/8/8/8/8/8/KN6 w - - 0 1").result());
        assertEquals("", position("6bk/8/8/8/8/8/8/KN6 w - - 0 1").result());
        assertEquals("", position("7k/8/8/8/8/8/8/KNN5 w - - 0 1").result());
    }
}
