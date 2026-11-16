package com.example.winsrehab

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.data.database.AppDatabase
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.data.repository.TaskExecutionRepository
import com.example.winsrehab.utils.DateRanges
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// 康复页本周查询：字段齐全、重要在前、7 桶统计与周汇总同源
@RunWith(AndroidJUnit4::class)
class RehabQueryTest {

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

    private suspend fun seed() {
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
        val ids = db.trainingTaskDao().insertTasks(
            listOf(
                TrainingTask(
                    planId = planId,
                    taskName = "important-task",
                    taskType = "upper_limb",
                    suggestedDuration = 10,
                    frequency = "daily",
                    priority = "important"
                ),
                TrainingTask(
                    planId = planId,
                    taskName = "normal-task",
                    taskType = "balance",
                    suggestedDuration = 20,
                    frequency = "daily",
                    priority = "normal"
                )
            )
        )
        db.taskInstanceDao().insertInstances(
            listOf(
                TaskInstance(taskId = ids[0], patientId = "test001", scheduledDate = today.toString(), status = TaskStatus.COMPLETED),
                TaskInstance(taskId = ids[1], patientId = "test001", scheduledDate = today.toString(), status = TaskStatus.PENDING)
            )
        )
    }

    @Test
    fun weekQueryReturnsFieldsAndOrdersImportantFirst() = runBlocking {
        seed()
        val data = TaskExecutionRepository(db).observeRehabWeek("test001").first()

        assertEquals(today.toString(), data.today)
        assertEquals(2, data.tasks.size)
        assertNotNull(data.tasks.first().taskName)
        assertEquals("important", data.tasks.first().priority)
        // 两个任务时长分别是 10 与 20 分钟，合计 30 用来确认查询把时长字段带出来了
        assertEquals(30, data.tasks.first().suggestedDuration + data.tasks.last().suggestedDuration)
        assertTrue(data.weekStart <= today.toString())
        assertTrue(data.weekEnd >= today.toString())
    }

    @Test
    fun weekBucketsSumToWeekTotalWithEmptyDays() = runBlocking {
        seed()
        val data = TaskExecutionRepository(db).observeRehabWeek("test001").first()
        val (start, _) = DateRanges.weekRange(today)

        // 7 个自然日分桶，验证空桶存在且总量守恒（与 ViewModel 同源口径）
        val buckets = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong()).toString()
            date to data.tasks.count { it.instance.scheduledDate == date }
        }

        assertEquals(7, buckets.size)
        assertEquals(data.tasks.size, buckets.sumOf { it.second })
        assertTrue(buckets.any { it.second == 0 })
    }
}
