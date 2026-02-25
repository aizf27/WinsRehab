package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 训练任务模板实体类
 * 
 * 这是任务模板，不是具体的任务实例
 * 系统会根据频率规则生成具体的任务实例（TaskInstance）
 * 
 * 频率类型说明：
 * - daily: 每天执行
 * - weekly: 每周特定天执行（frequencyDetail存储星期几，如"1,3,5"表示周一三五）
 * - interval: 间隔天数执行（frequencyDetail存储间隔天数，如"2"表示每隔2天）
 */
@Entity(
    tableName = "training_task",
    indices = [
        Index(value = ["planId"])
    ]
)
data class TrainingTask(
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0,                                  // 任务ID（自动生成）
    
    val planId: Long,                                      // 所属计划ID（外键）
    
    val taskName: String,                                  // 任务名称（如"肩关节外展训练"）
    val taskType: String,                                  // 任务类型：upper_limb/lower_limb/balance/coordination/full_body
    val description: String = "",                          // 任务描述
    
    val suggestedDuration: Int,                            // 建议时长（分钟）
    
    val frequency: String,                                 // 执行频率：daily/weekly/interval
    val frequencyDetail: String = "",                      // 频率详情（如"1,3,5"或"2"）
    
    val priority: String = "normal",                       // 优先级：normal/important
    val referenceVideoId: Long? = null,                    // 参考视频ID（关联DemoVideo）
    
    val createdAt: Long = System.currentTimeMillis()       // 创建时间戳
)

