package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.PtRehabVideoDao
import com.example.winsrehab.data.entity.PtRehabVideo
import kotlinx.coroutines.flow.Flow

class PtRehabVideoRepository(private val PtRehabVideoDao: PtRehabVideoDao) {
    suspend fun insertVideo(video: PtRehabVideo) = PtRehabVideoDao.insertVideo(video)

    fun getVideosByPatient(patientId: String): Flow<List<PtRehabVideo>> =
        PtRehabVideoDao.getVideosByPatient(patientId)

    suspend fun getVideoById(videoId: Long): PtRehabVideo? =
        PtRehabVideoDao.getVideoById(videoId)

    suspend fun deleteVideo(video: PtRehabVideo) = PtRehabVideoDao.deleteVideo(video)
}