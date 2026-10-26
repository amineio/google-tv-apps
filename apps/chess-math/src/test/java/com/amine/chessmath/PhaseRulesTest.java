package com.amine.chessmath;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhaseRulesTest {
    @Test public void fiveMinutesMustReachStableWhiteTurn() {
        assertFalse(PhaseRules.chessComplete(299999, true, -1, false, false));
        assertFalse(PhaseRules.chessComplete(300000, false, -1, false, false));
        assertFalse(PhaseRules.chessComplete(300000, true, 52, false, false));
        assertFalse(PhaseRules.chessComplete(300000, true, -1, true, false));
        assertTrue(PhaseRules.chessComplete(300000, true, -1, false, false));
        assertTrue(PhaseRules.chessComplete(300000, false, 52, true, true));
    }
    @Test public void timeAndThreeCorrectAndFinishedExerciseAreRequired() {
        assertFalse(PhaseRules.mathComplete(300000, 0, false));
        assertFalse(PhaseRules.mathComplete(299999, 3, true));
        assertFalse(PhaseRules.mathComplete(300000, 3, false));
        assertTrue(PhaseRules.mathComplete(300000, 3, true));
    }
}
