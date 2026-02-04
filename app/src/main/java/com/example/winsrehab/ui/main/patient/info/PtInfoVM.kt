package com.example.winsrehab.ui.main.patient.info

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class PtInfoVM: ViewModel() {
    private val repository : PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    private val doctorDao by lazy {
        MyApp.instance.database.doctorDao()
    }
    private val mode = MutableLiveData<String>()
    val patient = MutableLiveData<Patient>()

    fun loadPatient(account: String) {
        viewModelScope.launch {
            val patientData = repository.getPatientByAccount(account)
            
            // 如果患者有绑定医生工号，查询医生姓名
            if (patientData != null && patientData.doctorCode != "未设置") {
                android.util.Log.d("PtInfoVM", "患者绑定的医生工号: ${patientData.doctorCode}")
                val doctor = doctorDao.getDoctorByCode(patientData.doctorCode)
                
                if (doctor != null) {
                    android.util.Log.d("PtInfoVM", "找到医生信息: 工号=${doctor.doctorCode}, 姓名=${doctor.name}")
                    // 如果找到医生信息且姓名不同，更新患者的医生姓名
                    if (doctor.name != patientData.doctorName) {
                        val updatedPatient = patientData.copy(doctorName = doctor.name)
                        repository.updatePatient(updatedPatient)
                        patient.value = updatedPatient
                        android.util.Log.d("PtInfoVM", "已更新患者的医生姓名为: ${doctor.name}")
                    } else {
                        patient.value = patientData
                    }
                } else {
                    android.util.Log.w("PtInfoVM", "未找到工号为 ${patientData.doctorCode} 的医生")
                    patient.value = patientData
                }
            } else {
                patient.value = patientData
            }
        }
    }

    fun saveBasicInfo(
        account: String, name: String, gender: String, age: Int,
        doctor: String, doctorCode: String, signature: String
    ) {
        viewModelScope.launch {
            val old = repository.getPatientByAccount(account)
            val newPatient = old?.copy(
                name = name, 
                gender = gender, 
                age = age,
                doctorName = doctor, 
                doctorCode = doctorCode,
                account = account,
                signature = signature
            ) ?: Patient(
                patientId = account,   // 必须有唯一标识
                account = account,
                password = old?.password ?: "",
                name = name,
                gender = gender,
                age = age,
                doctorName = doctor,
                doctorCode = doctorCode,
                signature = signature
            )
            repository.insertPatient(newPatient)
            patient.postValue(newPatient)
        }
    }

    fun savePatientInfo(updatedPatient: Patient, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePatient(updatedPatient)
                patient.postValue(updatedPatient)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun saveRehabInfo(
        account: String, diagnosis: String, rehabStage: String,
        progress: Int
    ) {
        viewModelScope.launch {
            val old = repository.getPatientByAccount(account) ?: return@launch
            val updated = old.copy(
                diagnosis = diagnosis,
                rehabStage = rehabStage,
                overallProgress = progress,
                hasAlert = false
            )
            repository.updatePatient(updated)
            patient.postValue(updated)
        }
    }
}