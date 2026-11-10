package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 患者对康复计划的轻量反馈：本月只做"患者留一条、医生回一条"，不做聊天
// 状态：pending（已提交待回复）→ replied（医生已回复）
@Entity(
    tableName = "feedback",
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["planId"]),
        Index(value = ["status"])
    ]
)
data class Feedback(
    @PrimaryKey(autoGenerate = true)
    val feedbackId: Long = 0,                          // 反馈ID（自动生成）

    val patientId: String,                             // 留言的患者

    val planId: Long,                                  // 针对哪个康复计划

    val doctorCode: String = "",                       // 计划所属医生，避免患者换医生后留言串台

    val content: String,                               // 患者反馈内容

    val doctorReply: String? = null,                   // 医生回复；未回复为空

    val status: String = "pending",                    // 状态：pending/replied（Week 3 接入时再补常量与 DAO）

    val createdAt: Long = System.currentTimeMillis(),  // 患者提交时间

    val repliedAt: Long? = null                        // 医生回复时间
)
