package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor")
data class Doctor(
    @PrimaryKey val doctorCode: String,                    // 医生工号（唯一标识）
    val password: String,                                  // 密码
    val name: String = "未设置",                            // 姓名
    val gender: String = "未设置",                          // 性别
    val age: Int = 0,                                      // 年龄

    val title: String = "未设置",                           // 职称（主治医师、副主任医师等）
    val department: String = "未设置",                      // 科室
    val hospital: String = "未设置",                        // 所属医院
    val licenseNumber: String = "未设置",                   // 医师资格证号
    val phone: String = "未设置",                           // 手机号码
    val email: String = "未设置",                           // 电子邮箱
    val avatar: String? = null,                            // 头像URL或路径
    val patientCount: Int = 0,                             // 管理患者数（冗余字段）
    val monthlyCompletedPlans: Int = 0,                    // 本月完成计划数
    val satisfactionRate: Float = 0f,                      // 患者满意度（0-100）
    val createdAt: Long = System.currentTimeMillis(),      // 创建时间戳
    val updatedAt: Long = System.currentTimeMillis()       // 更新时间戳
)
