package com.example.winsrehab

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.data.dao.TaskInstanceDao
import com.example.winsrehab.data.database.AppDatabase
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.data.repository.TaskExecutionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// TaskExecutionRepository：完成/跳过、状态守卫、回滚与幂等
@RunWith(AndroidJUnit4::class)
class TaskExecutionRepositoryTest {

    private lateinit var db: AppDatabase
    private val today = LocalDate.now()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedPlanWithTwoDailyTasks(): Long {
        val planId = db.rehabPlanDao().insertPlan(
            RehabPlan(
                patientId = "test001",
                doctorCode = "1",
                planName = "p",
                goal = "g",
                startDate = today.toString(),
                endDate = today.plusDays(6).toString(),
                status = "active",
                progress = 0
            )
        )
        db.trainingTaskDao().insertTasks(
            listOf(
                TrainingTask(
                    planId = planId,
                    taskName = "a",
                    taskType = "upper_limb",
                    suggestedDuration = 10,
                    frequency = "daily",
                    priority = "important"
                ),
                TrainingTask(
                    planId = planId,
                    taskName = "b",
                    taskType = "balance",
                    suggestedDuration = 20,
                    frequency = "daily",
                    priority = "normal"
                )
            )
        )
        return planId
    }

    private suspend fun insertInstance(taskId: Long, status: String): Long =
        db.taskInstanceDao().insertInstance(
            TaskInstance(
                taskId = taskId,
                patientId = "test001",
                scheduledDate = today.toString(),
                status = status
            )
        )

    @Test
    fun completeTaskUpdatesStatusAndProgress() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val instanceId = insertInstance(tasks[0].taskId, TaskStatus.PENDING)

        TaskExecutionRepository(db).completeTask(instanceId, today)

        val instance = db.taskInstanceDao().getInstanceByIdOnce(instanceId)!!
        assertEquals(TaskStatus.COMPLETED, instance.status)
        assertNotNull(instance.completedAt)
        assertEquals(50, db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress)
    }

    @Test
    fun skipTaskWritesTimestampButKeepsProgressZero() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val instanceId = insertInstance(tasks[0].taskId, TaskStatus.PENDING)

        TaskExecutionRepository(db).skipTask(instanceId, today)

        val instance = db.taskInstanceDao().getInstanceByIdOnce(instanceId)!!
        assertEquals(TaskStatus.SKIPPED, instance.status)
        assertNotNull(instance.completedAt)
        assertEquals(0, db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress)
    }

    @Test
    fun nonPendingTaskIsNotExecuted() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val reviewId = insertInstance(tasks[0].taskId, TaskStatus.WAITING_REVIEW)
        val rejectedId = insertInstance(tasks[1].taskId, TaskStatus.REJECTED)

        val repo = TaskExecutionRepository(db)
        repo.completeTask(reviewId, today)
        repo.skipTask(rejectedId, today)

        assertEquals(TaskStatus.WAITING_REVIEW, db.taskInstanceDao().getInstanceByIdOnce(reviewId)!!.status)
        assertEquals(TaskStatus.REJECTED, db.taskInstanceDao().getInstanceByIdOnce(rejectedId)!!.status)
        assertEquals(0, db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress)
    }

    @Test
    fun missingInstanceIsNoOp() = runBlocking {
        TaskExecutionRepository(db).completeTask(9999L, today)
        // 不抛异常即为通过
    }

    @Test
    fun updateFailureRollsBackStatusAndProgress() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val instanceId = insertInstance(tasks[0].taskId, TaskStatus.PENDING)

        val throwingDao = object : TaskInstanceDao by db.taskInstanceDao() {
            override suspend fun updateInstanceStatus(instanceId: Long, status: String, completedAt: Long?) {
                throw RuntimeException("boom")
            }
        }

        try {
            TaskExecutionRepository(db, throwingDao).completeTask(instanceId, today)
            fail("expected exception")
        } catch (e: RuntimeException) {
            // 预期
        }

        assertEquals(TaskStatus.PENDING, db.taskInstanceDao().getInstanceByIdOnce(instanceId)!!.status)
        assertEquals(0, db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress)
    }

    @Test
    fun duplicateCompleteIsIdempotent() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val instanceId = insertInstance(tasks[0].taskId, TaskStatus.PENDING)

        val repo = TaskExecutionRepository(db)
        repo.completeTask(instanceId, today)
        val firstProgress = db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress
        repo.completeTask(instanceId, today)
        assertEquals(firstProgress, db.rehabPlanDao().getPlanByIdOnce(planId)!!.progress)
    }

    @Test
    fun duplicateGenerationIsNotInsertedTwice() = runBlocking {
        val planId = seedPlanWithTwoDailyTasks()
        val tasks = db.trainingTaskDao().getTasksByPlanOnce(planId)
        val instance = TaskInstance(
            taskId = tasks[0].taskId,
            patientId = "test001",
            scheduledDate = today.toString(),
            status = TaskStatus.PENDING
        )
        val firstIds = db.taskInstanceDao().insertInstances(listOf(instance))
        val secondIds = db.taskInstanceDao().insertInstances(listOf(instance))

        assertTrue(firstIds.first() > 0)
        assertEquals(-1L, secondIds.first())
    }
}
