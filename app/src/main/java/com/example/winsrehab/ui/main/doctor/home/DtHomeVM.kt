package com.example.winsrehab.ui.main.doctor.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

class DtHomeVM: ViewModel (){
    private val patientRepository = PatientRepository(MyApp.instance.database.patientDao())

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    fun loadPatients(doctorCode: String) {
        viewModelScope.launch {
            patientRepository.getPatientsByDoctor(doctorCode)
                .collect { list ->
                    _patients.postValue(list)
                }
        }
    }

}