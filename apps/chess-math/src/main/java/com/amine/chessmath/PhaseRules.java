package com.amine.chessmath;

final class PhaseRules {
    static boolean mathComplete(long elapsed, int correct, boolean solved) {
        return elapsed >= 300000 && correct >= 3 && solved;
    }
}
