package com.example.winsrehab.ui.main.doctor.application

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.repository.DoctorRepository
import com.example.winsrehab.data.repository.DoctorTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientApplicationViewModel : ViewModel() {
    
    private val patientRepository: PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    
    private val doctorRepository: DoctorRepository by lazy {
        DoctorRepository(MyApp.instance.database.doctorDao())
    }
    
    private val taskRepository: DoctorTaskRepository by lazy {
        DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())
    }
    
    val applications = MutableLiveData<List<DoctorTask>>()
    val operationResult = MutableLiveData<String>()
    
    fun getPendingApplications(doctorCode: String) {
        viewModelScope.launch {
            taskRepository.getPendingBindingTasks(doctorCode).collectLatest { tasks ->
                applications.postValue(tasks)
            }
        }
    }
    
    fun confirmPatient(patientId: String, doctorCode: String, taskId: Long) {
        viewModelScope.launch {
            try {
                // 1. 更新患者绑定状态
                val patient = patientRepository.getPatientByAccount(patientId)
                if (patient != null) {
                    val updatedPatient = patient.copy(bindingStatus = "active")
                    patientRepository.updatePatient(updatedPatient)
                }
                
                // 2. 更新医生患者数
                val doctor = doctorRepository.getDoctorInfo(doctorCode)
                if (doctor != null) {
                    doctorRepository.updatePatientCount(doctorCode, doctor.patientCount + 1)
                }
                
                // 3. 删除任务
                taskRepository.deleteTask(taskId)
                
                operationResult.postValue("confirm_success")
            } catch (e: Exception) {
                operationResult.postValue("error")
            }
        }
    }
    
    fun rejectPatient(patientId: String, taskId: Long) {
        viewModelScope.launch {
            try {
                // 1. 更新患者绑定状态为拒绝
                val patient = patientRepository.getPatientByAccount(patientId)
                if (patient != null) {
                    val updatedPatient = patient.copy(bindingStatus = "rejected")
                    patientRepository.updatePatient(updatedPatient)
                }
                
                // 2. 删除任务
                taskRepository.deleteTask(taskId)
                
                operationResult.postValue("reject_success")
            } catch (e: Exception) {
                operationResult.postValue("error")
            }
        }
    }
}



