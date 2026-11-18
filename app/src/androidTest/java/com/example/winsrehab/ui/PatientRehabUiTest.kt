package com.example.winsrehab.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.entity.TaskInstance
import com.example.winsrehab.data.entity.TrainingTask
import com.example.winsrehab.data.model.PlanStatus
import com.example.winsrehab.data.model.TaskStatus
import com.example.winsrehab.ui.main.patient.main.PtMainActivity
import com.google.android.material.card.MaterialCardView
import com.example.winsrehab.utils.DateRanges
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.containsString
import java.time.LocalDate

// 患者首页与康复中心共用周数据：完成计数一致，跳过不计入完成。
@RunWith(AndroidJUnit4::class)
class PatientRehabUiTest {

    @Test
    fun completeAndSkipTasksKeepHomeAndRehabCountsConsistent() {
        val account = "ui_rehab_${System.currentTimeMillis()}"
        seedTodayTasks(account)
        val scenario = ActivityScenario.launch<PtMainActivity>(
            Intent(MyApp.instance, PtMainActivity::class.java).putExtra("account", account)
        )
        try {
            onView(allOf(withId(R.id.pt_rehabFragment), isDescendantOfA(withId(R.id.bottom_nav))))
                .perform(click())
            onView(withId(R.id.rvTodayTasks)).check(matches(isDisplayed()))
            // 父容器落到卡片本身，否则整个列表都算祖先，两张卡的按钮会一起命中
            onView(
                allOf(
                    withId(R.id.btnStartTask),
                    isDescendantOfA(
                        allOf(
                            isAssignableFrom(MaterialCardView::class.java),
                            hasDescendant(withText("完成任务"))
                        )
                    )
                )
            ).perform(click())
            onView(withText("完成")).perform(click())
            onView(
                allOf(
                    withId(R.id.btnStartTask),
                    isDescendantOfA(
                        allOf(
                            isAssignableFrom(MaterialCardView::class.java),
                            hasDescendant(withText("跳过任务"))
                        )
                    )
                )
            ).perform(click())
            onView(withText("跳过")).perform(click())
            // 实际文案是「1/2 项已完成」，用包含匹配避免绑死后缀
            onView(withId(R.id.tvProgressCount)).check(matches(withText(containsString("1/2"))))

            onView(allOf(withId(R.id.pt_homeFragment), isDescendantOfA(withId(R.id.bottom_nav))))
                .perform(click())
            onView(withId(R.id.tvTodayTasks)).check(matches(withText("1/2")))
            onView(withId(R.id.tvWeekProgressPercent)).check(matches(withText(containsString("50%"))))
        } finally {
            scenario.close()
        }
    }

    private fun seedTodayTasks(account: String) = runBlocking {
        val db = MyApp.instance.database
        val today = LocalDate.now()
        val (_, weekEnd) = DateRanges.weekRange(today)
        db.patientDao().insertPatient(
            Patient(
                patientId = account,
                account = account,
                password = "123456",
                name = "康复测试患者",
                gender = "男",
                age = 50,
                bindingStatus = "active"
            )
        )
        val planId = db.rehabPlanDao().insertPlan(
            RehabPlan(
                patientId = account,
                doctorCode = "1",
                planName = "界面测试计划",
                goal = "验证同源数据",
                startDate = today.toString(),
                endDate = weekEnd.toString(),
                status = PlanStatus.ACTIVE
            )
        )
        val taskIds = db.trainingTaskDao().insertTasks(
            listOf(
                TrainingTask(planId = planId, taskName = "完成任务", taskType = "upper_limb", suggestedDuration = 10, frequency = "daily"),
                TrainingTask(planId = planId, taskName = "跳过任务", taskType = "upper_limb", suggestedDuration = 10, frequency = "daily")
            )
        )
        db.taskInstanceDao().insertInstances(
            taskIds.map { taskId ->
                TaskInstance(taskId = taskId, patientId = account, scheduledDate = today.toString(), status = TaskStatus.PENDING)
            }
        )
    }
}
