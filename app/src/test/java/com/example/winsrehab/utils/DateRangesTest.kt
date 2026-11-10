package com.example.winsrehab.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// DateRanges：周一至周日、周日边界、跨月跨年
class DateRangesTest {

    @Test
    fun mondayIsStartOfWeek() {
        val (start, end) = DateRanges.weekRange(LocalDate.of(2026, 11, 9))
        assertEquals(LocalDate.of(2026, 11, 9), start)
        assertEquals(LocalDate.of(2026, 11, 15), end)
    }

    @Test
    fun sundayBelongsToSameWeek() {
        val (start, end) = DateRanges.weekRange(LocalDate.of(2026, 11, 15))
        assertEquals(LocalDate.of(2026, 11, 9), start)
        assertEquals(LocalDate.of(2026, 11, 15), end)
    }

    @Test
    fun crossesMonth() {
        val (start, end) = DateRanges.weekRange(LocalDate.of(2026, 12, 1))
        assertEquals(LocalDate.of(2026, 11, 30), start)
        assertEquals(LocalDate.of(2026, 12, 6), end)
    }

    @Test
    fun crossesYear() {
        val (start, end) = DateRanges.weekRange(LocalDate.of(2027, 1, 1))
        assertEquals(LocalDate.of(2026, 12, 28), start)
        assertEquals(LocalDate.of(2027, 1, 3), end)
    }

    @Test
    fun stringOverloadInvalidFallsBackToToday() {
        val (startStr, endStr) = DateRanges.weekRange("bad")
        val (start, end) = DateRanges.weekRange(LocalDate.now())
        assertEquals(start.toString(), startStr)
        assertEquals(end.toString(), endStr)
    }

    @Test
    fun stringOverloadWithTimeIsInvalidAndFallsBack() {
        val (startStr, _) = DateRanges.weekRange("2026-11-10 08:00")
        val (start, _) = DateRanges.weekRange(LocalDate.now())
        assertEquals(start.toString(), startStr)
    }
}
