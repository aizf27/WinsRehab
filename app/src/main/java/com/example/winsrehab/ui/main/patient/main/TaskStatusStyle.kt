package com.example.winsrehab.ui.main.patient.main

import com.example.winsrehab.data.model.TaskStatus

// 任务状态 -> 视觉类型；这是状态视觉的唯一归属，RehabFormatter 不重复实现
enum class TaskVisual { PENDING, COMPLETED, SKIPPED, WAITING_REVIEW, REJECTED, UNKNOWN }

object TaskStatusStyle {

    fun visualOf(status: String): TaskVisual = when (status) {
        TaskStatus.PENDING -> TaskVisual.PENDING
        TaskStatus.COMPLETED -> TaskVisual.COMPLETED
        TaskStatus.SKIPPED -> TaskVisual.SKIPPED
        TaskStatus.WAITING_REVIEW -> TaskVisual.WAITING_REVIEW
        TaskStatus.REJECTED -> TaskVisual.REJECTED
        else -> TaskVisual.UNKNOWN
    }

    // 只有待完成任务可执行；未知状态保守视为不可操作
    fun isActionable(status: String): Boolean = status == TaskStatus.PENDING

    fun labelOf(status: String): String = when (visualOf(status)) {
        TaskVisual.PENDING -> "待完成"
        TaskVisual.COMPLETED -> "已完成"
        TaskVisual.SKIPPED -> "已跳过"
        TaskVisual.WAITING_REVIEW -> "待审核"
        TaskVisual.REJECTED -> "已驳回"
        TaskVisual.UNKNOWN -> "未知"
    }
}
