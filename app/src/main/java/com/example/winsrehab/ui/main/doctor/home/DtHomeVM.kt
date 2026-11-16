package com.example.winsrehab.ui.main.doctor.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.model.BindingStatus
import com.example.winsrehab.data.repository.DoctorTaskRepository
import com.example.winsrehab.data.repository.PatientRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 医生端首页与患者管理共用的数据控制器：患者列表 + 待处理申请
class DtHomeVM : ViewModel() {

    private val patientRepository = PatientRepository(MyApp.instance.database.patientDao())
    private val taskRepository = DoctorTaskRepository(MyApp.instance.database.doctorTaskDao())

    private val _patients = MutableLiveData<List<Patient>>()
    val patients: LiveData<List<Patient>> = _patients

    // 已绑定患者：医生端所有患者数都从这里取，保证首页统计与患者管理列表同源同口径
    private val _activePatients = MutableLiveData<List<Patient>>()
    val activePatients: LiveData<List<Patient>> = _activePatients

    // 最新患者申请列表：按申请时间倒序，拿到的条数同时喂横幅与欢迎卡
    private val _pendingApplications = MutableLiveData<List<DoctorTask>>()
    val pendingApplications: LiveData<List<DoctorTask>> = _pendingApplications

    // 待处理计数：与申请列表同一份查询结果，避免两处口径不一致
    private val _pendingCount = MutableLiveData<Int>()
    val pendingCount: LiveData<Int> = _pendingCount

    // 当前订阅的是哪个工号：同一工号不重复订阅，工号变了就换订阅，空白工号直接不查
    private var loadedDoctorCode: String? = null
    private var loadedPendingCode: String? = null
    private var patientsJob: Job? = null
    private var pendingJob: Job? = null

    fun loadPatients(doctorCode: String) {
        val code = doctorCode.trim()
        // 空工号查不出任何患者，还会把订阅位置占掉，必须直接返回
        if (code.isEmpty()) return
        if (code == loadedDoctorCode) return
        loadedDoctorCode = code
        patientsJob?.cancel()
        patientsJob = viewModelScope.launch {
            patientRepository.getPatientsByDoctor(code)
                .collect { list ->
                    _patients.postValue(list)
                    // 待确认 / 已拒绝的申请不算「管理患者」，统一在这里过滤
                    _activePatients.postValue(list.filter { it.bindingStatus == BindingStatus.ACTIVE })
                }
        }
    }

    // 首页专用：一次性把待处理申请与计数都读出来
    fun loadPendingApplications(doctorCode: String) {
        val code = doctorCode.trim()
        if (code.isEmpty()) return
        if (code == loadedPendingCode) return
        loadedPendingCode = code
        pendingJob?.cancel()
        pendingJob = viewModelScope.launch {
            taskRepository.getPendingBindingTasks(code)
                .collect { list ->
                    val sorted = list.sortedByDescending { it.createdAt }
                    _pendingApplications.postValue(sorted)
                    _pendingCount.postValue(sorted.size)
                }
        }
    }
}
