package com.amine.familyhome

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

internal data class UsageDay(val date: LocalDate, val milliseconds: Long?) {
    fun description(today: LocalDate): String = if (milliseconds == null && date > today) "Future date"
        else usageTotal(milliseconds)

    fun fractionOf(maximum: Long): Float = if (milliseconds == null || maximum <= 0) 0f
        else (milliseconds.toDouble() / maximum).toFloat()
}

internal data class UsageWeek(val days: List<UsageDay>) {
    val totalMs: Long = days.sumOf { it.milliseconds ?: 0L }
    val scaleMaxMs: Long = days.maxOf { it.milliseconds ?: 0L }
}

/** A read-only view of retained daily totals. Missing records remain distinct from zero. */
internal class UsageHistory(usage: Map<String, Map<Viewer, Long>>, viewer: Viewer, today: LocalDate) {
    private val records = usage.mapNotNull { (date, totals) ->
        totals[viewer]?.let { LocalDate.parse(date) to it }
    }.toMap()
    val firstWeek: LocalDate = weekStart(minOf(today, records.keys.minOrNull() ?: today))
    val lastWeek: LocalDate = weekStart(maxOf(today, records.keys.maxOrNull() ?: today))

    fun week(start: LocalDate): UsageWeek = UsageWeek((0L..6L).map { offset ->
        val date = start.plusDays(offset)
        UsageDay(date, records[date])
    })
}

internal fun usageTotal(milliseconds: Long?): String {
    if (milliseconds == null) return "No recorded use"
    if (milliseconds == 0L) return "0 min"
    val minutes = milliseconds / 60_000
    if (minutes == 0L) return "Less than 1 min"
    val hours = minutes / 60
    return when {
        hours == 0L -> "$minutes min"
        minutes % 60 == 0L -> "$hours h"
        else -> "$hours h ${minutes % 60} min"
    }
}
