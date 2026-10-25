package com.artiuillab.tieryourlife.feature.community.presentation.feed

import com.artiuillab.tieryourlife.feature.community.presentation.feed.components.Day
import com.artiuillab.tieryourlife.feature.community.presentation.feed.components.dayOf
import com.artiuillab.tieryourlife.feature.community.presentation.feed.components.groupedByDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DayGroupsTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 25)

    private fun at(date: LocalDate, hour: Int = 12): Long = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun todayYesterdayAndADate_areToldApart() {
        assertEquals(Day.Today, dayOf(at(today), today, zone))
        assertEquals(Day.Yesterday, dayOf(at(today.minusDays(1)), today, zone))
        assertEquals(Day.On(LocalDate.of(2026, 10, 20)), dayOf(at(LocalDate.of(2026, 10, 20)), today, zone))
    }

    // A server clock a little ahead of the phone's is not a reason to show a date.
    @Test
    fun aMomentAhead_isStillToday() {
        assertEquals(Day.Today, dayOf(at(today.plusDays(1)), today, zone))
    }

    @Test
    fun aRunOfTheSameDay_isOneGroup_andADayChangeCutsIt() {
        val moments = listOf(at(today, 9), at(today, 8), at(today.minusDays(1)), at(LocalDate.of(2026, 10, 1)))

        val groups = groupedByDay(moments, today, zone) { it }

        assertEquals(listOf(Day.Today, Day.Yesterday, Day.On(LocalDate.of(2026, 10, 1))), groups.map { it.first })
        assertEquals(listOf(2, 1, 1), groups.map { it.second.size })
    }

    @Test
    fun nothing_groupsToNothing() {
        assertEquals(emptyList<Pair<Day, List<Long>>>(), groupedByDay<Long>(emptyList(), today, zone) { it })
    }
}
