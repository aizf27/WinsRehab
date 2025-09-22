package com.example.winsrehab.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.winsrehab.data.entity.PtRehabVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface PtRehabVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: PtRehabVideo)

    @Update
    suspend fun updateVideo(video: PtRehabVideo)

    @Delete
    suspend fun deleteVideo(video: PtRehabVideo)

    // 查询某个患者的全部视频
    @Query("SELECT * FROM ptrehab_video WHERE patientId = :patientId ORDER BY uploadDate DESC")
    fun getVideosByPatient(patientId: String): Flow<List<PtRehabVideo>>

    // 查询单个视频
    @Query("SELECT * FROM ptrehab_video WHERE videoId = :videoId")
    suspend fun getVideoById(videoId: Long): PtRehabVideo?
}