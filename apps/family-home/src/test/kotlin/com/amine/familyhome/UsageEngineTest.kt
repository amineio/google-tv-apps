package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class UsageEngineTest {
    private val utc = ZoneId.of("UTC")
    private fun sample(elapsed: Long, wall: String, zone: ZoneId = utc) =
        UsageSample(elapsed, Instant.parse(wall).toEpochMilli(), zone)

    @Test fun midnightSplitsElapsedTimeBetweenLocalDates() {
        val cursor = UsageCursor(sample(0, "2026-10-27T23:59:50Z"), UsageActivity(Viewer.AMINE))
        val result = UsageEngine.observe(emptyMap(), cursor,
            sample(20_000, "2026-10-28T00:00:10Z"), UsageActivity(Viewer.AMINE))
        assertEquals(mapOf("2026-10-27" to mapOf(Viewer.AMINE to 10_000L),
            "2026-10-28" to mapOf(Viewer.AMINE to 10_000L)), result.usage)
    }

    @Test fun daylightSavingUsesActualTwentyThreeAndTwentyFiveHourDays() {
        val brussels = ZoneId.of("Europe/Brussels")
        for ((date, hours) in listOf("2026-03-29" to 23L, "2026-10-25" to 25L)) {
            val start = LocalDate.parse(date).atStartOfDay(brussels).toInstant()
            val duration = hours * 3_600_000
            val cursor = UsageCursor(UsageSample(0, start.toEpochMilli(), brussels), UsageActivity(Viewer.AMINE))
            val result = UsageEngine.observe(emptyMap(), cursor,
                UsageSample(duration + 1_000, start.plusMillis(duration + 1_000).toEpochMilli(), brussels), null)
            assertEquals(duration, result.usage.getValue(date).getValue(Viewer.AMINE))
            assertEquals(1_000L, result.usage.getValue(LocalDate.parse(date).plusDays(1).toString()).getValue(Viewer.AMINE))
        }
    }

    @Test fun clockAndZoneChangesReanchorWithoutAddingDuration() {
        val cursor = UsageCursor(sample(0, "2026-10-28T10:00:00Z"), UsageActivity(Viewer.AMINE))
        val jumped = sample(10_000, "2026-11-10T23:59:55Z", ZoneId.of("America/New_York"))
        val first = UsageEngine.observe(emptyMap(), cursor, jumped, UsageActivity(Viewer.AMINE))
        val second = UsageEngine.observe(first.usage, first.cursor,
            sample(20_000, "2026-09-01T00:00:00Z"), null)
        assertEquals(10_000L, second.usage.getValue("2026-10-28").getValue(Viewer.AMINE))
        assertEquals(10_000L, second.usage.getValue("2026-11-10").getValue(Viewer.AMINE))
        assertEquals(20_000L, second.usage.values.sumOf { it.values.sum() })
    }

    @Test fun childCreditsOnlyThroughItsDeadlineAndInactiveIntervalsAddNothing() {
        val start = sample(0, "2026-10-28T10:00:00Z")
        val cursor = UsageCursor(start, UsageActivity(Viewer.ELIAS, 5_000))
        val expired = UsageEngine.observe(emptyMap(), cursor,
            sample(10_000, "2026-10-28T10:00:10Z"), null)
        val idle = UsageEngine.observe(expired.usage, expired.cursor,
            sample(1_000_000, "2026-10-28T10:16:40Z"), null)
        assertEquals(mapOf("2026-10-28" to mapOf(Viewer.ELIAS to 5_000L)), idle.usage)
    }

    @Test fun retainsOnlyThirtyRecordedDates() {
        val history = (1..30).associate { day -> "2026-09-%02d".format(day) to mapOf(Viewer.AMINE to 1_000L) }
        val cursor = UsageCursor(sample(0, "2026-10-28T10:00:00Z"), UsageActivity(Viewer.AMINE))
        val result = UsageEngine.observe(history, cursor, sample(1_000, "2026-10-28T10:00:01Z"), null)
        assertEquals(30, result.usage.size)
        assertFalse(result.usage.containsKey("2026-09-01"))
        assertEquals(1_000L, result.usage.getValue("2026-10-28").getValue(Viewer.AMINE))
    }
}
