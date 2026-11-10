package com.example.winsrehab.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// 周区间：固定「周一至周日」，不随设备区域设置变化
object DateRanges {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 本周周一 ~ 本周周日；周日属于本周最后一天，不会被推到下一周
    fun weekRange(today: LocalDate): Pair<LocalDate, LocalDate> {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return start to start.plusDays(6)
    }

    // 字符串重载：解析失败回退到今天，统一格式化成 yyyy-MM-dd 再进 SQL
    fun weekRange(today: String): Pair<String, String> {
        val base = TaskSchedule.parseDate(today) ?: LocalDate.now()
        val (start, end) = weekRange(base)
        return start.format(DATE_FORMATTER) to end.format(DATE_FORMATTER)
    }
}
