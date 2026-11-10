package com.example.winsrehab.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// PlanDates 纯函数边界
class PlanDatesTest {

    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun startDateTodayIsValid() {
        assertTrue(PlanDates.isStartDateValid("2026-11-10", today))
    }

    @Test
    fun startDateTomorrowIsValid() {
        assertTrue(PlanDates.isStartDateValid("2026-11-11", today))
    }

    @Test
    fun startDateYesterdayIsInvalid() {
        assertFalse(PlanDates.isStartDateValid("2026-11-09", today))
    }

    @Test
    fun invalidStartDateIsInvalid() {
        assertFalse(PlanDates.isStartDateValid("not-a-date", today))
    }

    @Test
    fun endDateForOneDayPlanIsStartDay() {
        assertEquals("2026-11-10", PlanDates.calculateEndDate("2026-11-10", 1))
    }

    @Test
    fun endDateForSevenDayPlanCoversSevenDays() {
        assertEquals("2026-11-16", PlanDates.calculateEndDate("2026-11-10", 7))
    }

    @Test
    fun endDateWithInvalidStartReturnsNull() {
        assertNull(PlanDates.calculateEndDate("bad", 7))
    }

    @Test
    fun windowIncludesBothEnds() {
        assertTrue(PlanDates.isWithinPlanWindow("2026-11-01", "2026-11-30", "2026-11-01"))
        assertTrue(PlanDates.isWithinPlanWindow("2026-11-01", "2026-11-30", "2026-11-30"))
    }

    @Test
    fun windowExcludesOutside() {
        assertFalse(PlanDates.isWithinPlanWindow("2026-11-01", "2026-11-30", "2026-10-31"))
        assertFalse(PlanDates.isWithinPlanWindow("2026-11-01", "2026-11-30", "2026-12-01"))
    }

    @Test
    fun windowWithInvalidDateReturnsFalse() {
        assertFalse(PlanDates.isWithinPlanWindow("2026-11-01", "bad", "2026-11-10"))
    }
}
