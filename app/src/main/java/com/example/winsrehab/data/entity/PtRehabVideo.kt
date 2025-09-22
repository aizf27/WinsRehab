package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "ptrehab_video")
data class PtRehabVideo(
    @PrimaryKey(autoGenerate = true)
    val videoId: Long = 0,         // 主键，自增
    val patientId: String="",         // 对应 Patient.id
    val videoUrl: String="",          // 视频路径/URL
    val uploadDate: String="",        // 上传日期(yyyy-MM-dd HH:mm)
    val description: String? = null// 备注/说明
)
