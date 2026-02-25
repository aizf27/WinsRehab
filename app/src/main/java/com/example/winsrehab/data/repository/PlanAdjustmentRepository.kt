package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.PlanAdjustmentDao
import com.example.winsrehab.data.entity.PlanAdjustment
import kotlinx.coroutines.flow.Flow

/**
 * 计划调整记录Repository
 * 
 * 封装计划调整记录相关的业务逻辑
 */
class PlanAdjustmentRepository(private val dao: PlanAdjustmentDao) {
    
    // ==================== 基础操作 ====================
    
    /**
     * 创建调整记录
     * @return 调整记录ID
     */
    suspend fun createAdjustment(adjustment: PlanAdjustment): Long {
        return dao.insertAdjustment(adjustment)
    }
    
    /**
     * 批量创建调整记录
     */
    suspend fun createAdjustments(adjustments: List<PlanAdjustment>): List<Long> {
        return dao.insertAdjustments(adjustments)
    }
    
    /**
     * 更新调整记录
     */
    suspend fun updateAdjustment(adjustment: PlanAdjustment) {
        dao.updateAdjustment(adjustment)
    }
    
    /**
     * 删除调整记录
     */
    suspend fun deleteAdjustment(adjustment: PlanAdjustment) {
        dao.deleteAdjustment(adjustment)
    }
    
    /**
     * 根据ID获取调整记录
     */
    fun getAdjustmentById(adjustmentId: Long): Flow<PlanAdjustment?> {
        return dao.getAdjustmentById(adjustmentId)
    }
    
    /**
     * 根据ID获取调整记录（一次性）
     */
    suspend fun getAdjustmentByIdOnce(adjustmentId: Long): PlanAdjustment? {
        return dao.getAdjustmentByIdOnce(adjustmentId)
    }
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取计划的所有调整记录
     */
    fun getPlanAdjustments(planId: Long): Flow<List<PlanAdjustment>> {
        return dao.getAdjustmentsByPlan(planId)
    }
    
    /**
     * 获取计划的所有调整记录（一次性）
     */
    suspend fun getPlanAdjustmentsOnce(planId: Long): List<PlanAdjustment> {
        return dao.getAdjustmentsByPlanOnce(planId)
    }
    
    /**
     * 获取计划的调整次数
     */
    suspend fun getAdjustmentCount(planId: Long): Int {
        return dao.getAdjustmentCountByPlan(planId)
    }
    
    /**
     * 获取医生的所有调整记录
     */
    fun getDoctorAdjustments(doctorCode: String): Flow<List<PlanAdjustment>> {
        return dao.getAdjustmentsByDoctor(doctorCode)
    }
    
    /**
     * 获取指定类型的调整记录
     */
    fun getAdjustmentsByType(planId: Long, type: String): Flow<List<PlanAdjustment>> {
        return dao.getAdjustmentsByType(planId, type)
    }
    
    /**
     * 获取计划最近的调整记录
     */
    suspend fun getLatestAdjustment(planId: Long): PlanAdjustment? {
        return dao.getLatestAdjustment(planId)
    }
    
    /**
     * 获取指定时间范围内的调整记录
     */
    fun getAdjustmentsByTimeRange(planId: Long, startTime: Long, endTime: Long): Flow<List<PlanAdjustment>> {
        return dao.getAdjustmentsByTimeRange(planId, startTime, endTime)
    }
    
    /**
     * 删除计划的所有调整记录
     */
    suspend fun deletePlanAdjustments(planId: Long) {
        dao.deleteAdjustmentsByPlan(planId)
    }
    
    // ==================== 统计功能 ====================
    
    /**
     * 获取医生本月的调整次数
     */
    suspend fun getAdjustmentCountThisMonth(doctorCode: String): Int {
        val monthStartTime = getMonthStartTime()
        return dao.getAdjustmentCountThisMonth(doctorCode, monthStartTime)
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

