package com.example.winsrehab.utils

import com.example.winsrehab.data.entity.TrainingTask
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// PlanProgress 纯函数边界：应完成数与百分比
class PlanProgressTest {

    private fun task(frequency: String, detail: String = "", id: Long = 1) = TrainingTask(
        taskId = id,
        planId = 1,
        taskName = "t$id",
        taskType = "upper_limb",
        suggestedDuration = 10,
        frequency = frequency,
        frequencyDetail = detail
    )

    @Test
    fun dailyCountsSingleDay() {
        val due = PlanProgress.computeDueCount(
            listOf(task("daily")),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 10)
        )
        assertEquals(1, due)
    }

    @Test
    fun weeklyCountsOnlyHitDays() {
        // 2026-11-09 是周一；周一/三/五 命中 11-09、11-11、11-13
        val due = PlanProgress.computeDueCount(
            listOf(task("weekly", "1,3,5")),
            LocalDate.of(2026, 11, 9),
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 11, 13)
        )
        assertEquals(3, due)
    }

    @Test
    fun intervalAnchorsAtPlanStart() {
        // 从 11-10 起隔 2 天：11-10、11-12 命中，11-13 不命中
        val due = PlanProgress.computeDueCount(
            listOf(task("interval", "2")),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 13)
        )
        assertEquals(2, due)
    }

    @Test
    fun todayBeforeStartIsZero() {
        val due = PlanProgress.computeDueCount(
            listOf(task("daily")),
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 11, 10)
        )
        assertEquals(0, due)
    }

    @Test
    fun todayAfterEndCapsAtEndDate() {
        val due = PlanProgress.computeDueCount(
            listOf(task("daily")),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12),
            LocalDate.of(2026, 11, 20)
        )
        assertEquals(3, due)
    }

    @Test
    fun missingGenerationDoesNotShrinkDueCount() {
        // 分母只按排期推算，与库里是否生成过实例无关
        val due = PlanProgress.computeDueCount(
            listOf(task("daily")),
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 11, 12)
        )
        assertEquals(3, due)
    }

    @Test
    fun percentZeroWhenDueIsZero() {
        assertEquals(0, PlanProgress.computePercent(0, 0))
    }

    @Test
    fun percentFloorsAndClamps() {
        assertEquals(75, PlanProgress.computePercent(3, 4))
        assertEquals(33, PlanProgress.computePercent(1, 3))
        assertEquals(100, PlanProgress.computePercent(5, 2))
        assertEquals(0, PlanProgress.computePercent(0, 2))
    }

    @Test
    fun hugeWindowIsTruncatedTo3660() {
        val due = PlanProgress.computeDueCount(
            listOf(task("daily")),
            LocalDate.of(2000, 1, 1),
            LocalDate.of(2030, 1, 1),
            LocalDate.of(2030, 1, 1)
        )
        assertEquals(3660, due)
    }
}
