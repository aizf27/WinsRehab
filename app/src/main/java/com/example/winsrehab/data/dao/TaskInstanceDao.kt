package com.example.winsrehab.data.dao

import androidx.room.*
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.data.model.TaskInstanceWithTask
import kotlinx.coroutines.flow.Flow

/**
 * 任务实例数据访问对象
 * 
 * 提供任务实例的增删改查操作
 */
@Dao
interface TaskInstanceDao {
    
    // ==================== 基础 CRUD 操作 ====================
    
    /**
     * 插入任务实例
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: TaskInstance): Long
    
    /**
     * 批量插入任务实例（使用IGNORE策略，遇到冲突跳过）
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstances(instances: List<TaskInstance>): List<Long>
    
    /**
     * 更新任务实例
     */
    @Update
    suspend fun updateInstance(instance: TaskInstance)
    
    /**
     * 删除任务实例
     */
    @Delete
    suspend fun deleteInstance(instance: TaskInstance)
    
    /**
     * 根据ID获取任务实例
     */
    @Query("SELECT * FROM task_instance WHERE instanceId = :instanceId")
    fun getInstanceById(instanceId: Long): Flow<TaskInstance?>
    
    /**
     * 根据ID获取任务实例（一次性查询）
     */
    @Query("SELECT * FROM task_instance WHERE instanceId = :instanceId")
    suspend fun getInstanceByIdOnce(instanceId: Long): TaskInstance?
    
    // ==================== 业务查询 ====================

    /**
     * 获取患者指定状态的任务实例
     */
    @Query("SELECT * FROM task_instance WHERE patientId = :patientId AND status = :status ORDER BY scheduledDate DESC, createdAt DESC")
    fun getInstancesByStatus(patientId: String, status: String): Flow<List<TaskInstance>>
    
    /**
     * 获取任务模板的所有实例
     */
    @Query("SELECT * FROM task_instance WHERE taskId = :taskId ORDER BY scheduledDate DESC")
    fun getInstancesByTask(taskId: Long): Flow<List<TaskInstance>>
    
    /**
     * 检查指定日期是否已生成任务实例
     */
    @Query("SELECT EXISTS(SELECT 1 FROM task_instance WHERE patientId = :patientId AND taskId = :taskId AND scheduledDate = :date)")
    suspend fun isInstanceExists(patientId: String, taskId: Long, date: String): Boolean

    /**
     * 某计划下某患者、在计划窗口内已完成的任务数（进度分子；与分母同窗口）
     */
    @Query("""
        SELECT COUNT(*) FROM task_instance ti
        INNER JOIN training_task tt ON tt.taskId = ti.taskId
        WHERE tt.planId = :planId AND ti.patientId = :patientId AND ti.status = 'completed'
          AND ti.scheduledDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getCompletedCountInWindow(planId: Long, patientId: String, startDate: String, endDate: String): Int

    /**
     * 本周区间任务联表查询：只投影今日列表需要的任务名/时长/优先级
     * 今日列表与本周柱状图共用这一份结果，避免同页两套数据源
     */
    @Query("""
        SELECT ti.*, tt.taskName AS taskName,
               tt.suggestedDuration AS suggestedDuration, tt.priority AS priority
        FROM task_instance ti
        INNER JOIN training_task tt ON tt.taskId = ti.taskId
        WHERE ti.patientId = :patientId AND ti.scheduledDate BETWEEN :startDate AND :endDate
        ORDER BY CASE tt.priority WHEN 'important' THEN 0 ELSE 1 END ASC, ti.createdAt ASC
    """)
    fun getWeekInstancesWithTask(patientId: String, startDate: String, endDate: String): Flow<List<TaskInstanceWithTask>>
    
    /**
     * 更新任务实例状态
     */
    @Query("UPDATE task_instance SET status = :status, completedAt = :completedAt WHERE instanceId = :instanceId")
    suspend fun updateInstanceStatus(instanceId: Long, status: String, completedAt: Long? = null)
    
    /**
     * 更新任务实例的关联视频
     */
    @Query("UPDATE task_instance SET videoId = :videoId, status = :status WHERE instanceId = :instanceId")
    suspend fun updateInstanceVideo(instanceId: Long, videoId: Long, status: String = "waiting_review")
    
    /**
     * 删除任务模板的所有实例
     */
    @Query("DELETE FROM task_instance WHERE taskId = :taskId")
    suspend fun deleteInstancesByTask(taskId: Long)
    
    /**
     * 删除患者指定日期之前的任务实例（清理历史数据）
     */
    @Query("DELETE FROM task_instance WHERE patientId = :patientId AND scheduledDate < :date")
    suspend fun deleteInstancesBeforeDate(patientId: String, date: String)
}



