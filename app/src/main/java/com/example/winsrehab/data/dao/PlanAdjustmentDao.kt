package com.example.winsrehab.data.dao

import androidx.room.*
import com.example.winsrehab.data.entity.PlanAdjustment
import kotlinx.coroutines.flow.Flow

/**
 * 计划调整记录数据访问对象
 * 
 * 提供计划调整记录的增删改查操作
 */
@Dao
interface PlanAdjustmentDao {
    
    // ==================== 基础 CRUD 操作 ====================
    
    /**
     * 插入调整记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: PlanAdjustment): Long
    
    /**
     * 批量插入调整记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustments(adjustments: List<PlanAdjustment>): List<Long>
    
    /**
     * 更新调整记录
     */
    @Update
    suspend fun updateAdjustment(adjustment: PlanAdjustment)
    
    /**
     * 删除调整记录
     */
    @Delete
    suspend fun deleteAdjustment(adjustment: PlanAdjustment)
    
    /**
     * 根据ID获取调整记录
     */
    @Query("SELECT * FROM plan_adjustment WHERE adjustmentId = :adjustmentId")
    fun getAdjustmentById(adjustmentId: Long): Flow<PlanAdjustment?>
    
    /**
     * 根据ID获取调整记录（一次性查询）
     */
    @Query("SELECT * FROM plan_adjustment WHERE adjustmentId = :adjustmentId")
    suspend fun getAdjustmentByIdOnce(adjustmentId: Long): PlanAdjustment?
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取计划的所有调整记录（按时间倒序）
     */
    @Query("SELECT * FROM plan_adjustment WHERE planId = :planId ORDER BY createdAt DESC")
    fun getAdjustmentsByPlan(planId: Long): Flow<List<PlanAdjustment>>
    
    /**
     * 获取计划的所有调整记录（一次性查询）
     */
    @Query("SELECT * FROM plan_adjustment WHERE planId = :planId ORDER BY createdAt DESC")
    suspend fun getAdjustmentsByPlanOnce(planId: Long): List<PlanAdjustment>
    
    /**
     * 获取计划的调整次数
     */
    @Query("SELECT COUNT(*) FROM plan_adjustment WHERE planId = :planId")
    suspend fun getAdjustmentCountByPlan(planId: Long): Int
    
    /**
     * 获取医生的所有调整记录
     */
    @Query("SELECT * FROM plan_adjustment WHERE doctorCode = :doctorCode ORDER BY createdAt DESC")
    fun getAdjustmentsByDoctor(doctorCode: String): Flow<List<PlanAdjustment>>
    
    /**
     * 获取指定类型的调整记录
     */
    @Query("SELECT * FROM plan_adjustment WHERE planId = :planId AND adjustmentType = :type ORDER BY createdAt DESC")
    fun getAdjustmentsByType(planId: Long, type: String): Flow<List<PlanAdjustment>>
    
    /**
     * 获取计划最近的调整记录
     */
    @Query("SELECT * FROM plan_adjustment WHERE planId = :planId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAdjustment(planId: Long): PlanAdjustment?
    
    /**
     * 获取计划指定时间范围内的调整记录
     */
    @Query("SELECT * FROM plan_adjustment WHERE planId = :planId AND createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getAdjustmentsByTimeRange(planId: Long, startTime: Long, endTime: Long): Flow<List<PlanAdjustment>>
    
    /**
     * 删除计划的所有调整记录
     */
    @Query("DELETE FROM plan_adjustment WHERE planId = :planId")
    suspend fun deleteAdjustmentsByPlan(planId: Long)
    
    /**
     * 获取医生本月的调整次数
     */
    @Query("SELECT COUNT(*) FROM plan_adjustment WHERE doctorCode = :doctorCode AND createdAt >= :monthStartTime")
    suspend fun getAdjustmentCountThisMonth(doctorCode: String, monthStartTime: Long): Int
}

