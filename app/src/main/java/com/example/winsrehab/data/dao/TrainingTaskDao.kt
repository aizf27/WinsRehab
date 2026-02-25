package com.example.winsrehab.data.dao

import androidx.room.*
import com.example.winsrehab.data.entity.TrainingTask
import kotlinx.coroutines.flow.Flow

/**
 * 训练任务模板数据访问对象
 * 
 * 提供训练任务模板的增删改查操作
 */
@Dao
interface TrainingTaskDao {
    
    // ==================== 基础 CRUD 操作 ====================
    
    /**
     * 插入训练任务
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TrainingTask): Long
    
    /**
     * 批量插入训练任务
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TrainingTask>): List<Long>
    
    /**
     * 更新训练任务
     */
    @Update
    suspend fun updateTask(task: TrainingTask)
    
    /**
     * 删除训练任务
     */
    @Delete
    suspend fun deleteTask(task: TrainingTask)
    
    /**
     * 根据ID删除训练任务
     */
    @Query("DELETE FROM training_task WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: Long)
    
    /**
     * 根据ID获取训练任务
     */
    @Query("SELECT * FROM training_task WHERE taskId = :taskId")
    fun getTaskById(taskId: Long): Flow<TrainingTask?>
    
    /**
     * 根据ID获取训练任务（一次性查询）
     */
    @Query("SELECT * FROM training_task WHERE taskId = :taskId")
    suspend fun getTaskByIdOnce(taskId: Long): TrainingTask?
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取计划的所有训练任务（按创建时间排序）
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId ORDER BY createdAt ASC")
    fun getTasksByPlan(planId: Long): Flow<List<TrainingTask>>
    
    /**
     * 获取计划的所有训练任务（一次性查询）
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId ORDER BY createdAt ASC")
    suspend fun getTasksByPlanOnce(planId: Long): List<TrainingTask>
    
    /**
     * 获取计划的任务数量
     */
    @Query("SELECT COUNT(*) FROM training_task WHERE planId = :planId")
    suspend fun getTaskCountByPlan(planId: Long): Int
    
    /**
     * 获取计划中指定类型的任务
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId AND taskType = :taskType ORDER BY createdAt ASC")
    fun getTasksByType(planId: Long, taskType: String): Flow<List<TrainingTask>>
    
    /**
     * 获取计划中重要优先级的任务
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId AND priority = 'important' ORDER BY createdAt ASC")
    fun getImportantTasks(planId: Long): Flow<List<TrainingTask>>
    
    /**
     * 获取每日执行的任务
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId AND frequency = 'daily' ORDER BY createdAt ASC")
    suspend fun getDailyTasks(planId: Long): List<TrainingTask>
    
    /**
     * 获取每周执行的任务
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId AND frequency = 'weekly' ORDER BY createdAt ASC")
    suspend fun getWeeklyTasks(planId: Long): List<TrainingTask>
    
    /**
     * 获取间隔执行的任务
     */
    @Query("SELECT * FROM training_task WHERE planId = :planId AND frequency = 'interval' ORDER BY createdAt ASC")
    suspend fun getIntervalTasks(planId: Long): List<TrainingTask>
    
    /**
     * 删除计划的所有任务
     */
    @Query("DELETE FROM training_task WHERE planId = :planId")
    suspend fun deleteTasksByPlan(planId: Long)
    
    /**
     * 更新任务的参考视频
     */
    @Query("UPDATE training_task SET referenceVideoId = :videoId WHERE taskId = :taskId")
    suspend fun updateReferenceVideo(taskId: Long, videoId: Long?)
}

