package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.DoctorDao
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.flow.first

class DoctorRepository(private val doctorDao: DoctorDao) {

    suspend fun login(doctorCode: String, password: String): Boolean {
        return doctorDao.login(doctorCode, password) != null
    }

    suspend fun getDoctorInfo(doctorCode: String): Doctor? {
        return doctorDao.getDoctorById(doctorCode).first()
    }

    suspend fun isExist(doctorCode: String): Boolean {
        return doctorDao.isExist(doctorCode)
    }

    suspend fun registerDoctor(doctor: Doctor): Boolean {
        return try {
            doctorDao.insertDoctor(doctor)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 更新医生信息
    suspend fun updateDoctorInfo(doctor: Doctor) {
        // 先查询是否存在该医生信息
        val existingDoctor = doctorDao.getDoctorInfo(doctor.doctorCode).value
        if (existingDoctor == null) {
            // 如果不存在，执行插入操作
            doctorDao.insertDoctor(doctor)
        } else {
            // 如果存在，执行更新操作
            doctorDao.updateDoctor(doctor)
        }
    }

    suspend fun updatePatientCount(doctorCode: String, count: Int) {
        doctorDao.updatePatientCount(doctorCode, count)
    }

    suspend fun isInfoComplete(doctorCode: String): Boolean {
        val doctor = doctorDao.getDoctorById(doctorCode).first()
        return doctor?.let {
            it.name != "未设置" && it.age > 0
        } ?: false
    }
}
