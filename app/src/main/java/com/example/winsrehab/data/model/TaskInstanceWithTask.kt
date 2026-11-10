package com.example.winsrehab.data.model

import androidx.room.Embedded
import com.example.winsrehab.data.entity.TaskInstance

// 今日/本周任务查询模型：实例 + 展示所需的任务模板字段，只读投影，不落库
data class TaskInstanceWithTask(
    @Embedded val instance: TaskInstance,
    val taskName: String,
    val suggestedDuration: Int,
    val priority: String
)
