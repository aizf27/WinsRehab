package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor_task")
data class DoctorTask(
    @PrimaryKey(autoGenerate = true) val taskId: Long = 0,
    val doctorCode: String,           // 医生工号
    val taskType: String,             // 任务类型："patient_binding"
    val patientId: String,            // 关联患者ID
    val patientName: String,          // 患者姓名
    val patientAccount: String,       // 患者账号
    val status: String = "pending",   // 状态："pending"/"completed"
    val createdAt: Long               // 创建时间
)



