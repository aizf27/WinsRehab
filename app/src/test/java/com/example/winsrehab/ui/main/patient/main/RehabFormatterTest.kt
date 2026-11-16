package com.example.winsrehab.ui.main.patient.main

import org.junit.Assert.assertEquals
import org.junit.Test

// RehabFormatter：柱高、百分比、今天标记（占位与三态文案已上收到 UiTexts，见 UiTextsTest）
class RehabFormatterTest {

    @Test
    fun barIsZeroWhenTotalIsZero() {
        assertEquals(0f, RehabFormatter.barRatio(0, 0), 0.0001f)
        assertEquals(0, RehabFormatter.barHeightDp(0, 0))
    }

    @Test
    fun barIsProportional() {
        assertEquals(0.5f, RehabFormatter.barRatio(1, 2), 0.0001f)
        assertEquals(50, RehabFormatter.barHeightDp(1, 2))
    }

    @Test
    fun percentReusesPlanProgress() {
        assertEquals(75, RehabFormatter.percent(3, 4))
        assertEquals(0, RehabFormatter.percent(0, 0))
        assertEquals(100, RehabFormatter.percent(5, 2))
    }

    @Test
    fun todayBarIsLabeledToday() {
        assertEquals("今天", RehabFormatter.dayLabel(WeekBarData("2026-11-10", 1, 2, true)))
    }

    @Test
    fun otherBarsUseWeekdayLabel() {
        // 2026-11-09 周一，2026-11-15 周日
        assertEquals("周一", RehabFormatter.dayLabel(WeekBarData("2026-11-09", 0, 0, false)))
        assertEquals("周日", RehabFormatter.dayLabel(WeekBarData("2026-11-15", 0, 0, false)))
    }

    @Test
    fun textFormats() {
        assertEquals("1/2 项已完成", RehabFormatter.progressCountText(1, 2))
        assertEquals("完成率: 50%", RehabFormatter.progressPercentText(1, 2))
        assertEquals("1/2", RehabFormatter.weekCountText(WeekBarData("2026-11-09", 1, 2, false)))
    }
}
