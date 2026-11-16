package com.example.winsrehab.ui.main.doctor.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.repository.DoctorTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.launch

// 医生端首页与患者管理共用的数据控制器：患者列表 + 待处理申请
class DtHomeVM : ViewModel() {

    private val patientRepository = PatientRepository(MyApp.instance.database.patientDao())
    private val taskRepository = DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    // 最新患者申请列表：按申请时间倒序，拿到的条数同时喂横幅与欢迎卡
    private val _pendingApplications = MutableLiveData<List<DoctorTask>>()
    val pendingApplications: LiveData<List<DoctorTask>> = _pendingApplications

    // 待处理计数：与申请列表同一份查询结果，避免两处口径不一致
    private val _pendingCount = MutableLiveData<Int>()
    val pendingCount: LiveData<Int> = _pendingCount

    fun loadPatients(doctorCode: String) {
        viewModelScope.launch {
            patientRepository.getPatientsByDoctor(doctorCode)
                .collect { list ->
                    _patients.postValue(list)
                }
        }
    }

    // 首页专用：一次性把待处理申请与计数都读出来
    fun loadPendingApplications(doctorCode: String) {
        viewModelScope.launch {
            taskRepository.getPendingBindingTasks(doctorCode)
                .collect { list ->
                    val sorted = list.sortedByDescending { it.createdAt }
                    _pendingApplications.postValue(sorted)
                    _pendingCount.postValue(sorted.size)
                }
        }
    }
}
