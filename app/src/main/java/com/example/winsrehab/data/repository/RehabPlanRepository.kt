package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.RehabPlanDao
import com.example.winsrehab.data.entity.RehabPlan
import kotlinx.coroutines.flow.Flow

/**
 * 康复计划Repository
 * 
 * 封装康复计划相关的业务逻辑
 */
class RehabPlanRepository(private val dao: RehabPlanDao) {
    
    // ==================== 基础操作 ====================
    
    /**
     * 创建康复计划
     * @return 计划ID
     */
    suspend fun createPlan(plan: RehabPlan): Long {
        return dao.insertPlan(plan)
    }
    
    /**
     * 更新康复计划
     */
    suspend fun updatePlan(plan: RehabPlan) {
        dao.updatePlan(plan)
    }
    
    /**
     * 删除康复计划
     */
    suspend fun deletePlan(plan: RehabPlan) {
        dao.deletePlan(plan)
    }
    
    /**
     * 根据ID获取计划
     */
    fun getPlanById(planId: Long): Flow<RehabPlan?> {
        return dao.getPlanById(planId)
    }
    
    /**
     * 根据ID获取计划（一次性）
     */
    suspend fun getPlanByIdOnce(planId: Long): RehabPlan? {
        return dao.getPlanByIdOnce(planId)
    }
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取患者的所有计划
     */
    fun getPatientPlans(patientId: String): Flow<List<RehabPlan>> {
        return dao.getPlansByPatient(patientId)
    }
    
    /**
     * 获取患者的活跃计划
     */
    fun getActivePatientPlans(patientId: String): Flow<List<RehabPlan>> {
        return dao.getActivePlansByPatient(patientId)
    }
    
    /**
     * 获取患者的草稿计划
     */
    fun getDraftPatientPlans(patientId: String): Flow<List<RehabPlan>> {
        return dao.getDraftPlansByPatient(patientId)
    }
    
    /**
     * 获取医生制定的所有计划
     */
    fun getDoctorPlans(doctorCode: String): Flow<List<RehabPlan>> {
        return dao.getPlansByDoctor(doctorCode)
    }
    
    /**
     * 检查患者是否有活跃计划
     */
    suspend fun hasActivePlan(patientId: String): Boolean {
        return dao.hasActivePlan(patientId)
    }
    
    // ==================== 状态管理 ====================
    
    /**
     * 激活计划（从草稿变为活跃）
     */
    suspend fun activatePlan(planId: Long) {
        dao.updatePlanStatus(planId, "active", System.currentTimeMillis())
    }
    
    /**
     * 完成计划
     */
    suspend fun completePlan(planId: Long) {
        dao.updatePlanStatus(planId, "completed", System.currentTimeMillis())
    }
    
    /**
     * 取消计划
     */
    suspend fun cancelPlan(planId: Long) {
        dao.updatePlanStatus(planId, "cancelled", System.currentTimeMillis())
    }
    
    /**
     * 更新计划进度
     */
    suspend fun updateProgress(planId: Long, progress: Int) {
        dao.updatePlanProgress(planId, progress, System.currentTimeMillis())
    }
    
    // ==================== 统计功能 ====================
    
    /**
     * 获取医生的活跃计划数量
     */
    suspend fun getActivePlanCount(doctorCode: String): Int {
        return dao.getActivePlanCountByDoctor(doctorCode)
    }
    
    /**
     * 获取医生本月完成的计划数量
     */
    suspend fun getCompletedPlanCountThisMonth(doctorCode: String): Int {
        val monthStartTime = getMonthStartTime()
        return dao.getCompletedPlanCountThisMonth(doctorCode, monthStartTime)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取本月开始时间戳
     */
    private fun getMonthStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}



