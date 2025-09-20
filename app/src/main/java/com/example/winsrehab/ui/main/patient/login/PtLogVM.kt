package com.example.winsrehab.ui.main.patient.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class PtLogVM: ViewModel () {
    private val repository : PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    val loginResult = MutableLiveData<Int>()        // 0=不存在 1=成功 2=密码错误
    val registerResult = MutableLiveData<Boolean>()
    val infoComplete = MutableLiveData<Boolean>()

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
                //id和account值相同，id做主键
                Patient(id = account,account = account, password = password)
            )
            registerResult.postValue(success)
        }
    }

    fun checkInfoComplete(id: String) {
        viewModelScope.launch {
            val complete = repository.isInfoComplete(id)
            infoComplete.postValue(complete)
        }
    }

}