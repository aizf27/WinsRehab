package com.example.winsrehab.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "demo_video")
data class DemoVideo(
    @PrimaryKey val id: Int,
    val title: String,
    val videoPath: String,    //本地路径
    val description: String? = null,
    val category: String
)