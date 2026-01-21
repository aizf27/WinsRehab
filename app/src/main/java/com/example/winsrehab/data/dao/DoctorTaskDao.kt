package com.example.winsrehab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.winsrehab.data.entity.DoctorTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorTaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DoctorTask)
    
    @Query("SELECT * FROM doctor_task WHERE doctorCode = :doctorCode AND taskType = :taskType AND status = 'pending'")
    fun getPendingTasksByDoctor(doctorCode: String, taskType: String): Flow<List<DoctorTask>>
    
    @Query("SELECT COUNT(*) FROM doctor_task WHERE doctorCode = :doctorCode AND taskType = :taskType AND status = 'pending'")
    fun getPendingTaskCount(doctorCode: String, taskType: String): Flow<Int>
    
    @Query("DELETE FROM doctor_task WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: Long)
    
    @Query("DELETE FROM doctor_task WHERE patientId = :patientId AND taskType = 'patient_binding'")
    suspend fun deleteBindingTaskByPatient(patientId: String)
}



