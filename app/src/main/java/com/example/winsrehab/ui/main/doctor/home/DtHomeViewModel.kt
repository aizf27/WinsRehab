package com.example.winsrehab.ui.main.doctor.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.repository.DoctorTaskRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DtHomeViewModel : ViewModel() {
    
    private val taskRepository: DoctorTaskRepository by lazy {
        DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())
    }
    
    val pendingCount = MutableLiveData<Int>()
    
    fun getPendingTaskCount(doctorCode: String) {
        viewModelScope.launch {
            taskRepository.getPendingTaskCount(doctorCode).collectLatest { count ->
                pendingCount.postValue(count)
            }
        }
    }
}



