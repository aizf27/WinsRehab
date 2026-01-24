package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.DoctorTaskDao
import com.example.winsrehab.data.entity.DoctorTask
import kotlinx.coroutines.flow.Flow

class DoctorTaskRepository(private val dao: DoctorTaskDao) {
    
    fun getPendingBindingTasks(doctorCode: String): Flow<List<DoctorTask>> {
        return dao.getPendingTasksByDoctor(doctorCode, "patient_binding")
    }
    
    fun getPendingTaskCount(doctorCode: String): Flow<Int> {
        return dao.getPendingTaskCount(doctorCode, "patient_binding")
    }
    
    suspend fun createBindingTask(task: DoctorTask) {
        dao.insertTask(task)
    }
    
    suspend fun deleteTask(taskId: Long) {
        dao.deleteTask(taskId)
    }
    
    suspend fun deleteBindingTaskByPatient(patientId: String) {
        dao.deleteBindingTaskByPatient(patientId)
    }
}





