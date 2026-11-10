package com.example.winsrehab.data.model

// 康复页一周数据：把「今天、本周区间、本周任务」一次性带出，保证四处同源
data class RehabWeekData(
    val today: String,
    val weekStart: String,
    val weekEnd: String,
    val tasks: List<TaskInstanceWithTask>
)
