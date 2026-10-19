package com.amine.tvclock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/** Deadline state only. No Android APIs, playback detection, or polling. */
final class TimeLimits {
    static final long FIVE_MINUTES = 300_000;
    static final int NONE = 0, WARNING = 1, STOP = 2;
    long timerWall, timerElapsed;
    int timerBoot;
    int stateBoot;
    boolean bedtimeEnabled;
    int bedtimeMinute = 22 * 60 + 30;
    long bedtimeWall;
    long lastBedtimeDay = Long.MIN_VALUE;
    long extensionWall, extensionElapsed;
    int extensionBoot;
    boolean extensionBedtime;
    String warnedToken = "";
    boolean stopPending, stopBedtime, stopExtended;
    long stopVersion, lastScreenOnWall, lastScreenOnElapsed;
    boolean stopScreen = true;

    static final class Event {
        final int kind;
        final boolean bedtime;
        final long wall, version;
        Event(int kind, boolean bedtime, long wall, long version) {
            this.kind = kind; this.bedtime = bedtime; this.wall = wall; this.version = version;
        }
    }

    static final class Deadline {
        final int source;
        final long wall, elapsed;
        final boolean bedtime;
        final String token;
        Deadline(int source, long wall, long elapsed, boolean bedtime, String token) {
            this.source = source; this.wall = wall; this.elapsed = elapsed;
            this.bedtime = bedtime; this.token = token;
        }
    }

    void startTimer(long duration, long wall, long elapsed, int boot) {
        if (duration <= 0) throw new IllegalArgumentException("Timer duration must be positive");
        timerWall = wall + duration; timerElapsed = elapsed + duration; timerBoot = boot;
        stateBoot = boot;
        if (!extensionBedtime) extensionElapsed = extensionWall = 0;
        if (!stopBedtime) stopPending = false;
        warnedToken = "";
    }

    void cancelTimer() {
        timerElapsed = timerWall = 0;
        if (!extensionBedtime) extensionElapsed = extensionWall = 0;
        if (!stopBedtime) stopPending = false;
    }

    void setBedtime(boolean enabled, int minute, long wall, ZoneId zone) {
        bedtimeEnabled = enabled;
        bedtimeMinute = Math.floorMod(minute, 24 * 60);
        bedtimeWall = enabled ? nextBedtime(wall, zone) : 0;
        if (extensionBedtime) extensionElapsed = extensionWall = 0;
        if (stopBedtime) stopPending = false;
        warnedToken = "";
    }

    void skipBedtime(long wall, ZoneId zone) {
        if (!bedtimeEnabled) return;
        lastBedtimeDay = Math.max(lastBedtimeDay,
                Instant.ofEpochMilli(bedtimeWall).atZone(zone).toLocalDate().toEpochDay());
        bedtimeWall = nextBedtime(wall, zone);
    }

    void clockChanged(long wall, long elapsed, int boot, ZoneId zone) {
        restoreBoot(wall, elapsed, boot);
        if (timerElapsed > 0) timerWall = wall + Math.max(0, timerElapsed - elapsed);
        if (extensionElapsed > 0) extensionWall = wall + Math.max(0, extensionElapsed - elapsed);
        if (bedtimeEnabled) bedtimeWall = nextBedtime(wall, zone);
    }

    void restoreBoot(long wall, long elapsed, int boot) {
        if (stateBoot != 0 && stateBoot != boot) {
            stopPending = false;
            lastScreenOnWall = wall;
            lastScreenOnElapsed = elapsed;
        }
        stateBoot = boot;
        if (timerElapsed > 0 && timerBoot != boot) {
            timerElapsed = timerWall > wall ? elapsed + timerWall - wall : 0;
            if (timerElapsed == 0) timerWall = 0;
            timerBoot = boot;
        }
        if (extensionElapsed > 0 && extensionBoot != boot) {
            extensionElapsed = extensionWall > wall ? elapsed + extensionWall - wall : 0;
            if (extensionElapsed == 0) extensionWall = 0;
            extensionBoot = boot;
        }
    }

    Deadline next(long wall, long elapsed, int boot) {
        restoreBoot(wall, elapsed, boot);
        Deadline result = null;
        if (timerElapsed > 0) result = new Deadline(0, wall + timerElapsed - elapsed,
                timerElapsed, false, "timer:" + timerElapsed + ":" + timerBoot);
        if (bedtimeEnabled) {
            Deadline bed = new Deadline(1, bedtimeWall, elapsed + bedtimeWall - wall,
                    true, "bedtime:" + bedtimeWall);
            if (result == null || bed.elapsed <= result.elapsed) result = bed;
        }
        if (extensionElapsed > 0) {
            Deadline extra = new Deadline(2, wall + extensionElapsed - elapsed, extensionElapsed,
                    extensionBedtime, "extension:" + extensionElapsed + ":" + extensionBoot);
            if (result == null || extra.elapsed < result.elapsed) result = extra;
        }
        return result;
    }

