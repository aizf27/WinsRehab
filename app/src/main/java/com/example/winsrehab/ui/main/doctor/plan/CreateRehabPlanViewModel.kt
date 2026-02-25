package com.example.winsrehab.ui.main.doctor.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.RehabPlanRepository
import com.example.winsrehab.data.repository.TrainingTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 创建康复计划ViewModel
 */
class CreateRehabPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MyApp.instance.database
    private val planRepository = RehabPlanRepository(db.rehabPlanDao())
    private val taskRepository = TrainingTaskRepository(db.trainingTaskDao())
    private val patientRepository = PatientRepository(db.patientDao())

    // 当前患者
    private val _patient = MutableLiveData<Patient?>()
    val patient: LiveData<Patient?> = _patient

    // 任务列表
    private val _tasks = MutableLiveData<MutableList<TrainingTask>>(mutableListOf())
    val tasks: LiveData<MutableList<TrainingTask>> = _tasks

    // 操作结果
    private val _saveResult = MutableLiveData<Result<Long>>()
    val saveResult: LiveData<Result<Long>> = _saveResult

    /**
     * 加载患者信息
     */
    fun loadPatient(patientId: String) {
        viewModelScope.launch {
            try {
                val patient = patientRepository.getPatientByIdOnce(patientId)
                _patient.value = patient
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 添加任务
     */
    fun addTask(task: TrainingTask) {
        val currentList = _tasks.value ?: mutableListOf()
        currentList.add(task)
        _tasks.value = currentList
    }

    /**
     * 删除任务
     */
    fun removeTask(task: TrainingTask) {
        val currentList = _tasks.value ?: mutableListOf()
        currentList.remove(task)
        _tasks.value = currentList
    }

    /**
     * 保存计划（草稿或激活）
     */
    fun savePlan(
        planName: String,
        goal: String,
        startDate: String,
        durationDays: Int,
        isDraft: Boolean
    ) {
        viewModelScope.launch {
            try {
                val patient = _patient.value
                if (patient == null) {
                    _saveResult.value = Result.failure(Exception("患者信息未加载"))
                    return@launch
                }

                // 验证输入
                if (planName.isBlank()) {
                    _saveResult.value = Result.failure(Exception("请输入计划名称"))
                    return@launch
                }
                if (goal.isBlank()) {
                    _saveResult.value = Result.failure(Exception("请输入康复目标"))
                    return@launch
                }
                if (startDate.isBlank()) {
                    _saveResult.value = Result.failure(Exception("请选择开始日期"))
                    return@launch
                }
                if (durationDays <= 0) {
                    _saveResult.value = Result.failure(Exception("请输入有效的周期天数"))
                    return@launch
                }

                val taskList = _tasks.value ?: mutableListOf()
                if (taskList.isEmpty() && !isDraft) {
                    _saveResult.value = Result.failure(Exception("请至少添加一个训练任务"))
                    return@launch
                }

                // 计算结束日期
                val endDate = calculateEndDate(startDate, durationDays)

                // 创建计划
                val plan = RehabPlan(
                    patientId = patient.patientId,
                    doctorCode = patient.doctorCode,
                    planName = planName,
                    goal = goal,
                    startDate = startDate,
                    endDate = endDate,
                    status = if (isDraft) "draft" else "active",
                    progress = 0
                )

                val planId = planRepository.createPlan(plan)

                // 保存任务
                if (taskList.isNotEmpty()) {
                    val tasksWithPlanId = taskList.map { it.copy(planId = planId) }
                    taskRepository.createTasks(tasksWithPlanId)
                }

                _saveResult.value = Result.success(planId)
            } catch (e: Exception) {
                e.printStackTrace()
                _saveResult.value = Result.failure(e)
            }
        }
    }

    /**
     * 计算结束日期
     */
    private fun calculateEndDate(startDate: String, durationDays: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(startDate) ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, durationDays)
        return sdf.format(calendar.time)
    }
}

