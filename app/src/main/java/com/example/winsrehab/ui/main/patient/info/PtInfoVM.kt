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
    private val mode = MutableLiveData<String>()
    val patient = MutableLiveData<Patient>()

    fun loadPatient(account: String) {
        viewModelScope.launch {
            patient.value = repository.getPatientByAccount(account)
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