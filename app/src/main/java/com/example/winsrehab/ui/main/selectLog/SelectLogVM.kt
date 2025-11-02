package com.example.winsrehab.ui.main.selectLog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SelectLogVM(application: Application) : AndroidViewModel(application) {
    //用于导航的LiveData
    private val _navigationEvent = MutableLiveData<NavigationDestination>()

    //确保只暴露不可变得LiveData在外面
    val navigationEvent: LiveData<NavigationDestination> = _navigationEvent

    fun onDoctorLoginClicked() {
        _navigationEvent.value = NavigationDestination.DOCTOR_LOGIN
    }

    fun onPatientLoginClicked() {
        _navigationEvent.value = NavigationDestination.PATIENT_LOGIN
    }
    //枚举类
    enum class NavigationDestination {
        DOCTOR_LOGIN,
        PATIENT_LOGIN
    }
}