package com.example.winsrehab.utils

import android.util.Log
import com.example.winsrehab.data.entity.TrainingTask
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务生成辅助工具类
 *
 * 提供任务生成相关的日期计算和频率判断功能
 */
object TaskGenerationHelper {

    private const val TAG = "TaskGenerationHelper"
    private const val DATE_FORMAT = "yyyy-MM-dd"

    /**
     * 判断任务今天是否应该生成
     *
     * @param task 任务模板
     * @param date 目标日期（yyyy-MM-dd格式）
     * @param lastScheduledDate 上次生成日期（用于interval频率，可为null）
     * @return true表示应该生成，false表示不生成
     */
    @Deprecated("频率判断改用 TaskSchedule.shouldGenerate：只依赖计划和日期，不再依赖数据库历史")
    fun shouldGenerateToday(
        task: TrainingTask,
        date: String,
        lastScheduledDate: String? = null
    ): Boolean {
        return when (task.frequency) {
            "daily" -> {
                // 每天都生成
                true
            }
            "weekly" -> {
                // 每周特定天生成
                shouldGenerateWeekly(date, task.frequencyDetail)
            }
            "interval" -> {
                // 间隔天数生成
                shouldGenerateByInterval(lastScheduledDate, date, task.frequencyDetail)
            }
            else -> {
                Log.w(TAG, "未知的频率类型: ${task.frequency}, 任务ID: ${task.taskId}")
                false
            }
        }
    }

    /**
     * 判断weekly频率任务是否应该生成
     *
     * @param date 目标日期
     * @param frequencyDetail 频率详情，如"1,3,5"表示周一三五
     * @return true表示今天是指定的星期几
     */
    private fun shouldGenerateWeekly(date: String, frequencyDetail: String): Boolean {
        try {
            // 解析frequencyDetail获取星期几列表
            val weekDays = parseWeeklyDays(frequencyDetail)
            if (weekDays.isEmpty()) {
                Log.w(TAG, "weekly频率但frequencyDetail为空或格式错误: $frequencyDetail")
                return false
            }

            // 获取目标日期是星期几
            val dayOfWeek = getDayOfWeek(date)

            // 判断是否在列表中
            return weekDays.contains(dayOfWeek)
        } catch (e: Exception) {
            Log.e(TAG, "判断weekly频率失败: date=$date, detail=$frequencyDetail", e)
            return false
        }
    }

    /**
     * 判断interval频率任务是否应该生成
     *
     * @param lastDate 上次生成日期
     * @param targetDate 目标日期
     * @param frequencyDetail 间隔天数，如"2"表示间隔2天
     * @return true表示满足间隔条件
     */
    private fun shouldGenerateByInterval(
        lastDate: String?,
        targetDate: String,
        frequencyDetail: String
    ): Boolean {
        try {
            // 如果没有上次生成日期，说明是首次生成，直接生成
            if (lastDate == null) {
                Log.i(TAG, "interval任务首次生成")
                return true
            }

            // 解析间隔天数
            val interval = frequencyDetail.toIntOrNull()
            if (interval == null || interval <= 0) {
                Log.w(TAG, "interval频率但frequencyDetail格式错误: $frequencyDetail")
                return false
            }

            // 计算日期差
            val daysDiff = calculateDaysDiff(lastDate, targetDate)
            if (daysDiff < 0) {
                Log.w(TAG, "计算日期差失败，跳过interval任务生成")
                return false
            }

            // 判断是否满足间隔
            val shouldGenerate = daysDiff >= interval
            Log.i(TAG, "interval判断: lastDate=$lastDate, targetDate=$targetDate, interval=$interval, daysDiff=$daysDiff, result=$shouldGenerate")

            return shouldGenerate
        } catch (e: Exception) {
            Log.e(TAG, "判断interval频率失败: lastDate=$lastDate, targetDate=$targetDate, detail=$frequencyDetail", e)
            return false
        }
    }

    /**
     * 获取日期是星期几
     *
     * @param date 日期字符串（yyyy-MM-dd格式）
     * @return 1-7表示周一到周日，解析失败返回0
     */
    fun getDayOfWeek(date: String): Int {
        return try {
            if (date.isBlank()) {
                Log.w(TAG, "日期字符串为空")
                return 0
            }

            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            dateFormat.isLenient = false  // 严格模式，不允许非法日期
            val parsedDate = dateFormat.parse(date)

            if (parsedDate == null) {
                Log.w(TAG, "日期解析失败: $date")
                return 0
            }

            val calendar = Calendar.getInstance()
            calendar.time = parsedDate

            // Calendar.DAY_OF_WEEK返回1-7，但1是周日
            // 转换为1-7表示周一到周日
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
        } catch (e: Exception) {
            Log.e(TAG, "获取星期几失败: date=$date", e)
            0
        }
    }

    /**
     * 解析weekly的频率详情
     *
     * @param frequencyDetail 如"1,3,5"表示周一三五
     * @return 星期几的列表，如[1,3,5]
     */
    fun parseWeeklyDays(frequencyDetail: String): List<Int> {
        if (frequencyDetail.isBlank()) {
            return emptyList()
        }

        return try {
            frequencyDetail.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 1..7 }
        } catch (e: Exception) {
            Log.e(TAG, "解析weekly频率详情失败: $frequencyDetail", e)
            emptyList()
        }
    }

    /**
     * 计算两个日期之间的天数差
     *
     * @param fromDate 起始日期（yyyy-MM-dd）
     * @param toDate 结束日期（yyyy-MM-dd）
     * @return 天数差（正数），解析失败返回-1
     */
    private fun calculateDaysDiff(fromDate: String, toDate: String): Int {
        return try {
            if (fromDate.isBlank() || toDate.isBlank()) {
                Log.w(TAG, "日期字符串为空: from=$fromDate, to=$toDate")
                return -1
            }

            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            dateFormat.isLenient = false

            val fromTime = dateFormat.parse(fromDate)?.time
            val toTime = dateFormat.parse(toDate)?.time

            if (fromTime == null || toTime == null) {
                Log.w(TAG, "日期解析失败: from=$fromDate, to=$toDate")
                return -1
            }

            // 转换为天数（向下取整）
            ((toTime - fromTime) / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "计算日期差失败: from=$fromDate, to=$toDate", e)
            -1
        }
    }

    /**
     * 将时间戳格式化为日期字符串
     *
     * @param timeMillis 时间戳
     * @return yyyy-MM-dd格式的日期字符串
     */
    fun formatDate(timeMillis: Long): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(Date(timeMillis))
    }

    /**
     * 将日期字符串解析为时间戳
     *
     * @param dateStr yyyy-MM-dd格式的日期字符串
     * @return 时间戳
     */
    fun parseDate(dateStr: String): Long {
        return try {
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "解析日期失败: $dateStr", e)
            0L
        }
    }

    /**
     * 获取当前日期字符串
     *
     * @return yyyy-MM-dd格式的今天日期
     */
    fun getTodayDate(): String {
        return formatDate(System.currentTimeMillis())
    }
}
