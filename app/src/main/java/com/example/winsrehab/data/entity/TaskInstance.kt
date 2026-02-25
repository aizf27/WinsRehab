package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 任务实例实体类
 * 
 * 这是具体的任务实例，由系统根据任务模板（TrainingTask）生成
 * 患者完成的是任务实例，不是任务模板
 * 
 * 状态流转：
 * - pending（待完成）→ completed（已完成）/ skipped（跳过）
 * - 完成后如果上传视频，会关联videoId，状态变为waiting_review（等待审核）
 */
@Entity(
    tableName = "task_instance",
    indices = [
        Index(value = ["patientId", "scheduledDate"]),
        Index(value = ["taskId"]),
        Index(value = ["status"])
    ]
)
data class TaskInstance(
    @PrimaryKey(autoGenerate = true)
    val instanceId: Long = 0,                              // 实例ID（自动生成）
    
    val taskId: Long,                                      // 任务模板ID（外键）
    val patientId: String,                                 // 患者ID（外键）
    
    val scheduledDate: String,                             // 计划日期（yyyy-MM-dd）
    val scheduledTime: String = "",                        // 计划时间（HH:mm，可选）
    
    val status: String = "pending",                        // 状态：pending/completed/skipped/waiting_review
    
    val completedAt: Long? = null,                         // 完成时间戳
    val actualDuration: Int? = null,                       // 实际时长（分钟）
    
    val videoId: Long? = null,                             // 关联视频ID（外键，完成后上传视频）
    
    val createdAt: Long = System.currentTimeMillis()       // 创建时间戳
)

