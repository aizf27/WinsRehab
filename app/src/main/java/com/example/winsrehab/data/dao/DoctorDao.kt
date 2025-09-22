package com.example.winsrehab.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {

    @Query("SELECT * FROM doctor WHERE id = :id")
    fun getDoctorById(id: String): Flow<Doctor?>

    @Query("SELECT * FROM doctor WHERE id = :id AND password = :password")
    suspend fun login(id: String, password: String): Doctor?

    @Query("SELECT EXISTS(SELECT 1 FROM doctor WHERE id = :id)")
    suspend fun isExist(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDoctor(doctor: Doctor)

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Query("UPDATE doctor SET patientCount = :count WHERE id = :id")
    suspend fun updatePatientCount(id: String, count: Int)

    @Query("SELECT * FROM doctor WHERE id = :doctorCode LIMIT 1")
    fun getDoctorInfo(doctorCode: String): LiveData<Doctor?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorInfo(doctor: Doctor)

}
