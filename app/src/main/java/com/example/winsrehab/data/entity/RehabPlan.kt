package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 康复计划实体类
 * 
 * 一个患者可以有多个康复计划（如上肢康复计划、下肢康复计划）
 * 状态流转：draft（草稿）→ active（激活）→ completed（完成）/ cancelled（取消）
 */
@Entity(
    tableName = "rehab_plan",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["status"])
    ]
)
data class RehabPlan(
    @PrimaryKey(autoGenerate = true)
    val planId: Long = 0,                                  // 计划ID（自动生成）
    
    val patientId: String,                                 // 患者ID（外键）
    val doctorCode: String,                                // 制定医生工号（外键）
    
    val planName: String,                                  // 计划名称（如"上肢功能康复计划"）
    val goal: String,                                      // 康复目标（如"恢复手臂抬举功能"）
    
    val startDate: String,                                 // 开始日期（yyyy-MM-dd）
    val endDate: String,                                   // 结束日期（yyyy-MM-dd）
    
    val status: String = "draft",                          // 状态：draft/active/completed/cancelled
    val progress: Int = 0,                                 // 完成进度（0-100）
    
    val createdAt: Long = System.currentTimeMillis(),      // 创建时间戳
    val updatedAt: Long = System.currentTimeMillis()       // 更新时间戳
)

