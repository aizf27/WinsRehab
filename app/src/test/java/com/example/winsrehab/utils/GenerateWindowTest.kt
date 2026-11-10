package com.example.winsrehab.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 生成时间窗判断（plan 级窗口，纯函数）
class GenerateWindowTest {

    @Test
    fun endDateDayIsInWindow() {
        assertTrue(PlanDates.isWithinPlanWindow("2026-11-01", "2026-11-10", "2026-11-10"))
    }

    @Test
    fun expiredPlanIsOutOfWindow() {
        assertFalse(PlanDates.isWithinPlanWindow("2026-09-01", "2026-09-30", "2026-11-10"))
    }

    @Test
    fun notStartedPlanIsOutOfWindow() {
        assertFalse(PlanDates.isWithinPlanWindow("2026-11-11", "2026-11-20", "2026-11-10"))
    }

    @Test
    fun invalidEndDateIsOutOfWindow() {
        assertFalse(PlanDates.isWithinPlanWindow("2026-11-01", "oops", "2026-11-05"))
    }
}
