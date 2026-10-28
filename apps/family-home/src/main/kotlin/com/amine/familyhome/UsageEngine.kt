package com.amine.familyhome

import java.time.Instant
import java.time.ZoneId

data class UsageSample(val elapsedMs: Long, val wallEpochMs: Long, val zone: ZoneId)
data class UsageActivity(val viewer: Viewer, val deadlineElapsedMs: Long? = null)
data class UsageCursor(val sample: UsageSample, val activity: UsageActivity?)
data class UsageUpdate(val usage: Map<String, Map<Viewer, Long>>, val cursor: UsageCursor)

/** Elapsed time supplies duration. The previous local clock sample supplies date boundaries. */
object UsageEngine {
    const val HISTORY_DATES = 30

    fun observe(
        usage: Map<String, Map<Viewer, Long>>,
        cursor: UsageCursor?,
        sample: UsageSample,
        nextActivity: UsageActivity?,
    ): UsageUpdate {
        require(sample.elapsedMs >= 0)
        val totals = usage.toMutableMap()
        val activity = cursor?.activity
        if (cursor != null && activity != null) {
            val end = minOf(sample.elapsedMs, activity.deadlineElapsedMs ?: sample.elapsedMs)
            var remaining = (end - cursor.sample.elapsedMs).coerceAtLeast(0)
            var instant = Instant.ofEpochMilli(cursor.sample.wallEpochMs)
            while (remaining > 0) {
                val date = instant.atZone(cursor.sample.zone).toLocalDate()
                val midnight = date.plusDays(1).atStartOfDay(cursor.sample.zone).toInstant()
                val duration = minOf(remaining, midnight.toEpochMilli() - instant.toEpochMilli())
                val day = totals[date.toString()].orEmpty()
                totals[date.toString()] = day + (activity.viewer to Math.addExact(day[activity.viewer] ?: 0L, duration))
                remaining -= duration
                instant = instant.plusMillis(duration)
            }
        }
        return UsageUpdate(totals.toSortedMap().entries.toList().takeLast(HISTORY_DATES).associate { it.toPair() },
            UsageCursor(sample, nextActivity))
    }
}
