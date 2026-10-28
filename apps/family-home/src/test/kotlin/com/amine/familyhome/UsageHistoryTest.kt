package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class UsageHistoryTest {
    private val today = LocalDate.of(2026, 10, 28)
    private fun date(value: String) = LocalDate.parse(value)

    @Test fun weekHasSevenMondayThroughSundayDatesAcrossYearBoundary() {
        val history = UsageHistory(emptyMap(), Viewer.ELIAS, date("2027-01-01"))
        val week = history.week(weekStart(date("2027-01-01")))
        assertEquals((0L..6L).map { date("2026-12-28").plusDays(it) }, week.days.map { it.date })
        assertEquals(history.firstWeek, history.lastWeek)
        assertEquals(0L, week.totalMs)
        assertEquals(0L, week.scaleMaxMs)
    }

    @Test fun totalsAndBoundsUseOnlyTheSelectedProfileIncludingRetainedFutureDates() {
        val usage = mapOf(
            "2026-09-01" to mapOf(Viewer.SOFIA to 99L),
            "2026-10-20" to mapOf(Viewer.ELIAS to 60_000L),
            "2026-10-26" to mapOf(Viewer.ELIAS to 120_000L, Viewer.SOFIA to 900_000L),
            "2026-10-27" to mapOf(Viewer.ELIAS to 60_000L),
            "2026-11-12" to mapOf(Viewer.ELIAS to 0L),
        )
        val history = UsageHistory(usage, Viewer.ELIAS, today)
        assertEquals(date("2026-10-19"), history.firstWeek)
        assertEquals(date("2026-11-09"), history.lastWeek)
        val week = history.week(weekStart(today))
        assertEquals(180_000L, week.totalMs)
        assertEquals(120_000L, week.scaleMaxMs)
        assertEquals(listOf(120_000L, 60_000L, null, null, null, null, null), week.days.map { it.milliseconds })
        assertEquals(1f, week.days[0].fractionOf(week.scaleMaxMs), 0f)
        assertEquals(.5f, week.days[1].fractionOf(week.scaleMaxMs), 0f)
        assertEquals(0f, week.days[2].fractionOf(week.scaleMaxMs), 0f)
    }

    @Test fun missingZeroAndFutureRemainDistinctAndShortUseIsNotRoundedToZero() {
        assertEquals("No recorded use", UsageDay(today.minusDays(1), null).description(today))
        assertEquals("No recorded use", UsageDay(today, null).description(today))
        assertEquals("Future date", UsageDay(today.plusDays(1), null).description(today))
        assertEquals("0 min", UsageDay(today.plusDays(1), 0L).description(today))
        assertEquals("Less than 1 min", UsageDay(today, 1L).description(today))
        assertEquals("1 h 15 min", UsageDay(today, 4_500_000L).description(today))
        assertTrue(UsageDay(today, 1L).fractionOf(3_600_000L) > 0f)
        assertEquals(0f, UsageDay(today, 0L).fractionOf(0L), 0f)
    }

    @Test fun currentWeekAlwaysRemainsInBoundsAndPagingKeepsTheWeekday() {
        val old = UsageHistory(mapOf("2026-09-01" to mapOf(Viewer.ELIAS to 1L)), Viewer.ELIAS, today)
        assertEquals(weekStart(today), old.lastWeek)
        assertEquals(today.minusWeeks(1).dayOfWeek, old.week(weekStart(today).minusWeeks(1)).days[today.dayOfWeek.value - 1].date.dayOfWeek)
        val future = UsageHistory(mapOf("2026-11-12" to mapOf(Viewer.ELIAS to 1L)), Viewer.ELIAS, today)
        assertEquals(weekStart(today), future.firstWeek)
    }
}
