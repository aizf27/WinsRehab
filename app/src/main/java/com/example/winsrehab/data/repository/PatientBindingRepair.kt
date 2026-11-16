package com.example.winsrehab.data.repository

import android.util.Log
import com.example.winsrehab.data.dao.DoctorTaskDao
import com.example.winsrehab.data.dao.PatientDao
import com.example.winsrehab.data.model.BindingStatus

// 旧版本遗留数据修复：把绑在「占位医生」上的患者复位成未绑定
// 旧数据里医生工号是 00 / 未设置，患者却是 active，导致个人中心显示未绑定、医生端也看不到人
object PatientBindingRepair {

    private const val TAG = "PatientBindingRepair"

    // 旧版本种子数据的占位医生工号，真实医生不会有这个工号
    const val PLACEHOLDER_DOCTOR_CODE = "00"

    // 库里表示「没填」的占位值，与 Patient 字段默认值保持一致
    const val UNSET_VALUE = "未设置"

    // 进 App 时跑一次：复位患者并清掉指向占位医生的申请任务
    suspend fun repair(patientDao: PatientDao, doctorTaskDao: DoctorTaskDao): Int {
        val patients = patientDao.getPatientsBoundToPlaceholder(
            unboundStatus = BindingStatus.UNBOUND,
            unsetValue = UNSET_VALUE,
            placeholderCode = PLACEHOLDER_DOCTOR_CODE
        )

        patients.forEach { patient ->
            // 申请任务也一起清掉，否则医生端会残留一条永远确认不了的申请
            doctorTaskDao.deleteBindingTaskByPatient(patient.patientId)
            patientDao.resetBinding(
                patientId = patient.patientId,
                unsetValue = UNSET_VALUE,
                unboundStatus = BindingStatus.UNBOUND
            )
        }

        if (patients.isNotEmpty()) {
            Log.i(TAG, "复位占位医生绑定: count=${patients.size}, ids=${patients.map { it.patientId }}")
        }
        return patients.size
    }
}
