package com.example.winsrehab.utils

import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.entity.TrainingTask
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

    /**
     * 初始化任务生成测试数据
     * 创建测试患者、康复计划和任务模板
     */
    fun initTaskGenerationTestData() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = MyApp.instance.database
            val patientDao = database.patientDao()
            val rehabPlanDao = database.rehabPlanDao()
            val trainingTaskDao = database.trainingTaskDao()

            try {
                // 1. 创建测试患者
                val testPatient = Patient(
                    patientId = "test001",
                    account = "test001",
                    password = "123456",
                    name = "测试患者001",
                    gender = "男",
                    age = 45,
                    doctorCode = "1",
                    doctorName = "张医生",
                    diagnosis = "脑卒中后遗症",
                    rehabStage = "恢复期",
                    bindingStatus = "active",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // 检查患者是否已存在
                val existingPatient = patientDao.getPatientById("test001")
                if (existingPatient == null) {
                    patientDao.insertPatient(testPatient)
                    android.util.Log.i("TestDataHelper", "创建测试患者: test001")
                } else {
                    android.util.Log.i("TestDataHelper", "测试患者已存在: test001")
                }

                // 2. 创建测试康复计划
                val testPlan = RehabPlan(
                    patientId = "test001",
                    doctorCode = "1",
                    planName = "测试康复计划",
                    goal = "测试任务生成功能",
                    startDate = "2026-09-01",
                    endDate = "2026-09-30",
                    status = "active",  // 重要：必须是active状态
                    progress = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val planId = rehabPlanDao.insertPlan(testPlan)
                android.util.Log.i("TestDataHelper", "创建测试康复计划: planId=$planId")

                // 3. 创建测试任务模板（3个不同频率）
                val testTasks = listOf(
                    // 任务A - daily频率
                    TrainingTask(
                        planId = planId,
                        taskName = "肩关节外展训练",
                        taskType = "upper_limb",
                        description = "每天执行的上肢训练",
                        suggestedDuration = 15,
                        frequency = "daily",
                        frequencyDetail = "",
                        priority = "important"
                    ),
                    // 任务B - weekly频率（周一三五）
                    TrainingTask(
                        planId = planId,
                        taskName = "平衡训练",
                        taskType = "balance",
                        description = "每周一三五执行的平衡训练",
                        suggestedDuration = 20,
                        frequency = "weekly",
                        frequencyDetail = "1,3,5",  // 周一、三、五
                        priority = "normal"
                    ),
                    // 任务C - interval频率（间隔2天）
                    TrainingTask(
                        planId = planId,
                        taskName = "步态训练",
                        taskType = "lower_limb",
                        description = "每隔2天执行的下肢步态训练",
                        suggestedDuration = 30,
                        frequency = "interval",
                        frequencyDetail = "2",  // 每隔2天
                        priority = "normal"
                    )
                )

                val taskIds = trainingTaskDao.insertTasks(testTasks)
                android.util.Log.i("TestDataHelper", "创建3个测试任务模板: taskIds=$taskIds")

                android.util.Log.i("TestDataHelper", "========================================")
                android.util.Log.i("TestDataHelper", "任务生成测试数据初始化完成！")
                android.util.Log.i("TestDataHelper", "测试账号: test001 / 123456")
                android.util.Log.i("TestDataHelper", "康复计划ID: $planId")
                android.util.Log.i("TestDataHelper", "任务模板: ${taskIds.size}个")
                android.util.Log.i("TestDataHelper", "========================================")

            } catch (e: Exception) {
                android.util.Log.e("TestDataHelper", "初始化测试数据失败", e)
            }
        }
    }

    /**
     * 清空测试患者的任务实例数据
     * 用于重复测试
     */
    fun clearTestTaskInstances() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val taskInstanceDao = MyApp.instance.database.taskInstanceDao()
                // 直接执行SQL删除
                // taskInstanceDao.deleteInstancesByPatient("test001")  // 需要先在Dao中添加此方法
                android.util.Log.i("TestDataHelper", "清空test001的任务实例数据")
            } catch (e: Exception) {
                android.util.Log.e("TestDataHelper", "清空任务实例失败", e)
            }
        }
    }
}





