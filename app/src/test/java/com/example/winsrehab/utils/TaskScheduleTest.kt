package com.example.winsrehab.utils

import com.example.winsrehab.data.entity.TrainingTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// TaskSchedule 的单测：覆盖计划开始前/后、三种频率、重复调用与非法的脏数据
class TaskScheduleTest {

    // 2026-11-09 是周一，正好对应开发计划里的演示日期
    private val planStart = LocalDate.of(2026, 11, 9)

    // 造一个只关心频率的任务模板，其他字段给固定值
    private fun task(frequency: String, detail: String = ""): TrainingTask {
        return TrainingTask(
            planId = 1L,
            taskName = "测试任务",
            taskType = "upper_limb",
            suggestedDuration = 10,
            frequency = frequency,
            frequencyDetail = detail
        )
    }

    @Test
    fun `daily 从计划开始日起每天都生成`() {
        val daily = task(TaskSchedule.FREQUENCY_DAILY)

        for (offset in 0L..13L) {
            val date = planStart.plusDays(offset)
            assertTrue("第 $offset 天应该生成", TaskSchedule.shouldGenerate(daily, planStart, date))
        }
    }

    @Test
    fun `计划开始前一律不生成`() {
        val beforeStart = planStart.minusDays(1)

        assertFalse(TaskSchedule.shouldGenerate(task("daily"), planStart, beforeStart))
        assertFalse(TaskSchedule.shouldGenerate(task("weekly", "1,3,5"), planStart, beforeStart))
        // 间隔 1 天也要靠"开始前"这道闸拦住，否则 -1 % 1 == 0 会误生成
        assertFalse(TaskSchedule.shouldGenerate(task("interval", "1"), planStart, beforeStart))
    }

    @Test
    fun `weekly 只在一三五生成`() {
        val monWedFri = task(TaskSchedule.FREQUENCY_WEEKLY, "1,3,5")

        assertTrue("周一", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 9)))
        assertFalse("周二", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 10)))
        assertTrue("周三", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 11)))
        assertFalse("周四", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 12)))
        assertTrue("周五", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 13)))
        assertFalse("周六", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 14)))
        assertFalse("周日", TaskSchedule.shouldGenerate(monWedFri, planStart, LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun `weekly 支持用 7 表示周日`() {
        val sundayOnly = task(TaskSchedule.FREQUENCY_WEEKLY, "7")

        assertFalse(TaskSchedule.shouldGenerate(sundayOnly, planStart, LocalDate.of(2026, 11, 14)))
        assertTrue(TaskSchedule.shouldGenerate(sundayOnly, planStart, LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun `interval 以计划开始日为零点按间隔生成`() {
        val everyTwoDays = task(TaskSchedule.FREQUENCY_INTERVAL, "2")

        assertTrue("startDate 当天", TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart))
        assertFalse("第 1 天", TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(1)))
        assertTrue("第 2 天", TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(2)))
        assertFalse("第 3 天", TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(3)))
        assertTrue("第 4 天", TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(4)))

        // 第 3 天（间隔 3 天）的节奏单独验一次
        val everyThreeDays = task(TaskSchedule.FREQUENCY_INTERVAL, "3")
        assertTrue(TaskSchedule.shouldGenerate(everyThreeDays, planStart, planStart.plusDays(3)))
        assertFalse(TaskSchedule.shouldGenerate(everyThreeDays, planStart, planStart.plusDays(4)))
    }

    @Test
    fun `interval 不依赖调用历史`() {
        val everyTwoDays = task(TaskSchedule.FREQUENCY_INTERVAL, "2")

        // 直接跳到第 10 天（模拟患者漏签后重新打开 App），结果只和日期有关
        assertTrue(TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(10)))
        assertFalse(TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(11)))

        // 同一天重复调用，结果必须一致
        repeat(3) {
            assertTrue(TaskSchedule.shouldGenerate(everyTwoDays, planStart, planStart.plusDays(10)))
        }
    }

    @Test
    fun `倒序调用与正序调用结果一致`() {
        val everyThreeDays = task(TaskSchedule.FREQUENCY_INTERVAL, "3")
        val dates = (0L..20L).map { planStart.plusDays(it) }

        val forward = dates.map { TaskSchedule.shouldGenerate(everyThreeDays, planStart, it) }
        val backward = dates.reversed().map { TaskSchedule.shouldGenerate(everyThreeDays, planStart, it) }.reversed()

        assertEquals(forward, backward)
    }

    @Test
    fun `字符串日期版本与 LocalDate 版本结果一致`() {
        val weekly = task(TaskSchedule.FREQUENCY_WEEKLY, "1,3,5")

        assertEquals(
            TaskSchedule.shouldGenerate(weekly, planStart, LocalDate.of(2026, 11, 11)),
            TaskSchedule.shouldGenerate(weekly, "2026-11-09", "2026-11-11")
        )
        assertTrue(TaskSchedule.shouldGenerate(weekly, "2026-11-09", "2026-11-13"))
        assertFalse(TaskSchedule.shouldGenerate(weekly, "2026-11-09", "2026-11-10"))
    }

    @Test
    fun `非法输入返回 false 而不崩溃`() {
        // 频率类型不认识
        assertFalse(TaskSchedule.shouldGenerate("monthly", "", planStart, planStart))
        // weekly 没有配置星期几
        assertFalse(TaskSchedule.shouldGenerate(task("weekly", ""), planStart, planStart))
        // interval 配置成 0 或非数字
        assertFalse(TaskSchedule.shouldGenerate(task("interval", "0"), planStart, planStart))
        assertFalse(TaskSchedule.shouldGenerate(task("interval", "abc"), planStart, planStart))
        // 日期字符串格式不对
        assertFalse(TaskSchedule.shouldGenerate(task("daily"), "2026/11/09", "2026-11-10"))
        assertFalse(TaskSchedule.shouldGenerate(task("daily"), "2026-11-09", "not-a-date"))
    }

    @Test
    fun `parseWeekDays 过滤越界和非法值`() {
        assertEquals(setOf(1, 3, 5), TaskSchedule.parseWeekDays("1,3,5"))
        assertEquals(setOf(1, 3, 5), TaskSchedule.parseWeekDays(" 1, 3 ,5 "))
        assertEquals(setOf(1, 7), TaskSchedule.parseWeekDays("0,1,7,8,abc"))
        assertEquals(emptySet<Int>(), TaskSchedule.parseWeekDays(""))
    }
}
