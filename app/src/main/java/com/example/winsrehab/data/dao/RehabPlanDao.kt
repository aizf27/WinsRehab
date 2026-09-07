package com.example.winsrehab.data.dao

import androidx.room.*
import com.example.winsrehab.data.entity.RehabPlan
import kotlinx.coroutines.flow.Flow

/**
 * 康复计划数据访问对象
 * 
 * 提供康复计划的增删改查操作
 */
@Dao
interface RehabPlanDao {
    
    // ==================== 基础 CRUD 操作 ====================
    
    /**
     * 插入康复计划
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: RehabPlan): Long
    
    /**
     * 更新康复计划
     */
    @Update
    suspend fun updatePlan(plan: RehabPlan)
    
    /**
     * 删除康复计划
     */
    @Delete
    suspend fun deletePlan(plan: RehabPlan)
    
    /**
     * 根据ID获取康复计划
     */
    @Query("SELECT * FROM rehab_plan WHERE planId = :planId")
    fun getPlanById(planId: Long): Flow<RehabPlan?>
    
    /**
     * 根据ID获取康复计划（一次性查询）
     */
    @Query("SELECT * FROM rehab_plan WHERE planId = :planId")
    suspend fun getPlanByIdOnce(planId: Long): RehabPlan?
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取患者的所有康复计划（按创建时间倒序）
     */
    @Query("SELECT * FROM rehab_plan WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getPlansByPatient(patientId: String): Flow<List<RehabPlan>>
    
    /**
     * 获取患者的活跃康复计划
     */
    @Query("SELECT * FROM rehab_plan WHERE patientId = :patientId AND status = 'active' ORDER BY createdAt DESC")
    fun getActivePlansByPatient(patientId: String): Flow<List<RehabPlan>>
    
    /**
     * 获取患者的草稿计划
     */
    @Query("SELECT * FROM rehab_plan WHERE patientId = :patientId AND status = 'draft' ORDER BY createdAt DESC")
    fun getDraftPlansByPatient(patientId: String): Flow<List<RehabPlan>>
    
    /**
     * 获取医生制定的所有计划
     */
    @Query("SELECT * FROM rehab_plan WHERE doctorCode = :doctorCode ORDER BY createdAt DESC")
    fun getPlansByDoctor(doctorCode: String): Flow<List<RehabPlan>>
    
    /**
     * 获取医生制定的活跃计划数量
     */
    @Query("SELECT COUNT(*) FROM rehab_plan WHERE doctorCode = :doctorCode AND status = 'active'")
    suspend fun getActivePlanCountByDoctor(doctorCode: String): Int
    
    /**
     * 获取医生本月完成的计划数量
     */
    @Query("SELECT COUNT(*) FROM rehab_plan WHERE doctorCode = :doctorCode AND status = 'completed' AND updatedAt >= :monthStartTime")
    suspend fun getCompletedPlanCountThisMonth(doctorCode: String, monthStartTime: Long): Int
    
    /**
     * 更新计划状态
     */
    @Query("UPDATE rehab_plan SET status = :status, updatedAt = :updatedAt WHERE planId = :planId")
    suspend fun updatePlanStatus(planId: Long, status: String, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 更新计划进度
     */
    @Query("UPDATE rehab_plan SET progress = :progress, updatedAt = :updatedAt WHERE planId = :planId")
    suspend fun updatePlanProgress(planId: Long, progress: Int, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 检查患者是否有活跃的计划
     */
    @Query("SELECT EXISTS(SELECT 1 FROM rehab_plan WHERE patientId = :patientId AND status = 'active')")
    suspend fun hasActivePlan(patientId: String): Boolean
    
    /**
     * 获取指定状态的计划列表
     */
    @Query("SELECT * FROM rehab_plan WHERE status = :status ORDER BY createdAt DESC")
    fun getPlansByStatus(status: String): Flow<List<RehabPlan>>
}



