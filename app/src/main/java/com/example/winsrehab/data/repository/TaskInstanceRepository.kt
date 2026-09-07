package com.example.winsrehab.data.repository

import com.example.winsrehab.data.dao.TaskInstanceDao
import com.example.winsrehab.data.entity.TaskInstance
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务实例Repository
 * 
 * 封装任务实例相关的业务逻辑
 */
class TaskInstanceRepository(private val dao: TaskInstanceDao) {
    
    // ==================== 基础操作 ====================
    
    /**
     * 创建任务实例
     * @return 实例ID
     */
    suspend fun createInstance(instance: TaskInstance): Long {
        return dao.insertInstance(instance)
    }
    
    /**
     * 批量创建任务实例
     * @return 实例ID列表
     */
    suspend fun createInstances(instances: List<TaskInstance>): List<Long> {
        return dao.insertInstances(instances)
    }
    
    /**
     * 更新任务实例
     */
    suspend fun updateInstance(instance: TaskInstance) {
        dao.updateInstance(instance)
    }
    
    /**
     * 删除任务实例
     */
    suspend fun deleteInstance(instance: TaskInstance) {
        dao.deleteInstance(instance)
    }
    
    /**
     * 根据ID获取任务实例
     */
    fun getInstanceById(instanceId: Long): Flow<TaskInstance?> {
        return dao.getInstanceById(instanceId)
    }
    
    /**
     * 根据ID获取任务实例（一次性）
     */
    suspend fun getInstanceByIdOnce(instanceId: Long): TaskInstance? {
        return dao.getInstanceByIdOnce(instanceId)
    }
    
    // ==================== 业务查询 ====================
    
    /**
     * 获取患者今日任务
     */
    fun getTodayInstances(patientId: String): Flow<List<TaskInstance>> {
        val today = getTodayDate()
        return dao.getInstancesByDate(patientId, today)
    }
    
    /**
     * 获取患者今日任务（一次性）
     */
    suspend fun getTodayInstancesOnce(patientId: String): List<TaskInstance> {
        val today = getTodayDate()
        return dao.getInstancesByDateOnce(patientId, today)
    }
    
    /**
     * 获取患者指定日期的任务
     */
    fun getInstancesByDate(patientId: String, date: String): Flow<List<TaskInstance>> {
        return dao.getInstancesByDate(patientId, date)
    }
    
    /**
     * 获取患者本周任务
     */
    fun getWeekInstances(patientId: String): Flow<List<TaskInstance>> {
        val (startDate, endDate) = getWeekDateRange()
        return dao.getInstancesByDateRange(patientId, startDate, endDate)
    }
    
    /**
     * 获取患者本周任务（一次性）
     */
    suspend fun getWeekInstancesOnce(patientId: String): List<TaskInstance> {
        val (startDate, endDate) = getWeekDateRange()
        return dao.getInstancesByDateRangeOnce(patientId, startDate, endDate)
    }
    
    /**
     * 获取指定状态的任务实例
     */
    fun getInstancesByStatus(patientId: String, status: String): Flow<List<TaskInstance>> {
        return dao.getInstancesByStatus(patientId, status)
    }
    
    // ==================== 任务生成相关 ====================
    
    /**
     * 检查任务实例是否已存在（防止重复生成）
     */
    suspend fun isInstanceExists(patientId: String, taskId: Long, date: String): Boolean {
        return dao.isInstanceExists(patientId, taskId, date)
    }
    
    // ==================== 统计功能 ====================
    
    /**
     * 获取患者今日任务总数
     */
    suspend fun getTodayTaskCount(patientId: String): Int {
        val today = getTodayDate()
        return dao.getTodayTaskCount(patientId, today)
    }
    
    /**
     * 获取患者今日已完成任务数
     */
    suspend fun getTodayCompletedCount(patientId: String): Int {
        val today = getTodayDate()
        return dao.getTodayCompletedCount(patientId, today)
    }
    
    /**
     * 获取患者本周任务总数
     */
    suspend fun getWeekTaskCount(patientId: String): Int {
        val (startDate, endDate) = getWeekDateRange()
        return dao.getWeekTaskCount(patientId, startDate, endDate)
    }
    
    /**
     * 获取患者本周已完成任务数
     */
    suspend fun getWeekCompletedCount(patientId: String): Int {
        val (startDate, endDate) = getWeekDateRange()
        return dao.getWeekCompletedCount(patientId, startDate, endDate)
    }
    
    /**
     * 计算本周完成率
     */
    suspend fun getWeekCompletionRate(patientId: String): Float {
        val total = getWeekTaskCount(patientId)
        if (total == 0) return 0f
        val completed = getWeekCompletedCount(patientId)
        return (completed.toFloat() / total.toFloat()) * 100f
    }
    
    // ==================== 状态管理 ====================
    
    /**
     * 标记任务为已完成
     */
    suspend fun completeInstance(instanceId: Long) {
        dao.updateInstanceStatus(instanceId, "completed", System.currentTimeMillis())
    }
    
    /**
     * 标记任务为跳过
     */
    suspend fun skipInstance(instanceId: Long) {
        dao.updateInstanceStatus(instanceId, "skipped", System.currentTimeMillis())
    }
    
    /**
     * 关联训练视频
     */
    suspend fun attachVideo(instanceId: Long, videoId: Long) {
        dao.updateInstanceVideo(instanceId, videoId, "waiting_review")
    }
    
    /**
     * 删除任务模板的所有实例
     */
    suspend fun deleteTaskInstances(taskId: Long) {
        dao.deleteInstancesByTask(taskId)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取今日日期字符串（yyyy-MM-dd）
     */
    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
    
    /**
     * 获取本周日期范围（周一到周日）
     */
    private fun getWeekDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 设置为本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = sdf.format(calendar.time)
        
        // 设置为本周日
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = sdf.format(calendar.time)
        
        return Pair(startDate, endDate)
    }
}



