package com.example.winsrehab.data.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.Patient
import kotlinx.coroutines.flow.Flow

interface PatientDao {
    @Query("SELECT * FROM patient WHERE account = :account")
    fun getPatientByAccount(account: String): Flow<Patient?>

    @Query("SELECT * FROM patient WHERE account = :account AND password = :password")
    suspend fun login(account: String, password: String): Patient?

    @Query("SELECT EXISTS(SELECT 1 FROM patient WHERE id = :id)")
    suspend fun isExist(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPatient(patient: Patient)

    @Update
    suspend fun updatePatient(patient: Patient)


}