package com.example.winsrehab.utils

import com.example.winsrehab.data.entity.TrainingTask
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 计划完成进度口径：纯函数，只依赖计划窗口、任务频率与今天，不查已生成实例
object PlanProgress {

    // 窗口最多算 3660 天（约 10 年），只可能来自脏数据，避免逐日循环被拖长
    private const val MAX_WINDOW_DAYS = 3660L

    // 应完成数 = 计划内每个任务在 [startDate, min(today, endDate)] 内按频率应生成的天数之和
    fun computeDueCount(
        tasks: List<TrainingTask>,
        planStartDate: LocalDate,
        planEndDate: LocalDate,
        today: LocalDate
    ): Int {
        val windowEnd = minOf(today, planEndDate)
        if (windowEnd.isBefore(planStartDate)) return 0

        var windowDays = ChronoUnit.DAYS.between(planStartDate, windowEnd) + 1
        if (windowDays > MAX_WINDOW_DAYS) windowDays = MAX_WINDOW_DAYS

        var due = 0
        for (offset in 0 until windowDays) {
            val date = planStartDate.plusDays(offset)
            for (task in tasks) {
                if (TaskSchedule.shouldGenerate(task, planStartDate, date)) {
                    due++
                }
            }
        }
        return due
    }

    // 百分比：向下取整并限制在 0..100；应完成数为 0 时记 0，避免除以零
    fun computePercent(completed: Int, due: Int): Int {
        if (due <= 0) return 0
        return ((completed.toLong() * 100L) / due).toInt().coerceIn(0, 100)
    }
}
