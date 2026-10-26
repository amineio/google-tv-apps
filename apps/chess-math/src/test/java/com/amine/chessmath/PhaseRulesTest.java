package com.amine.chessmath;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhaseRulesTest {
    @Test public void timeAndThreeCorrectAndFinishedExerciseAreRequired() {
        assertFalse(PhaseRules.mathComplete(300000, 0, false));
        assertFalse(PhaseRules.mathComplete(299999, 3, true));
        assertFalse(PhaseRules.mathComplete(300000, 3, false));
        assertTrue(PhaseRules.mathComplete(300000, 3, true));
    }
}
