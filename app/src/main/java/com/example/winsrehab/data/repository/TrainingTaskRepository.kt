package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.TrainingTaskDao
import com.example.winsrehab.data.entity.TrainingTask
import kotlinx.coroutines.flow.Flow

/**
 * 训练任务模板Repository
 * 
 * 封装训练任务模板相关的业务逻辑
 */
class TrainingTaskRepository(private val dao: TrainingTaskDao) {
    
    // ==================== 基础操作 ====================
    
    /**
     * 创建训练任务
     * @return 任务ID
     */
    suspend fun createTask(task: TrainingTask): Long {
        return dao.insertTask(task)
    }
    
    /**
     * 批量创建训练任务
     * @return 任务ID列表
     */
    suspend fun createTasks(tasks: List<TrainingTask>): List<Long> {
        return dao.insertTasks(tasks)
    }
    
    /**
     * 更新训练任务
     */
    suspend fun updateTask(task: TrainingTask) {
        dao.updateTask(task)
    }
    
    /**
     * 删除训练任务
     */
    suspend fun deleteTask(task: TrainingTask) {
        dao.deleteTask(task)
    }
    
    /**
     * 根据ID删除任务
     */
    suspend fun deleteTaskById(taskId: Long) {
        dao.deleteTaskById(taskId)
    }
    
    /**
     * 根据ID获取任务
     */
    fun getTaskById(taskId: Long): Flow<TrainingTask?> {
        return dao.getTaskById(taskId)
    }
    
    /**
     * 根据ID获取任务（一次性）
     */
    suspend fun getTaskByIdOnce(taskId: Long): TrainingTask? {
        return dao.getTaskByIdOnce(taskId)
    }
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取计划的所有任务
     */
    fun getPlanTasks(planId: Long): Flow<List<TrainingTask>> {
        return dao.getTasksByPlan(planId)
    }
    
    /**
     * 获取计划的所有任务（一次性）
     */
    suspend fun getPlanTasksOnce(planId: Long): List<TrainingTask> {
        return dao.getTasksByPlanOnce(planId)
    }
    
    /**
     * 获取计划的任务数量
     */
    suspend fun getTaskCount(planId: Long): Int {
        return dao.getTaskCountByPlan(planId)
    }
    
    /**
     * 获取指定类型的任务
     */
    fun getTasksByType(planId: Long, taskType: String): Flow<List<TrainingTask>> {
        return dao.getTasksByType(planId, taskType)
    }
    
    /**
     * 获取重要优先级的任务
     */
    fun getImportantTasks(planId: Long): Flow<List<TrainingTask>> {
        return dao.getImportantTasks(planId)
    }
    
    // ==================== 任务生成相关 ====================
    
    /**
     * 获取每日执行的任务（用于生成今日任务实例）
     */
    suspend fun getDailyTasks(planId: Long): List<TrainingTask> {
        return dao.getDailyTasks(planId)
    }
    
    /**
     * 获取每周执行的任务（用于生成今日任务实例）
     */
    suspend fun getWeeklyTasks(planId: Long): List<TrainingTask> {
        return dao.getWeeklyTasks(planId)
    }
    
    /**
     * 获取间隔执行的任务（用于生成今日任务实例）
     */
    suspend fun getIntervalTasks(planId: Long): List<TrainingTask> {
        return dao.getIntervalTasks(planId)
    }
    
    /**
     * 删除计划的所有任务
     */
    suspend fun deletePlanTasks(planId: Long) {
        dao.deleteTasksByPlan(planId)
    }
    
    /**
     * 更新任务的参考视频
     */
    suspend fun updateReferenceVideo(taskId: Long, videoId: Long?) {
        dao.updateReferenceVideo(taskId, videoId)
    }
}

