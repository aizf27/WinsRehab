package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 计划调整记录实体类
 * 
 * 记录医生对康复计划的每次调整
 * 用于追溯计划变更历史，便于分析康复效果
 * 
 * 调整类型说明：
 * - add_task: 添加新任务
 * - modify_task: 修改现有任务
 * - delete_task: 删除任务
 * - change_period: 修改计划周期
 * 
 * adjustmentDetail存储JSON格式的详细信息，例如：
 * - 添加任务：{"taskName": "手指灵活性训练", "frequency": "daily"}
 * - 修改任务：{"taskId": 123, "oldDuration": 30, "newDuration": 20}
 * - 删除任务：{"taskId": 123, "taskName": "肩关节训练"}
 * - 修改周期：{"oldEndDate": "2024-03-01", "newEndDate": "2024-03-15"}
 */
@Entity(
    tableName = "plan_adjustment",
    indices = [
        Index(value = ["planId"])
    ]
)
data class PlanAdjustment(
    @PrimaryKey(autoGenerate = true)
    val adjustmentId: Long = 0,                            // 调整ID（自动生成）
    
    val planId: Long,                                      // 计划ID（外键）
    val doctorCode: String,                                // 调整医生工号
    
    val adjustmentType: String,                            // 调整类型：add_task/modify_task/delete_task/change_period
    val adjustmentDetail: String,                          // 调整详情（JSON格式）
    
    val reason: String = "",                               // 调整原因（可选）
    
    val createdAt: Long = System.currentTimeMillis()       // 调整时间戳
)

