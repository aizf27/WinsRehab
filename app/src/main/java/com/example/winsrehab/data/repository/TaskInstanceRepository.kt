package com.example.winsrehab.data.repository

import android.util.Log
import com.example.winsrehab.data.dao.TaskInstanceDao
import com.example.winsrehab.data.dao.RehabPlanDao
import com.example.winsrehab.data.dao.TrainingTaskDao
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.utils.PlanDates
import com.example.winsrehab.utils.TaskSchedule
import kotlinx.coroutines.flow.Flow

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

            // 目标日期解析失败就没法判断频率，直接返回
            val targetDate = TaskSchedule.parseDate(date)
            if (targetDate == null) {
                Log.e(TAG, "生成日期格式非法: $date")
                return 0
            }
            val dateKey = targetDate.toString()  // 统一成 yyyy-MM-dd 再入库，避免日期字符串带空格

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
                // 计划开始日期非法，整个计划跳过，避免 interval 从错误起点算
                val planStartDate = TaskSchedule.parseDate(plan.startDate)
                if (planStartDate == null) {
                    Log.e(TAG, "计划[${plan.planName}]开始日期非法: ${plan.startDate}，跳过")
                    continue
                }

                // 计划时间窗：只在开始日到结束日之间生成；未开始或已到期直接跳过该计划
                // 结束日非法同样视为出窗（PlanDates.isWithinPlanWindow 解析失败返回 false）
                if (!PlanDates.isWithinPlanWindow(plan.startDate, plan.endDate, dateKey)) {
                    Log.i(TAG, "计划[${plan.planName}]日期${dateKey}不在窗口[${plan.startDate}~${plan.endDate}]内，跳过")
                    continue
                }

                val tasks = trainingTaskDao.getTasksByPlanOnce(plan.planId)
                totalTasks += tasks.size
                Log.i(TAG, "计划[${plan.planName}]有${tasks.size}个任务模板")

                for (task in tasks) {
                    // 4. 检查是否已存在（去重）
                    if (dao.isInstanceExists(patientId, task.taskId, dateKey)) {
                        skippedExists++
                        Log.d(TAG, "任务[${task.taskName}]已存在，跳过")
                        continue
                    }

                    // 5. 判断频率：只依赖任务模板、计划开始日期、目标日期，不查历史
                    val shouldGenerate = TaskSchedule.shouldGenerate(
                        task = task,
                        planStartDate = planStartDate,
                        date = targetDate
                    )

                    if (shouldGenerate) {
                        // 6. 构建TaskInstance对象
                        val instance = TaskInstance(
                            taskId = task.taskId,
                            patientId = patientId,
                            scheduledDate = dateKey,
                            status = TaskStatus.PENDING
                        )
                        instancesToGenerate.add(instance)
                        Log.i(TAG, "任务[${task.taskName}]满足频率规则，准备生成")
                    } else {
                        skippedFrequency++
                        Log.d(TAG, "任务[${task.taskName}]不满足频率规则，跳过")
                    }
                }
            }

            // 7. 批量插入
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

            // 8. 性能统计
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

    // ==================== 状态管理 ====================

    /**
     * 关联训练视频
     */
    suspend fun attachVideo(instanceId: Long, videoId: Long) {
        dao.updateInstanceVideo(instanceId, videoId, TaskStatus.WAITING_REVIEW)
    }
    
    /**
     * 删除任务模板的所有实例
     */
    suspend fun deleteTaskInstances(taskId: Long) {
        dao.deleteInstancesByTask(taskId)
    }
    
    // ==================== 辅助方法 ====================

    // 已移除：本周区间计算收敛到 utils/DateRanges，今日/本周日期助手不再被使用
}



