package com.example.winsrehab.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {

    @Query("SELECT * FROM doctor WHERE doctorCode = :doctorCode")
    fun getDoctorById(doctorCode: String): Flow<Doctor?>

    @Query("SELECT * FROM doctor WHERE doctorCode = :doctorCode AND password = :password")
    suspend fun login(doctorCode: String, password: String): Doctor?

    @Query("SELECT EXISTS(SELECT 1 FROM doctor WHERE doctorCode = :doctorCode)")
    suspend fun isExist(doctorCode: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDoctor(doctor: Doctor)

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Query("UPDATE doctor SET patientCount = :count WHERE doctorCode = :doctorCode")
    suspend fun updatePatientCount(doctorCode: String, count: Int)

    @Query("SELECT * FROM doctor WHERE doctorCode = :doctorCode LIMIT 1")
    fun getDoctorInfo(doctorCode: String): LiveData<Doctor?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorInfo(doctor: Doctor)

}
