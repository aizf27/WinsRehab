package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient")
data class Patient(
    @PrimaryKey val patientId: String,                     // 患者ID（唯一标识）
    val account: String,                                   // 登录账号（手机号）
    val password: String,                                  // 密码
    val name: String = "未设置",                            // 姓名
    val gender: String = "未设置",                          // 性别
    val age: Int = 0,                                      // 年龄
    val phone: String = "未设置",                           // 手机号码
    val email: String = "未设置",                           // 电子邮箱
    val dateOfBirth: String = "未设置",                     // 出生日期（yyyy-MM-dd）
    val address: String = "未设置",                         // 地址
    val avatar: String? = null,                            // 头像URL或路径
    val nickname: String = "未设置",                        // 昵称（用于社区显示）

    val doctorCode: String = "未设置",                      // 主治医生工号（外键）
    val doctorName: String = "未设置",                      // 主治医生姓名
    val diagnosis: String = "未设置",                       // 诊断/病情描述
    val rehabStage: String = "未设置",                      // 康复阶段（软瘫期/痉挛期/恢复期）
    val rehabCenter: String = "未设置",                     // 康复中心
    val rehabStartDate: String = "未设置",                  // 康复开始日期（yyyy-MM-dd）
    val rehabDays: Int = 0,                                // 累计康复天数（冗余字段）
    val overallProgress: Int = 0,                          // 总体康复进度（0-100）

    val weeklyCompletionRate: Float = 0f,                  // 本周完成率（0-100，冗余字段）
    val todayTasksCompleted: Int = 0,                      // 今日已完成任务数（冗余字段）
    val todayTasksTotal: Int = 0,                          // 今日总任务数（冗余字段）
    val signature: String = "这个人很懒，什么都没留下",        // 个性签名
    val hasAlert: Boolean = false,                         // 是否有异常预警
    val hasNewFeedback: Boolean = false,                   // 是否有新反馈
    val lastActiveTime: Long = 0L,                         // 最后活跃时间戳
    val bindingStatus: String = "unbound",                 // 绑定状态（unbound/pending/active/rejected）
    val joinRanking: Boolean = true,                       // 是否参与排行榜
    val createdAt: Long = 0L,                              // 创建时间戳
    val updatedAt: Long = 0L                               // 更新时间戳
)