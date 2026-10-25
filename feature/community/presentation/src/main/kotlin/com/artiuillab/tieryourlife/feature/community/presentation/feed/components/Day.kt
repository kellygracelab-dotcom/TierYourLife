package com.artiuillab.tieryourlife.feature.community.presentation.feed.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The day a list arrived, as a reader counts days: today, yesterday, or a date. */
internal sealed interface Day {
    data object Today : Day

    data object Yesterday : Day

    data class On(val date: LocalDate) : Day
}

/** A moment a little ahead of the clock is still today: a date in the future would read as a mistake. */
internal fun dayOf(millis: Long, today: LocalDate, zone: ZoneId): Day {
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return when {
        !date.isBefore(today) -> Day.Today
        date == today.minusDays(1) -> Day.Yesterday
        else -> Day.On(date)
    }
}

/** The items in the order given, cut wherever the day changes. */
internal fun <T> groupedByDay(
    items: List<T>,
    today: LocalDate,
    zone: ZoneId,
    millisOf: (T) -> Long,
): List<Pair<Day, List<T>>> {
    val groups = mutableListOf<Pair<Day, MutableList<T>>>()
    for (item in items) {
        val day = dayOf(millisOf(item), today, zone)
        val last = groups.lastOrNull()
        if (last != null && last.first == day) {
            last.second += item
        } else {
            groups += day to mutableListOf(item)
        }
    }
    return groups
}
