package com.example.winsrehab.ui.main.patient.main

import com.example.winsrehab.data.model.TaskInstanceWithTask

// 本周柱状图单个自然日的数据（日期 + 完成/总数 + 是否今天）
data class WeekBarData(
    val date: String,
    val completed: Int,
    val total: Int,
    val isToday: Boolean
)

// 康复页 UI 状态
sealed class RehabUiState {
    object Loading : RehabUiState()
    data class Content(
        val todayTasks: List<TaskInstanceWithTask>,
        val todayCompleted: Int,
        val todayTotal: Int,
        val weekBars: List<WeekBarData>,
        val weekCompleted: Int,
        val weekTotal: Int
    ) : RehabUiState()
    data class Error(val message: String) : RehabUiState()
}
