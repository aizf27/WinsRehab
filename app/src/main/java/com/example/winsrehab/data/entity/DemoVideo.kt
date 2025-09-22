package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "demo_video")
data class DemoVideo(
    @PrimaryKey(autoGenerate = true)
    val demoId: Long = 0,           // 主键
    val title: String="",              // 标题
    val videoUrl: String="",           // 视频URL/本地路径
    val description: String? = null,// 文字说明
    val uploadDate: String=""          // 上传时间

)