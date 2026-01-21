package com.example.winsrehab.ui.main.patient.binding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class WaitingBindingViewModel : ViewModel() {
    
    private val repository: PatientRepository by lazy {
        PatientRepository(MyApp.instance.database.patientDao())
    }
    
    val bindingStatus = MutableLiveData<String>()
    
    fun checkBindingStatus(account: String) {
        viewModelScope.launch {
            val status = repository.getBindingStatus(account)
            bindingStatus.postValue(status ?: "pending")
        }
    }
}



