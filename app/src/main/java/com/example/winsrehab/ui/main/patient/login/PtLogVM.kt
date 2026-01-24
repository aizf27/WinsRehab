package com.example.winsrehab.ui.main.patient.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.DoctorRepository
import com.example.winsrehab.data.repository.DoctorTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class PtLogVM: ViewModel () {
    private val repository : PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    private val doctorRepository : DoctorRepository by lazy {
        DoctorRepository(MyApp.instance.database.doctorDao())
    }
    private val taskRepository : DoctorTaskRepository by lazy {
        DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())
    }
    
    val loginResult = MutableLiveData<Int>()        // 0=不存在 1=成功 2=密码错误
    val registerResult = MutableLiveData<Boolean>()
    val infoComplete = MutableLiveData<Boolean>()
    val bindingStatus = MutableLiveData<String>()   // 绑定状态
    val bindingResult = MutableLiveData<Int>()      // 0=医生不存在 1=成功 2=失败

    fun loginPatient(account: String, password: String) {
        //启动一个协程
        viewModelScope.launch {
            val success = repository.login(account, password)
            loginResult.postValue(
                when {
                    !repository.isExist(account) -> 0
                    success -> 1
                    else -> 2
                }
            )
        }
    }

    fun registerPatient(account: String, password: String) {
        viewModelScope.launch {
            val success = repository.registerPatient(
                // patientId和account值相同，patientId做主键
                Patient(patientId = account, account = account, password = password)
            )
            registerResult.postValue(success)
        }
    }

    fun checkInfoComplete(account: String) {
        viewModelScope.launch {
            val complete = repository.isInfoComplete(account)
            infoComplete.postValue(complete)
        }
    }
    
    fun checkBindingStatus(account: String) {
        viewModelScope.launch {
            val status = repository.getBindingStatus(account)
            android.util.Log.d("PtLogVM", "checkBindingStatus: account=$account, status=$status")
            bindingStatus.postValue(status ?: "unbound")
        }
    }
    
    fun bindDoctor(patientAccount: String, doctorCode: String) {
        viewModelScope.launch {
            try {
                // 1. 验证医生工号是否存在
                val doctor = doctorRepository.getDoctorInfo(doctorCode)
                if (doctor == null) {
                    bindingResult.postValue(0) // 医生不存在
                    return@launch
                }
                
                // 2. 获取患者信息
                val patient = repository.getPatientByAccount(patientAccount)
                if (patient == null) {
                    bindingResult.postValue(2) // 患者不存在
                    return@launch
                }
                
                // 3. 更新患者信息
                val updatedPatient = patient.copy(
                    doctorCode = doctorCode,
                    doctorName = doctor.name,
                    bindingStatus = "pending"
                )
                repository.updatePatient(updatedPatient)
                
                // 4. 创建医生任务
                val task = DoctorTask(
                    doctorCode = doctorCode,
                    taskType = "patient_binding",
                    patientId = patient.patientId,
                    patientName = patient.name,
                    patientAccount = patient.account,
                    createdAt = System.currentTimeMillis()
                )
                taskRepository.createBindingTask(task)
                
                bindingResult.postValue(1) // 成功
            } catch (e: Exception) {
                bindingResult.postValue(2) // 失败
            }
        }
    }

}