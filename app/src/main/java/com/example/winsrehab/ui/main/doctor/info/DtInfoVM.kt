package com.example.winsrehab.ui.main.doctor.info

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.repository.DoctorRepository
import kotlinx.coroutines.launch

class DtInfoVM: ViewModel() {
    private val repository : DoctorRepository by lazy {
        DoctorRepository(MyApp.instance.database.doctorDao())
    }
    val doctor = MutableLiveData<Doctor?>()
    private val doctorCode = MutableLiveData<String>()

    fun loadDoctorInfo(code: String) {
        doctorCode.value = code
        viewModelScope.launch {
            try {
                val doctorInfo = repository.getDoctorInfo(code)
                doctor.postValue(doctorInfo)
            } catch (e: Exception) {
                // 如果获取医生信息失败，设置为null
                doctor.postValue(null)
            }
        }
    }

    fun saveDoctorInfo(doctor: Doctor, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateDoctorInfo( doctor)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

}