    Event evaluate(long wall, long elapsed, int boot, ZoneId zone, boolean interactive) {
        Deadline first = next(wall, elapsed, boot);
        if (first == null) return new Event(NONE, false, 0, stopVersion);
        if (first.elapsed <= elapsed) {
            // Consume every overdue source, but present the most recent due event.
            Deadline cause = first;
            if (timerElapsed > 0 && timerElapsed <= elapsed) {
                long due = wall + timerElapsed - elapsed;
                if (due > cause.wall) cause = new Deadline(0, due, timerElapsed, false, "");
                timerElapsed = timerWall = 0;
            }
            if (extensionElapsed > 0 && extensionElapsed <= elapsed) {
                long due = wall + extensionElapsed - elapsed;
                if (due >= cause.wall) cause = new Deadline(2, due, extensionElapsed, extensionBedtime, "");
                extensionElapsed = extensionWall = 0;
            }
            if (bedtimeEnabled && bedtimeWall <= wall) {
                if (bedtimeWall >= cause.wall) cause = new Deadline(1, bedtimeWall,
                        elapsed + bedtimeWall - wall, true, "");
                lastBedtimeDay = Math.max(lastBedtimeDay,
                        Instant.ofEpochMilli(bedtimeWall).atZone(zone).toLocalDate().toEpochDay());
                bedtimeWall = nextBedtime(wall, zone);
            }
            stopVersion++;
            stopBedtime = cause.bedtime;
            stopExtended = cause.source == 2;
            stopPending = interactive && elapsed - cause.elapsed <= 120_000
                    && cause.elapsed >= lastScreenOnElapsed;
            warnedToken = "";
            return new Event(stopPending ? STOP : NONE, stopBedtime, cause.wall, stopVersion);
        }
        if (first.source != 2 && first.elapsed - elapsed <= FIVE_MINUTES
                && !first.token.equals(warnedToken)) {
            warnedToken = first.token;
            return new Event(interactive ? WARNING : NONE, first.bedtime, first.wall, stopVersion);
        }
        return new Event(NONE, false, first.wall, stopVersion);
    }

    boolean canExtend(long wall, long elapsed, int boot) {
        if (!stopPending || stopExtended) return false;
        Deadline other = next(wall, elapsed, boot);
        return other == null || other.elapsed > elapsed + FIVE_MINUTES;
    }

    boolean extend(long wall, long elapsed, int boot) {
        if (!canExtend(wall, elapsed, boot)) return false;
        extensionWall = wall + FIVE_MINUTES;
        extensionElapsed = elapsed + FIVE_MINUTES;
        extensionBoot = boot;
        extensionBedtime = stopBedtime;
        stopPending = false;
        return true;
    }

    void dismiss(long version) { if (version == stopVersion) stopPending = false; }

    long checkpoint(long wall, long elapsed, int boot) {
        Deadline next = next(wall, elapsed, boot);
        if (next == null) return 0;
        if (next.source != 2 && !next.token.equals(warnedToken)
                && next.elapsed - elapsed > FIVE_MINUTES) return next.elapsed - FIVE_MINUTES;
        return Math.max(elapsed + 1, next.elapsed);
    }

    boolean active() { return timerElapsed > 0 || bedtimeEnabled || extensionElapsed > 0; }

    private long nextBedtime(long wall, ZoneId zone) {
        LocalDate day = Instant.ofEpochMilli(wall).atZone(zone).toLocalDate();
        if (day.toEpochDay() <= lastBedtimeDay) day = LocalDate.ofEpochDay(lastBedtimeDay + 1);
        long candidate = bedtimeOn(day, zone);
        return candidate > wall ? candidate : bedtimeOn(day.plusDays(1), zone);
    }

    private long bedtimeOn(LocalDate day, ZoneId zone) {
        LocalDateTime local = LocalDateTime.of(day,
                LocalTime.of(bedtimeMinute / 60, bedtimeMinute % 60));
        if (zone.getRules().getValidOffsets(local).isEmpty()) {
            local = zone.getRules().getTransition(local).getDateTimeAfter();
        }
        return local.atZone(zone).toInstant().toEpochMilli();
    }
}
