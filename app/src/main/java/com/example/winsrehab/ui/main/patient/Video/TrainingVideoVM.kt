package com.example.winsrehab.ui.main.patient.Video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.winsrehab.data.entity.DemoVideo

class TrainingVideoViewModel : ViewModel() {

    private val categoryLive = MutableLiveData<String>("全部")

    // 本地静态视频列表
    private val allVideos = listOf(
        DemoVideo(
            id = 1,
            title = "肩关节拉伸",
            videoPath = "file:///android_asset/video/shoulder_stretch.mp4",
            description = "适合术后康复",
            category = "肩部"
        ),
        DemoVideo(
            id = 2,
            title = "手臂伸展",
            videoPath = "file:///android_asset/video/arm_extension.mp4",
            description = "增强上肢力量",
            category = "肩部"
        ),
        DemoVideo(
            id = 3,
            title = "腿部拉伸",
            videoPath = "file:///android_asset/video/leg_stretch.mp4",
            description = "缓解下肢紧张",
            category = "腿部"
        ),
        DemoVideo(
            id = 4,
            title = "全身热身操",
            videoPath = "file:///android_asset/video/warmup.mp4",
            description = "运动前准备",
            category = "全身"
        )
    )

    // 根据分类筛选
    val videos: LiveData<List<DemoVideo>> = categoryLive.map { cat ->
        if (cat == "全部") allVideos else allVideos.filter { it.category == cat }
    }

    fun setCategory(category: String) {
        categoryLive.value = category
    }
}