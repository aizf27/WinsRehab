package com.example.winsrehab.ui.main.patient.main

import com.example.winsrehab.utils.PlanProgress
import java.time.LocalDate

// 康复页展示口径纯函数：本页文案、柱高、今天标记；百分比复用 PlanProgress，占位与三态文案在 UiTexts
object RehabFormatter {

    private const val MAX_BAR_HEIGHT_DP = 100

    fun progressCountText(completed: Int, total: Int): String = "$completed/$total 项已完成"

    fun progressPercentText(completed: Int, total: Int): String = "完成率: ${percent(completed, total)}%"

    fun percent(completed: Int, total: Int): Int = PlanProgress.computePercent(completed, total)

    // 柱高比例：总数 0 时为 0，否则 completed/total，限制在 0..1
    fun barRatio(completed: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun barHeightDp(completed: Int, total: Int, maxHeightDp: Int = MAX_BAR_HEIGHT_DP): Int =
        (barRatio(completed, total) * maxHeightDp).toInt()

    // 今天桶显示「今天」，其余显示周几
    fun dayLabel(bar: WeekBarData): String {
        if (bar.isToday) return "今天"
        val date = runCatching { LocalDate.parse(bar.date) }.getOrNull() ?: return ""
        return when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }
    }

    fun weekCountText(bar: WeekBarData): String = "${bar.completed}/${bar.total}"
}
