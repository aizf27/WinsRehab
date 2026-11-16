package com.example.winsrehab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patient WHERE account = :account")
    fun getPatientByAccount(account: String): Flow<Patient?>

    @Query("SELECT * FROM patient WHERE account = :account AND password = :password")
    suspend fun login(account: String, password: String): Patient?

    @Query("SELECT EXISTS(SELECT 1 FROM patient WHERE patientId = :patientId)")
    suspend fun isExist(patientId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Update
    suspend fun updatePatient(patient: Patient)

    @Query("SELECT * FROM patient WHERE doctorCode = :doctorCode")
    fun getPatientsByDoctorFlow(doctorCode: String): Flow<List<Patient>>

    @Query("SELECT * FROM patient WHERE patientId = :patientId")
    suspend fun getPatientById(patientId: String): Patient?

    // 查出「绑在占位医生上」的患者：工号为空、未设置或旧版本占位工号，但绑定状态不是未绑定
    @Query(
        """
        SELECT * FROM patient
        WHERE bindingStatus != :unboundStatus
          AND (
            doctorCode IS NULL
            OR TRIM(doctorCode) = ''
            OR doctorCode = :unsetValue
            OR doctorCode = :placeholderCode
          )
        """
    )
    suspend fun getPatientsBoundToPlaceholder(
        unboundStatus: String,
        unsetValue: String,
        placeholderCode: String
    ): List<Patient>

    // 把单个患者的绑定关系复位为未绑定，让他重新走一遍绑定申请
    @Query(
        """
        UPDATE patient
        SET doctorCode = :unsetValue, doctorName = :unsetValue, bindingStatus = :unboundStatus
        WHERE patientId = :patientId
        """
    )
    suspend fun resetBinding(patientId: String, unsetValue: String, unboundStatus: String)

}