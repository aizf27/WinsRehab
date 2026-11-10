package com.example.winsrehab.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.winsrehab.data.dao.RehabPlanDao
import com.example.winsrehab.data.dao.TaskInstanceDao
import com.example.winsrehab.data.dao.TrainingTaskDao
import com.example.winsrehab.data.database.AppDatabase
import com.example.winsrehab.data.model.RehabWeekData
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.utils.DateRanges
import com.example.winsrehab.utils.PlanProgress
import com.example.winsrehab.utils.TaskSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * 任务执行仓库：完成/跳过 + 计划进度重算，是计划进度的唯一写入口
 *
 * 构造允许注入 DAO，测试可传装饰器；db 用于 withTransaction 事务边界。
 */
class TaskExecutionRepository(
    private val db: AppDatabase,
    private val instanceDao: TaskInstanceDao = db.taskInstanceDao(),
    private val trainingTaskDao: TrainingTaskDao = db.trainingTaskDao(),
    private val planDao: RehabPlanDao = db.rehabPlanDao()
) {

    companion object {
        private const val TAG = "TaskExecutionRepository"
    }

    // 标记完成：改状态 + 重算计划进度，同一事务
    suspend fun completeTask(instanceId: Long, today: LocalDate = LocalDate.now()) {
        updateTaskStatus(instanceId, TaskStatus.COMPLETED, today)
    }

    // 标记跳过：与完成走同一事务与重算，但不改变进度分子
    suspend fun skipTask(instanceId: Long, today: LocalDate = LocalDate.now()) {
        updateTaskStatus(instanceId, TaskStatus.SKIPPED, today)
    }

    // 康复页数据源：每次订阅都重新取「今天」与周区间，跨零点/跨周恢复不会沿用旧区间
    fun observeRehabWeek(patientId: String): Flow<RehabWeekData> = flow {
        val today = LocalDate.now()
        val (start, end) = DateRanges.weekRange(today)
        emitAll(
            instanceDao.getWeekInstancesWithTask(patientId, start.toString(), end.toString())
                .map { tasks -> RehabWeekData(today.toString(), start.toString(), end.toString(), tasks) }
        )
    }

    private suspend fun updateTaskStatus(instanceId: Long, targetStatus: String, today: LocalDate) {
        db.withTransaction {
            val instance = instanceDao.getInstanceByIdOnce(instanceId)
            if (instance == null) {
                Log.e(TAG, "任务实例不存在: $instanceId")
                return@withTransaction
            }
            // 只有待完成任务可被执行；待审核/已驳回/终态一律早退，避免污染进度分子
            if (instance.status != TaskStatus.PENDING) {
                Log.i(TAG, "任务[$instanceId]状态为${instance.status}，不可执行，忽略")
                return@withTransaction
            }

            instanceDao.updateInstanceStatus(instanceId, targetStatus, System.currentTimeMillis())

            // 顺着任务找到计划，再重算进度；脏数据只降级不中断
            val task = trainingTaskDao.getTaskByIdOnce(instance.taskId)
            if (task == null) {
                Log.e(TAG, "任务模板不存在: ${instance.taskId}，跳过进度重算")
                return@withTransaction
            }
            val plan = planDao.getPlanByIdOnce(task.planId)
            if (plan == null) {
                Log.e(TAG, "计划不存在: ${task.planId}，跳过进度重算")
                return@withTransaction
            }

            val startDate = TaskSchedule.parseDate(plan.startDate)
            val endDate = TaskSchedule.parseDate(plan.endDate)
            if (startDate == null || endDate == null) {
                Log.e(TAG, "计划日期非法: ${plan.startDate}~${plan.endDate}，跳过进度重算")
                return@withTransaction
            }

            val tasks = trainingTaskDao.getTasksByPlanOnce(plan.planId)
            val due = PlanProgress.computeDueCount(tasks, startDate, endDate, today)
            val windowEnd = minOf(today, endDate)
            val completed = if (windowEnd.isBefore(startDate)) {
                0
            } else {
                instanceDao.getCompletedCountInWindow(
                    plan.planId,
                    instance.patientId,
                    startDate.toString(),
                    windowEnd.toString()
                )
            }
            val percent = PlanProgress.computePercent(completed, due)
            planDao.updatePlanProgress(plan.planId, percent)
            Log.i(TAG, "任务[$instanceId] -> $targetStatus，计划[${plan.planId}]进度=$percent ($completed/$due)")
        }
    }
}
