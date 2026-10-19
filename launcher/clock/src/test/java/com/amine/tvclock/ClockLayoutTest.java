package com.amine.tvclock;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ClockLayoutTest {
    @Test public void lowerPositionKeepsTheEntireClockInsideTheScreen() throws Exception {
        assertEquals(18, offset(540, 32, 18, 0));
        assertEquals(254, offset(540, 32, 18, 10));
        assertEquals(490, offset(540, 32, 18, 20));
        assertEquals(462, offset(540, 60, 18, 20));
    }

    @Test public void positionAndSizeCannotExceedTheirLimits() throws Exception {
        assertEquals(0, ClockLayout.position(-1));
        assertEquals(20, ClockLayout.position(21));
        assertEquals(14, ClockLayout.size(2));
        assertEquals(40, ClockLayout.size(99));
        assertEquals(22, ClockLayout.size(22));
        assertEquals(18, offset(540, 32, 18, -1));
        assertEquals(490, offset(540, 32, 18, 99));
    }

    @Test public void smallWindowsDoNotProduceNegativeCoordinates() throws Exception {
        assertEquals(0, offset(32, 32, 18, 20));
        assertEquals(4, offset(40, 32, 18, 10));
    }

    private int offset(int height, int clockHeight, int inset, int position) {
        return ClockLayout.verticalOffset(height, clockHeight, inset, position);
    }
}
