package com.example.winsrehab.utils

import com.example.winsrehab.data.entity.TrainingTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// 任务生成规则：纯函数，只依赖 (任务模板, 计划开始日期, 目标日期)
// 不读数据库、不打日志、不引用"上次生成日期"，所以能直接在 JVM 单测里验证，患者漏几天也不会让 interval 节奏漂移
object TaskSchedule {

    const val FREQUENCY_DAILY = "daily"        // 每天执行
    const val FREQUENCY_WEEKLY = "weekly"      // 每周固定几天
    const val FREQUENCY_INTERVAL = "interval"  // 每隔 N 天

    // 全项目统一的日期格式，和数据库里的字符串保持一致
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 判断任务在 date 这天是否该生成（LocalDate 版本，规则本体）
    fun shouldGenerate(task: TrainingTask, planStartDate: LocalDate, date: LocalDate): Boolean {
        return shouldGenerate(task.frequency, task.frequencyDetail, planStartDate, date)
    }

    // 判断任务在 date 这天是否该生成（字符串版本，数据库字段直接传进来）
    fun shouldGenerate(task: TrainingTask, planStartDate: String, date: String): Boolean {
        val start = parseDate(planStartDate) ?: return false
        val target = parseDate(date) ?: return false
        return shouldGenerate(task, start, target)
    }

    // 规则本体：计划开始前一律不生成，之后按频率判断
    fun shouldGenerate(
        frequency: String,
        frequencyDetail: String,
        planStartDate: LocalDate,
        date: LocalDate
    ): Boolean {
        // 计划还没开始就不该有任务，漏了也不补生成
        if (date.isBefore(planStartDate)) return false

        return when (frequency.trim().lowercase()) {
            // daily：计划窗口内每天都生成
            FREQUENCY_DAILY -> true

            // weekly：frequencyDetail 存 "1,3,5"，1=周一、7=周日
            FREQUENCY_WEEKLY -> date.dayOfWeek.value in parseWeekDays(frequencyDetail)

            // interval：从计划开始日算起，隔 N 天生成一次；不参考上次生成日期
            FREQUENCY_INTERVAL -> {
                val interval = frequencyDetail.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
                ChronoUnit.DAYS.between(planStartDate, date) % interval == 0L
            }

            // 频率类型不认识就不生成，避免脏数据生成一堆任务
            else -> false
        }
    }

    // 解析 weekly 的 "1,3,5"；非法值和越界值直接丢掉
    fun parseWeekDays(frequencyDetail: String): Set<Int> {
        return frequencyDetail.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    // 把 yyyy-MM-dd 解析成 LocalDate，格式不对返回 null，调用方决定怎么处理
    fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value.trim(), DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
