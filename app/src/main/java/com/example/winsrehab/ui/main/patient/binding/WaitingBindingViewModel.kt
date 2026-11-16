package com.example.winsrehab.ui.main.patient.binding

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.repository.DoctorRepository
import com.example.winsrehab.data.repository.DoctorTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

// 等待绑定页的状态来源：查绑定状态，被拒绝时可重新发起绑定申请
class WaitingBindingViewModel : ViewModel() {

    private val repository: PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    private val doctorRepository: DoctorRepository by lazy {
        DoctorRepository(MyApp.instance.database.doctorDao())
    }
    private val taskRepository: DoctorTaskRepository by lazy {
        DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())
    }

    companion object {
        private const val TAG = "WaitingBindingVM"
    }

    val bindingStatus = MutableLiveData<String>()
    // 重新申请绑定的结果：0=医生不存在 1=成功 2=失败
    val bindingResult = MutableLiveData<Int>()

    fun checkBindingStatus(account: String) {
        viewModelScope.launch {
            val status = repository.getBindingStatus(account)
            bindingStatus.postValue(status ?: "pending")
        }
    }

    // 被拒绝后重新申请：校验医生工号、回写患者绑定状态并给医生建一条待处理申请
    fun bindDoctor(patientAccount: String, doctorCode: String) {
        viewModelScope.launch {
            try {
                val doctor = doctorRepository.getDoctorInfo(doctorCode)
                if (doctor == null) {
                    bindingResult.postValue(0)
                    return@launch
                }

                val patient = repository.getPatientByAccount(patientAccount)
                if (patient == null) {
                    bindingResult.postValue(2)
                    return@launch
                }

                repository.updatePatient(
                    patient.copy(
                        doctorCode = doctorCode,
                        doctorName = doctor.name,
                        bindingStatus = "pending"
                    )
                )

                taskRepository.createBindingTask(
                    DoctorTask(
                        doctorCode = doctorCode,
                        taskType = "patient_binding",
                        patientId = patient.patientId,
                        patientName = patient.name,
                        patientAccount = patient.account,
                        createdAt = System.currentTimeMillis()
                    )
                )

                Log.i(TAG, "重新申请绑定成功: account=$patientAccount, doctorCode=$doctorCode")
                bindingResult.postValue(1)
            } catch (e: Exception) {
                Log.e(TAG, "重新申请绑定失败: account=$patientAccount", e)
                bindingResult.postValue(2)
            }
        }
    }
}
