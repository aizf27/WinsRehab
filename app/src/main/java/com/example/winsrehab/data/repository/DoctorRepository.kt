package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.DoctorDao
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.flow.first

class DoctorRepository(private val doctorDao: DoctorDao) {

    suspend fun login(id: String, password: String): Boolean {
        return doctorDao.login(id, password) != null
    }

    suspend fun getDoctorInfo(doctorCode: String): Doctor? {
        return doctorDao.getDoctorById(doctorCode).first()
    }



    suspend fun isExist(id: String): Boolean {
        return doctorDao.isExist(id)
    }

    suspend fun registerDoctor(doctor: Doctor): Boolean {
        return try {
            doctorDao.insertDoctor(doctor)
            true
        } catch (e: Exception) {
            false
        }
    }

   //更新
   suspend fun updateDoctorInfo(doctor: Doctor) {
       // 先查询是否存在该医生信息
       val existingDoctor = doctorDao.getDoctorInfo(doctor.id)
       if (existingDoctor == null) {
           // 如果不存在，执行插入操作
           doctorDao.insertDoctor(doctor)
       } else {
           // 如果存在，执行更新操作
           doctorDao.updateDoctor(doctor)
       }
   }

    suspend fun updatePatientCount(id: String, count: Int) {
        doctorDao.updatePatientCount(id, count)
    }

    suspend fun isInfoComplete(id: String): Boolean {
        val doctor = doctorDao.getDoctorById(id).first()
        return doctor?.let {
            !it.name.isNullOrEmpty() && (it.age ?: 0) > 0
        } ?: false
    }
}
