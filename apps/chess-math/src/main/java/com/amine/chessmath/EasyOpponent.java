package com.amine.chessmath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deliberately weak one-ply opponent. No rating or age-suitability claim is made. */
public final class EasyOpponent {
    private EasyOpponent() { }

    public static String choose(ChessGame game, long seed) {
        long start = System.nanoTime();
        ChessGame snapshot = game.copy();
        List<String> legal = snapshot.legalMoves();
        if (legal.isEmpty()) return null;
        Random random = new Random(seed);
        String fallback = legal.get(random.nextInt(legal.size()));
        if (random.nextInt(10) < 7) return fallback;
        int sign = snapshot.whiteToMove() ? 1 : -1;
        int best = Integer.MIN_VALUE;
        List<Integer> scores = new ArrayList<>();
        for (int i = 0; i < legal.size(); i++) {
            if (i >= 256 || Thread.currentThread().isInterrupted() || System.nanoTime() - start >= 100_000_000L) return fallback;
            ChessGame child = snapshot.copy();
            child.play(legal.get(i));
            int score = material(child) * sign;
            if (child.result().contains("wint")) score = 100000;
            scores.add(score); best = Math.max(best, score);
        }
        List<String> candidates = new ArrayList<>();
        for (int i = 0; i < legal.size(); i++) if (scores.get(i) >= best - 100) candidates.add(legal.get(i));
        if (System.nanoTime() - start >= 100_000_000L) return fallback;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static int material(ChessGame game) {
        int score = 0;
        for (int i = 0; i < 64; i++) {
            char p = game.pieceAt(i);
            int value;
            switch (Character.toLowerCase(p)) {
                case 'p': value = 100; break;
                case 'n': value = 300; break;
                case 'b': value = 300; break;
                case 'r': value = 500; break;
                case 'q': value = 900; break;
                default: value = 0;
            }
            score += Character.isUpperCase(p) ? value : -value;
        }
        return score;
    }
}
