package com.example.winsrehab.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// 康复计划日期规则：纯函数，不碰数据库，可直接 JVM 单测
object PlanDates {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 开始日期不能早于今天；解析失败一律视为非法
    fun isStartDateValid(startDate: String, today: LocalDate): Boolean {
        val start = parseDate(startDate) ?: return false
        return !start.isBefore(today)
    }

    // 结束日 = 开始日 + 周期天数 - 1，这样「7 天计划」正好覆盖 7 个自然日
    fun calculateEndDate(startDate: String, durationDays: Int): String? {
        val start = parseDate(startDate) ?: return null
        if (durationDays <= 0) return null
        return start.plusDays((durationDays - 1).toLong()).format(DATE_FORMATTER)
    }

    // 目标日期是否落在计划窗口内（含两端）；任一端解析失败都返回 false，按脏数据保守跳过
    fun isWithinPlanWindow(startDate: String, endDate: String, date: String): Boolean {
        val start = parseDate(startDate) ?: return false
        val end = parseDate(endDate) ?: return false
        val target = parseDate(date) ?: return false
        return !target.isBefore(start) && !target.isAfter(end)
    }

    // 解析 yyyy-MM-dd，格式不对返回 null
    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value.trim(), DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
