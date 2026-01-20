package com.example.winsrehab.data.repository

import android.util.Log

import com.example.winsrehab.data.dao.PatientDao
import com.example.winsrehab.data.entity.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
//先绑Dao
class PatientRepository(private val patientDao: PatientDao) {
    suspend fun login(account: String, password: String): Boolean {
        return patientDao.login(account, password)!= null
    }
    suspend fun isExist(id: String): Boolean {
        return patientDao.isExist(id)
    }
    suspend fun registerPatient(patient: Patient): Boolean {
        return try {
            patientDao.insertPatient(patient)
            Log.i("PatientRepository", "Register patient success")
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun insertPatient(patient: Patient) {
        patientDao.insertPatient(patient)
    }
    suspend fun updatePatient(patient: Patient) {
        patientDao.updatePatient(patient)
    }
    suspend fun getPatientByAccount(account: String): Patient? {
        return patientDao.getPatientByAccount(account).first()
    }
    suspend fun isInfoComplete(account: String): Boolean {
        val patient = patientDao.getPatientByAccount(account).first()
        return patient?.let {
            !it.name.isNullOrEmpty() && (it.age ?: 0) > 0
        } ?: false
    }
    fun getPatientsByDoctor(doctorCode: String): Flow<List<Patient>> {
        return patientDao.getPatientsByDoctorFlow(doctorCode)
    }
    
    suspend fun getBindingStatus(account: String): String? {
        val patient = patientDao.getPatientByAccount(account).first()
        return patient?.bindingStatus
    }

}