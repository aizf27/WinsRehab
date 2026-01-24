package com.example.winsrehab.ui.main.doctor.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.repository.DoctorRepository
import com.example.winsrehab.data.entity.Doctor
import kotlinx.coroutines.launch

class DtLogVM : ViewModel() {

    private val repository: DoctorRepository by lazy {
        DoctorRepository(MyApp.instance.database.doctorDao())
    }

    val loginResult = MutableLiveData<Int>()        //0=不存在 1=成功 2=密码错误
    val registerResult = MutableLiveData<Boolean>()
    val infoComplete = MutableLiveData<Boolean>()

    fun loginDoctor(doctorCode: String, password: String) {
        //启动一个协程
        viewModelScope.launch {
            val success = repository.login(doctorCode, password)
            loginResult.postValue(
                when {
                    !repository.isExist(doctorCode) -> 0
                    success -> 1
                    else -> 2
                }
            )
        }
    }

    fun registerDoctor(doctorCode: String, password: String) {
        viewModelScope.launch {
            val success = repository.registerDoctor(
                Doctor(doctorCode = doctorCode, password = password)
            )
            registerResult.postValue(success)
        }
    }

    fun checkInfoComplete(doctorCode: String) {
        viewModelScope.launch {
            val complete = repository.isInfoComplete(doctorCode)
            infoComplete.postValue(complete)
        }
    }
}
