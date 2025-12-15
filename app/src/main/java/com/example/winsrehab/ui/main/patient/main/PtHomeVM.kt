package com.example.winsrehab.ui.main.patient.main

import androidx.lifecycle.ViewModel
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.repository.PatientRepository

class PtHomeVM: ViewModel() {
    val repository = PatientRepository(MyApp.instance.database.patientDao())

}