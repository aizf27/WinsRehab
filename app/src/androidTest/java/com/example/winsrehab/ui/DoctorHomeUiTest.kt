package com.example.winsrehab.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotClickable
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.data.entity.RehabPlan
import com.example.winsrehab.data.model.PlanStatus
import com.example.winsrehab.ui.main.selectLog.SelectLogActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// 医生首页与计划列表入口的界面自动化：路径 1（首页真实数据与入口可用性）+ 路径 5（底部三 tab 与计划列表）
@RunWith(AndroidJUnit4::class)
class DoctorHomeUiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun seedData() {
        seedDoctor()
        seedPatientWithPlan()
        seedPendingApplication()
    }

    // 路径 1：启动 → 医生登录 → 医生首页，横幅与待办区接真实数据，两个入口可点、两个置灰点击无变化
    @Test
    fun doctorHomeShowsRealDataAndEntryStates() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()

            // 横幅与欢迎卡摘要都来自待办查询：至少有我们刚插入的一条申请
            waitForTextContains("新患者申请")
            onView(withId(R.id.layoutNotification)).check(matches(isDisplayed()))

            // 往下滚一屏，四宫格与待办列表才有足够可见面积接受点击
            onView(withId(R.id.homeScroll)).perform(swipeUp())

            // 四宫格：两个可用入口可点，两个待开放入口置灰
            onView(withId(R.id.cardPatientManage)).check(matches(isClickable()))
            onView(withId(R.id.cardCreatePlan)).check(matches(isClickable()))
            onView(withId(R.id.cardReviewVideo)).check(matches(isNotClickable()))
            onView(withId(R.id.cardFeedback)).check(matches(isNotClickable()))
            onView(
                allOf(
                    withText(R.string.doctor_home_coming_soon),
                    isDescendantOfA(withId(R.id.cardReviewVideo))
                )
            ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(
                allOf(
                    withText(R.string.doctor_home_coming_soon),
                    isDescendantOfA(withId(R.id.cardFeedback))
                )
            ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            // 点置灰入口不该跳转、不该弹提示：首页根容器仍然在
            onView(withId(R.id.cardReviewVideo)).perform(click())
            onView(withId(R.id.doctorHomeRoot)).check(matches(isDisplayed()))
            onView(withId(R.id.cardFeedback)).perform(click())
            onView(withId(R.id.doctorHomeRoot)).check(matches(isDisplayed()))

            // 待办区：列表有数据、空态隐藏，条目来自真实待办
            onView(withId(R.id.stateApplicationsEmpty)).check(
                matches(withEffectiveVisibility(Visibility.GONE))
            )
            waitForAdapterCount(R.id.rvApplications, 1)

            // 点待办条目进患者申请页，返回后仍在首页
            onView(withText(PATIENT_NAME)).perform(click())
            waitForText(context.getString(R.string.patient_application_title))
            pressBack()
            waitForView(R.id.doctorHomeRoot)
        } finally {
            scenario.close()
        }
    }

    // 路径 5：底部三 tab 切换与选中态，医生首页「制定计划」能进计划列表
    @Test
    fun doctorBottomNavAndPlanListEntry() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()

            // 三个 tab 依次切换，每次都要落成对应的选中态
            clickBottomTab(TAB_HOME)
            waitForSelectedTab(R.id.dt_homeFragment)

            clickBottomTab(TAB_PATIENTS)
            waitForSelectedTab(R.id.dt_pt_manageFragment)

            clickBottomTab(TAB_MINE)
            waitForSelectedTab(R.id.dt_info_Fragment)

            clickBottomTab(TAB_HOME)
            waitForSelectedTab(R.id.dt_homeFragment)

            // 首页「制定计划」进计划列表：有数据出列表、空态隐藏
            onView(withId(R.id.homeScroll)).perform(swipeUp())
            onView(withId(R.id.cardCreatePlan)).perform(click())
            waitForView(R.id.rv_patient_plans)
            waitForAdapterCount(R.id.rv_patient_plans, 1)
            onView(withId(R.id.statePlansEmpty)).check(
                matches(withEffectiveVisibility(Visibility.GONE))
            )
        } finally {
            scenario.close()
        }
    }

    @After
    fun clearSeededData() {
        val database = MyApp.instance.database
        runBlocking {
            database.doctorTaskDao().deleteBindingTaskByPatient(PATIENT_ID)
            database.rehabPlanDao().getPlansByPatient(PATIENT_ID).first()
                .filter { it.planName == PLAN_NAME }
                .forEach { database.rehabPlanDao().deletePlan(it) }
        }
    }

    // 走真实登录入口：选医生 → 填工号密码 → 进医生主界面
    private fun loginAsDoctor() {
        onView(withId(R.id.cardDoctorLogin)).perform(click())
        waitForView(R.id.fieldDoctorId)
        // 「记住密码」可能回填上次账号，先清空再填测试账号
        clearField(R.id.fieldDoctorId)
        typeInto(R.id.fieldDoctorId, DOCTOR_CODE)
        clearField(R.id.fieldDoctorPassword)
        typeInto(R.id.fieldDoctorPassword, PASSWORD)
        onView(withId(R.id.btnDoctorLogin)).perform(scrollTo(), click())
        waitForView(R.id.doctorHomeRoot)
    }

    private fun clickBottomTab(title: String) {
        onView(
            allOf(withText(title), isDescendantOfA(withId(R.id.bottom_nav)))
        ).perform(click())
    }

    // 底部导航的选中态只能从控件自身取，用自定义 matcher 断言
    private fun waitForSelectedTab(itemId: Int) {
        waitUntil(10_000L, "bottomNav selected=$itemId") {
            onView(withId(R.id.bottom_nav)).check(matches(selectedItem(itemId)))
        }
    }

    private fun selectedItem(itemId: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("BottomNavigationView selectedItemId=$itemId")
        }

        override fun matchesSafely(view: View): Boolean {
            return view is BottomNavigationView && view.selectedItemId == itemId
        }
    }

    // 列表是否真的拿到了数据，看适配器条数，避免依赖条目是否滚动到屏幕内
    private fun waitForAdapterCount(recyclerViewId: Int, minCount: Int) {
        waitUntil(10_000L, "recyclerView=$recyclerViewId count>=$minCount") {
            onView(withId(recyclerViewId)).check { view, _ ->
                val count = (view as RecyclerView).adapter?.itemCount ?: 0
                assertTrue("列表条数不足：实际 $count，期望至少 $minCount", count >= minCount)
            }
        }
    }

    private fun typeInto(fieldId: Int, value: String) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(value), closeSoftKeyboard())
    }

    private fun clearField(fieldId: Int) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(""), closeSoftKeyboard())
    }

    private fun waitForView(viewId: Int, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "viewId=$viewId") {
            onView(withId(viewId)).check(matches(isDisplayed()))
        }
    }

    private fun waitForText(text: String, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "text=$text") {
            onView(withText(text)).check(matches(isDisplayed()))
        }
    }

    // 文案带变量时用包含匹配，不绑死具体数字
    private fun waitForTextContains(text: String, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "text contains=$text") {
            onView(withText(containsString(text))).check(matches(isDisplayed()))
        }
    }

    // 轮询直到断言通过，期间保持界面空闲同步
    private fun waitUntil(timeoutMs: Long, description: String, assertion: () -> Unit) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastFailure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                assertion()
                return
            } catch (e: NoMatchingViewException) {
                lastFailure = e
            } catch (e: AssertionError) {
                lastFailure = e
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(200)
        }
        org.junit.Assert.fail("等待界面超时：$description，耗时 ${timeoutMs}ms，最后一次失败：$lastFailure")
    }

    // 医生工号 1 用测试账号，已有就跳过，保证登录能过
    private fun seedDoctor() {
        val doctorDao = MyApp.instance.database.doctorDao()
        runBlocking {
            if (doctorDao.getDoctorByCode(DOCTOR_CODE) == null) {
                doctorDao.insertDoctorInfo(
                    Doctor(
                        doctorCode = DOCTOR_CODE,
                        password = PASSWORD,
                        name = "张医生",
                        gender = "男",
                        age = 35,
                        title = "主治医师",
                        department = "康复科",
                        hospital = "市人民医院",
                        phone = "13800138001",
                        email = "doctor1@hospital.com",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // 患者 + 一条 active 计划：计划列表只显示「有计划的患者」，两者必须一起造
    private fun seedPatientWithPlan() {
        val database = MyApp.instance.database
        val now = System.currentTimeMillis()
        runBlocking {
            if (database.patientDao().getPatientById(PATIENT_ID) == null) {
                database.patientDao().insertPatient(
                    Patient(
                        patientId = PATIENT_ID,
                        account = PATIENT_ID,
                        password = PASSWORD,
                        name = PATIENT_NAME,
                        gender = "男",
                        age = 45,
                        doctorCode = DOCTOR_CODE,
                        doctorName = "张医生",
                        diagnosis = "脑卒中后遗症",
                        rehabStage = "恢复期",
                        bindingStatus = "active",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            val existing = database.rehabPlanDao().getPlansByPatient(PATIENT_ID).first()
            if (existing.none { it.planName == PLAN_NAME }) {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                database.rehabPlanDao().insertPlan(
                    RehabPlan(
                        patientId = PATIENT_ID,
                        doctorCode = DOCTOR_CODE,
                        planName = PLAN_NAME,
                        goal = "恢复上肢抬举功能",
                        startDate = formatter.format(Date(now)),
                        endDate = formatter.format(Date(now + 30L * 24 * 60 * 60 * 1000)),
                        status = PlanStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    // 一条待处理绑定申请，让首页横幅与待办列表都有真实数据
    private fun seedPendingApplication() {
        val now = System.currentTimeMillis()
        runBlocking {
            MyApp.instance.database.doctorTaskDao().insertTask(
                DoctorTask(
                    doctorCode = DOCTOR_CODE,
                    taskType = "patient_binding",
                    patientId = PATIENT_ID,
                    patientName = PATIENT_NAME,
                    patientAccount = PATIENT_ID,
                    status = "pending",
                    createdAt = now
                )
            )
        }
    }

    private companion object {
        const val DOCTOR_CODE = "1"
        const val PATIENT_ID = "test001"
        const val PATIENT_NAME = "测试患者001"
        const val PASSWORD = "123456"
        const val PLAN_NAME = "首页入口测试计划"

        // 底部导航标题写在 res/menu/dt_nav_menu.xml 里，按用户可见文案点击
        const val TAB_HOME = "首页"
        const val TAB_PATIENTS = "患者管理"
        const val TAB_MINE = "我的"
    }
}
