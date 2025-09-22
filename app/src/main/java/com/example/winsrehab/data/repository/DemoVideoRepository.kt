package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.DemoVideoDao
import com.example.winsrehab.data.entity.DemoVideo
import kotlinx.coroutines.flow.Flow

class DemoVideoRepository(private val demoVideoDao: DemoVideoDao) {
    fun getAllDemoVideos(): Flow<List<DemoVideo>> = demoVideoDao.getAllDemoVideos()
    suspend fun insertDemoVideo(video: DemoVideo) = demoVideoDao.insertDemoVideo(video)
}