package com.amine.tvclock;

import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.Assert.*;

public class TimeLimitsTest {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static long wall(String value) { return Instant.parse(value).toEpochMilli(); }

    @Test public void timerUsesElapsedTimeAndWarnsOnlyOnce() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 600_000L, 1_000_000L, 10_000L, 1);
        assertEquals(0, h.kind(1_200_000L, 309_999L));
        assertEquals(1, h.kind(9_000_000L, 310_000L));
        assertEquals(0, h.kind(9_000_001L, 310_001L));
        assertEquals(2, h.kind(9_300_000L, 610_000L));
        assertEquals(0, h.kind(9_300_001L, 610_001L));
    }

    @Test public void extensionRequiresAnActiveStopAndCanOnlyBeUsedOnce() throws Exception {
        Harness h = new Harness();
        assertEquals(false, h.call("extend", 1_000L, 1_000L, 1));
        h.call("startTimer", 1_000L, 1_000_000L, 1_000L, 1);
        assertEquals(2, h.kind(1_001_000L, 2_000L));
        assertEquals(true, h.call("extend", 1_001_000L, 2_000L, 1));
        assertEquals(0, h.kind(1_300_999L, 301_999L));
        assertEquals(2, h.kind(1_301_000L, 302_000L));
        assertEquals(false, h.call("extend", 1_301_000L, 302_000L, 1));
    }

    @Test public void bedtimeWinsTiesAndDoesNotMoveWhenTimerIsExtended() throws Exception {
        Harness h = new Harness();
        long start = wall("2026-10-19T22:20:00Z");
        h.call("setBedtime", true, 22 * 60 + 30, start, UTC);
        h.call("startTimer", 300_000L, start, 1_000L, 1);
        assertEquals(2, h.kind(start + 300_000, 301_000));
        assertEquals(false, h.call("extend", start + 300_000, 301_000L, 1));
        h.call("startTimer", 300_000L, start + 300_000, 301_000L, 1);
        assertEquals(2, h.kind(start + 600_000, 601_000));
        assertEquals(true, h.field("stopBedtime"));
        assertEquals(0, h.kind(start + 600_001, 601_001));
    }

    @Test public void bedtimeEnabledAfterItsTimeStartsTomorrow() throws Exception {
        Harness h = new Harness();
        h.call("setBedtime", true, 22 * 60 + 30, wall("2026-10-19T23:00:00Z"), UTC);
        assertEquals(wall("2026-10-20T22:30:00Z"), h.field("bedtimeWall"));
    }

    @Test public void dismissedAndStaleEventsDoNotReopen() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 1_000L, 1_000_000L, 1_000L, 1);
        assertEquals(2, h.kind(1_001_000L, 2_000L));
        h.call("dismiss", (long) h.field("stopVersion"));
        assertEquals(false, h.field("stopPending"));
        assertEquals(false, h.call("extend", 1_001_000L, 2_000L, 1));
        assertEquals(0, h.kind(1_001_001L, 2_001L));
        h.call("startTimer", 1_000L, 2_000_000L, 3_000L, 1);
        assertEquals(0, h.kind(2_200_000L, 203_000L));
    }

    @Test public void standbyDoesNotProduceAStopAfterTheDisplayWakes() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 1_000L, 1_000_000L, 1_000L, 1);
        h.set("lastScreenOnWall", 1_002_000L);
        h.set("lastScreenOnElapsed", 3_000L);
        assertEquals(0, h.kind(1_002_000L, 3_000L));
        assertEquals(false, h.field("stopPending"));
    }

    @Test public void rebootRestoresFutureTimersAndDropsExpiredTimers() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 600_000L, 1_000_000L, 900_000L, 1);
        h.call("restoreBoot", 1_100_000L, 1_000L, 2);
        assertEquals(501_000L, h.field("timerElapsed"));
        h.call("restoreBoot", 1_700_000L, 1_000L, 3);
        assertEquals(0L, h.field("timerElapsed"));
    }

    @Test public void clockCorrectionThenRebootPreservesTheRemainingTimer() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 600_000L, 1_000_000L, 10_000L, 1);
        h.call("clockChanged", 4_700_000L, 110_000L, 1, UTC);
        h.call("restoreBoot", 4_800_000L, 1_000L, 2);
        assertEquals(401_000L, h.field("timerElapsed"));
    }

    @Test public void backwardClockCorrectionDoesNotSuppressAnAwakeTimer() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 600_000L, 10_000_000L, 10_000L, 1);
        h.set("lastScreenOnWall", 10_000_000L);
        h.set("lastScreenOnElapsed", 10_000L);
        h.call("clockChanged", 6_410_000L, 20_000L, 1, UTC);
        assertEquals(2, h.kind(7_000_000L, 610_000L));
    }

    @Test public void rebootDoesNotRestoreAnOldStopScreen() throws Exception {
        Harness h = new Harness();
        h.call("startTimer", 1_000L, 1_000_000L, 1_000L, 1);
        assertEquals(2, h.kind(1_001_000L, 2_000L));
        h.call("restoreBoot", 1_300_000L, 1_000L, 2);
        assertEquals(false, h.field("stopPending"));
    }

    @Test public void dstGapUsesFirstValidTimeAndRepeatedTimeFiresOnce() throws Exception {
        Harness h = new Harness();
        ZoneId zone = ZoneId.of("Europe/Brussels");
        h.call("setBedtime", true, 150, wall("2026-03-28T23:00:00Z"), zone);
        assertEquals(wall("2026-03-29T01:00:00Z"), h.field("bedtimeWall"));
        h.call("setBedtime", true, 150, wall("2026-10-24T22:00:00Z"), zone);
        assertEquals(wall("2026-10-25T00:30:00Z"), h.field("bedtimeWall"));
        h.call("evaluate", wall("2026-10-25T00:30:00Z"), 1_000L, 1, zone, true);
        assertEquals(wall("2026-10-26T01:30:00Z"), h.field("bedtimeWall"));
    }

    private static class Harness {
        final Class<?> type;
        final Object value;
        Harness() throws Exception {
            try { type = Class.forName("com.amine.tvclock.TimeLimits"); }
            catch (ClassNotFoundException e) { throw new AssertionError("Time limits are not implemented", e); }
            value = type.getDeclaredConstructor().newInstance();
        }
        Object call(String name, Object... args) throws Exception {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    try { return method.invoke(value, args); }
                    catch (InvocationTargetException e) { throw new AssertionError(e.getCause()); }
                }
            }
            throw new AssertionError("Missing behavior: " + name);
        }
        Object field(String name) throws Exception { return type.getDeclaredField(name).get(value); }
        void set(String name, Object data) throws Exception { type.getDeclaredField(name).set(value, data); }
        int kind(long wall, long elapsed) throws Exception {
            Object event = call("evaluate", wall, elapsed, 1, UTC, true);
            return event.getClass().getDeclaredField("kind").getInt(event);
        }
    }
}
