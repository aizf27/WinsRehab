package com.example.winsrehab.ui.main.doctor.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.winsrehab.MyApp
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.repository.PatientRepository
import com.example.winsrehab.data.repository.RehabPlanRepository
import com.example.winsrehab.ui.main.doctor.adapter.PatientPlanGroupAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 康复计划列表ViewModel
 */
class RehabPlanListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MyApp.instance.database
    private val patientRepository = PatientRepository(db.patientDao())
    private val planRepository = RehabPlanRepository(db.rehabPlanDao())

    private val _patientPlanGroups = MutableLiveData<List<PatientPlanGroupAdapter.PatientPlanGroup>>()
    val patientPlanGroups: LiveData<List<PatientPlanGroupAdapter.PatientPlanGroup>> = _patientPlanGroups

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 加载失败原因：非空时页面切错误态，空表示本次加载没报错
    private val _loadError = MutableLiveData<String?>()
    val loadError: LiveData<String?> = _loadError

    /**
     * 加载医生的所有患者及其康复计划
     */
    fun loadPatientPlans(doctorCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadError.value = null

                // 获取医生的所有患者
                val patients = patientRepository.getPatientsByDoctorOnce(doctorCode)

                // 为每个患者获取康复计划
                val groups = patients.map { patient ->
                    val plans = planRepository.getPatientPlans(patient.patientId).first()
                    PatientPlanGroupAdapter.PatientPlanGroup(
                        patient = patient,
                        plans = plans,
                        isExpanded = false
                    )
                }.filter { group -> group.plans.isNotEmpty() } // 只显示有计划的患者

                _patientPlanGroups.value = groups
            } catch (e: Exception) {
                e.printStackTrace()
                // 查询失败只上报原因，列表内容保持上一次结果
                _loadError.value = e.message ?: "查询计划失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh(doctorCode: String) {
        loadPatientPlans(doctorCode)
    }
}

