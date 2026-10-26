package com.amine.chessmath;

final class PhaseRules {
    static boolean chessComplete(long elapsed, boolean white, int selected,
                                 boolean modal, boolean ended) {
        return elapsed >= 300000 && (ended || (white && selected < 0 && !modal));
    }
    static boolean mathComplete(long elapsed, int correct, boolean solved) {
        return elapsed >= 300000 && correct >= 3 && solved;
    }
}
