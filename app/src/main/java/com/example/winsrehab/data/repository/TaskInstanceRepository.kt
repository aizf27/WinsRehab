package com.example.winsrehab.data.repository

import android.util.Log
import com.example.winsrehab.data.dao.TaskInstanceDao
import com.example.winsrehab.data.dao.RehabPlanDao
import com.example.winsrehab.data.dao.TrainingTaskDao
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.utils.TaskGenerationHelper
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务实例Repository
 *
 * 封装任务实例相关的业务逻辑，包括任务生成、查询、状态管理等
 */
class TaskInstanceRepository(
    private val dao: TaskInstanceDao,
    private val rehabPlanDao: RehabPlanDao,
    private val trainingTaskDao: TrainingTaskDao
) {

    companion object {
        private const val TAG = "TaskInstanceRepository"
    }
    
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
     * 为患者生成指定日期的任务
     *
     * @param patientId 患者ID
     * @param date 日期（yyyy-MM-dd格式）
     * @return 生成的任务实例数量
     */
    suspend fun generateDailyTasks(patientId: String, date: String): Int {
        val startTime = System.currentTimeMillis()

        try {
            Log.i(TAG, "开始生成任务: patientId=$patientId, date=$date")

            // 1. 获取患者的所有活跃康复计划
            val activePlans = rehabPlanDao.getActivePlansByPatientOnce(patientId)
            if (activePlans.isEmpty()) {
                Log.i(TAG, "患者无活跃康复计划，无需生成任务")
                return 0
            }
            Log.i(TAG, "找到${activePlans.size}个活跃计划")

            // 2. 收集所有待生成的任务实例
            val instancesToGenerate = mutableListOf<TaskInstance>()
            var totalTasks = 0
            var skippedExists = 0
            var skippedFrequency = 0

            // 3. 遍历每个计划的任务模板
            for (plan in activePlans) {
                val tasks = trainingTaskDao.getTasksByPlanOnce(plan.planId)
                totalTasks += tasks.size
                Log.i(TAG, "计划[${plan.planName}]有${tasks.size}个任务模板")

                for (task in tasks) {
                    // 4. 检查是否已存在（去重）
                    if (dao.isInstanceExists(patientId, task.taskId, date)) {
                        skippedExists++
                        Log.d(TAG, "任务[${task.taskName}]已存在，跳过")
                        continue
                    }

                    // 5. 获取上次生成日期（用于interval频率）
                    val lastScheduledDate = dao.getLastScheduledDate(task.taskId, patientId)

                    // 6. 判断是否满足频率规则
                    val shouldGenerate = TaskGenerationHelper.shouldGenerateToday(
                        task = task,
                        date = date,
                        lastScheduledDate = lastScheduledDate
                    )

                    if (shouldGenerate) {
                        // 7. 构建TaskInstance对象
                        val instance = TaskInstance(
                            taskId = task.taskId,
                            patientId = patientId,
                            scheduledDate = date,
                            status = "pending"
                        )
                        instancesToGenerate.add(instance)
                        Log.i(TAG, "任务[${task.taskName}]满足频率规则，准备生成")
                    } else {
                        skippedFrequency++
                        Log.d(TAG, "任务[${task.taskName}]不满足频率规则，跳过")
                    }
                }
            }

            // 8. 批量插入
            val generatedCount = if (instancesToGenerate.isNotEmpty()) {
                val insertedIds = dao.insertInstances(instancesToGenerate)
                // 统计实际插入的数量（IGNORE策略下，冲突的会返回-1）
                val actualInserted = insertedIds.count { it > 0 }
                Log.i(TAG, "批量插入${instancesToGenerate.size}个任务，实际成功${actualInserted}个")
                actualInserted
            } else {
                Log.i(TAG, "没有需要生成的任务")
                0
            }

            // 9. 性能统计
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "任务生成完成: 总计${totalTasks}个任务模板, 已存在${skippedExists}个, 频率不符${skippedFrequency}个, 新生成${generatedCount}个, 耗时${elapsed}ms")

            return generatedCount

        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "生成任务失败: patientId=$patientId, date=$date, 耗时${elapsed}ms", e)
            // 不抛出异常，避免影响登录流程
            return 0
        }
    }

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



