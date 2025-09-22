package com.example.winsrehab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.winsrehab.data.entity.DemoVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface DemoVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDemoVideo(video: DemoVideo)

    @Query("SELECT * FROM demo_video ORDER BY uploadDate DESC")
    fun getAllDemoVideos(): Flow<List<DemoVideo>>
}