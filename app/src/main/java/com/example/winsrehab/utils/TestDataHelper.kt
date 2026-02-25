package com.example.winsrehab.utils

import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 测试数据帮助类
 * 用于初始化测试数据
 */
object TestDataHelper {
    
    /**
     * 初始化测试医生数据
     * 确保数据库中有工号为 "1" 的医生
     */
    fun initTestDoctors() {
        CoroutineScope(Dispatchers.IO).launch {
            val doctorDao = MyApp.instance.database.doctorDao()
            
            // 检查工号为 "1" 的医生是否存在
            val existingDoctor = doctorDao.getDoctorByCode("1")
            
            if (existingDoctor == null) {
                // 创建测试医生
                val testDoctor = Doctor(
                    doctorCode = "1",
                    password = "123456",
                    name = "张医生",
                    gender = "男",
                    age = 35,
                    title = "主治医师",
                    department = "康复科",
                    hospital = "市人民医院",
                    phone = "13800138001",
                    email = "doctor1@hospital.com",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                try {
                    doctorDao.insertDoctorInfo(testDoctor)
                    android.util.Log.d("TestDataHelper", "成功创建测试医生: 工号=1, 姓名=张医生")
                } catch (e: Exception) {
                    android.util.Log.e("TestDataHelper", "创建测试医生失败: ${e.message}")
                }
            } else {
                android.util.Log.d("TestDataHelper", "医生已存在: 工号=${existingDoctor.doctorCode}, 姓名=${existingDoctor.name}")
            }
        }
    }
    
    /**
     * 创建多个测试医生
     */
    fun initMultipleTestDoctors() {
        CoroutineScope(Dispatchers.IO).launch {
            val doctorDao = MyApp.instance.database.doctorDao()
            
            val testDoctors = listOf(
                Doctor(
                    doctorCode = "1",
                    password = "123456",
                    name = "张医生",
                    gender = "男",
                    age = 35,
                    title = "主治医师",
                    department = "康复科",
                    hospital = "市人民医院",
                    phone = "13800138001",
                    email = "doctor1@hospital.com",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                Doctor(
                    doctorCode = "2",
                    password = "123456",
                    name = "李医生",
                    gender = "女",
                    age = 32,
                    title = "副主任医师",
                    department = "康复科",
                    hospital = "市人民医院",
                    phone = "13800138002",
                    email = "doctor2@hospital.com",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                Doctor(
                    doctorCode = "3",
                    password = "123456",
                    name = "王医生",
                    gender = "男",
                    age = 40,
                    title = "主任医师",
                    department = "康复科",
                    hospital = "市人民医院",
                    phone = "13800138003",
                    email = "doctor3@hospital.com",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            
            testDoctors.forEach { doctor ->
                try {
                    val existing = doctorDao.getDoctorByCode(doctor.doctorCode)
                    if (existing == null) {
                        doctorDao.insertDoctorInfo(doctor)
                        android.util.Log.d("TestDataHelper", "创建测试医生: ${doctor.doctorCode} - ${doctor.name}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TestDataHelper", "创建医生失败: ${e.message}")
                }
            }
        }
    }
}



