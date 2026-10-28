package com.amine.familyhome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Constant-size day targets keep D-pad focus independent of the measured bar height. */
@Composable
internal fun UsageHistoryScreen(usage: Map<String, Map<Viewer, Long>>, viewer: Viewer,
    today: LocalDate, resumeGeneration: Int, restoreFocus: Boolean) {
    val history = remember(usage, viewer, today) { UsageHistory(usage, viewer, today) }
    var savedWeek by rememberSaveable { mutableStateOf(weekStart(today).toString()) }
    var selectedDay by rememberSaveable { mutableIntStateOf(today.dayOfWeek.value - 1) }
    var focused by rememberSaveable { mutableStateOf("day") }
    val start = LocalDate.parse(savedWeek).coerceIn(history.firstWeek, history.lastWeek)
    val week = history.week(start)
    val days = remember { List(7) { FocusRequester() } }
    val earlier = remember { FocusRequester() }
    val later = remember { FocusRequester() }
    val canEarlier = start > history.firstWeek
    val canLater = start < history.lastWeek
    val upper = when {
        canEarlier -> earlier
        canLater -> later
        else -> FocusRequester.Cancel
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()) }
    val rangeFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val weekdayFormat = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val day = week.days[selectedDay]
    Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("usage-chart")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Recorded week total", color = Muted, fontSize = 15.sp)
                Text(usageTotal(week.totalMs.takeIf { week.days.any { it.milliseconds != null } }), fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("usage-week-total"))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${start.format(rangeFormat)} – ${start.plusDays(6).format(rangeFormat)}",
                    color = Muted, fontSize = 17.sp)
                if (week.days.any { it.milliseconds != null }) Text("Max day ${usageTotal(week.scaleMaxMs)}",
                    color = Muted, fontSize = 15.sp, modifier = Modifier.testTag("usage-scale"))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvAction("Earlier week", Modifier.testTag("usage-earlier").focusRequester(earlier)
                .onFocusChanged { if (it.isFocused) focused = "earlier" }
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = if (canLater) later else FocusRequester.Cancel
                    up = FocusRequester.Cancel; down = days[selectedDay]
                }, enabled = canEarlier) { savedWeek = start.minusWeeks(1).toString() }
            TvAction("Later week", Modifier.testTag("usage-later").focusRequester(later)
                .onFocusChanged { if (it.isFocused) focused = "later" }
                .focusProperties {
                    left = if (canEarlier) earlier else FocusRequester.Cancel
                    right = FocusRequester.Cancel
                    up = FocusRequester.Cancel; down = days[selectedDay]
                }, enabled = canLater) { savedWeek = start.plusWeeks(1).toString() }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.days.forEachIndexed { index, item ->
                var hasFocus by remember { mutableStateOf(false) }
                Column(Modifier.weight(1f).height(158.dp).testTag("usage-day:${item.date}")
                    .focusRequester(days[index]).focusProperties {
                        left = if (index > 0) days[index - 1] else FocusRequester.Cancel
                        right = if (index < 6) days[index + 1] else FocusRequester.Cancel
                        up = upper; down = FocusRequester.Cancel
                    }.onFocusChanged {
                        hasFocus = it.isFocused
                        if (it.isFocused) { selectedDay = index; focused = "day" }
                    }.border(if (hasFocus) 2.dp else 0.dp, if (hasFocus) Gold else Background,
                        RoundedCornerShape(10.dp))
                    .background(if (hasFocus) Panel else Background.copy(alpha = .2f), RoundedCornerShape(10.dp))
                    .semantics {
                        selected = index == selectedDay
                        contentDescription = "${item.date.format(dateFormat)}, ${item.description(today)}"
                    }.focusable().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        val fraction = item.fractionOf(week.scaleMaxMs)
                        if (fraction > 0f) Box(Modifier.width(24.dp)
                            .height(maxOf(2.dp, maxHeight * fraction.coerceAtMost(1f)))
                            .background(if (hasFocus) Gold else Muted, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        else Box(Modifier.width(24.dp).height(2.dp).background(Muted.copy(alpha = .35f)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.date.format(weekdayFormat), color = if (hasFocus) Gold else Ink, fontSize = 15.sp)
                    Text(item.date.dayOfMonth.toString(), color = Muted, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(day.date.format(dateFormat), color = Muted, fontSize = 17.sp,
            modifier = Modifier.testTag("usage-selected-date"))
        Text(day.description(today), fontSize = 23.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("usage-selected-duration"))
    }
    LaunchedEffect(start, resumeGeneration, restoreFocus) {
        if (restoreFocus) {
            withFrameNanos { }
            val target = when (focused) {
                "earlier" -> when { canEarlier -> earlier; canLater -> later; else -> days[selectedDay] }
                "later" -> when { canLater -> later; canEarlier -> earlier; else -> days[selectedDay] }
                else -> days[selectedDay]
            }
            target.requestFocus()
        }
    }
}
