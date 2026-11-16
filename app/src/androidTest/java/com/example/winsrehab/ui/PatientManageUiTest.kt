package com.example.winsrehab.ui

import android.content.Context
import android.view.View
import android.widget.TextView
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
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.DoctorTask
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.ui.main.selectLog.SelectLogActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

// 患者管理页与医生端患者数一致性：路径 3（确认申请）+ 患者卡真实字段，两处患者数同口径
@RunWith(AndroidJUnit4::class)
class PatientManageUiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun seedData() {
        seedDoctor()
        seedPendingPatient()
        seedActivePatient()
    }

    // 路径 3：首页最新申请出现待办 → 确认 → 该条消失 + 待处理减 1 + 患者管理列表新增该患者 + 两处总数一致
    @Test
    fun confirmingApplicationUpdatesBothPatientCounts() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()
            // 待办区与横幅先有数据，才能取到非零基线
            waitForTextContains("新患者申请")

            val pendingBefore = intText(R.id.tvPendingCount)
            val patientsBefore = intText(R.id.tvPatientCount)
            assertTrue("前置条件：至少有一条待处理申请", pendingBefore >= 1)

            // 进患者申请页，确认我们造的那一条
            onView(withId(R.id.layoutNotification)).perform(click())
            waitForText(context.getString(R.string.patient_application_title))
            onView(
                allOf(withId(R.id.btnConfirm), isDescendantOfA(hasDescendant(withText(PENDING_NAME))))
            ).perform(click())

            // 该条申请从申请列表消失
            waitUntil(10_000L, "申请条目已消失") {
                onView(
                    allOf(withText(PENDING_NAME), isDescendantOfA(withId(R.id.recyclerView)))
                ).check(doesNotExist())
            }

            pressBack()
            waitForView(R.id.doctorHomeRoot)

            // 首页：待处理计数减 1，管理患者数加 1
            waitForIntText(R.id.tvPendingCount, pendingBefore - 1)
            waitForIntText(R.id.tvPatientCount, patientsBefore + 1)

            // 患者管理页：总数与首页一致，且确认后的患者真的进了列表
            clickBottomTab(TAB_PATIENTS)
            waitForSelectedTab(R.id.dt_pt_manageFragment)
            waitForText(context.getString(R.string.doctor_patient_manage_total, patientsBefore + 1))
            waitForAdapterCount(R.id.rv_patients, patientsBefore + 1)
            scrollListUntilText(R.id.rv_patients, PENDING_NAME)
        } finally {
            scenario.close()
        }
    }

    // 患者卡按真实字段展示：阶段徽标来自 rehabStage，进度来自 overallProgress，与预留默认值无关
    @Test
    fun patientCardShowsRealStageAndProgress() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()

            clickBottomTab(TAB_PATIENTS)
            waitForSelectedTab(R.id.dt_pt_manageFragment)
            waitForAdapterCount(R.id.rv_patients, 1)

            // 滚动到我们造的那位患者，卡上阶段与进度都得是库里的值
            scrollListUntilText(R.id.rv_patients, ACTIVE_NAME)
            onView(
                allOf(withText(ACTIVE_STAGE), isDescendantOfA(withId(R.id.rv_patients)))
            ).check(matches(isDisplayed()))
            onView(
                allOf(
                    withText(context.getString(R.string.patient_card_progress_value, ACTIVE_PROGRESS)),
                    isDescendantOfA(withId(R.id.rv_patients))
                )
            ).check(matches(isDisplayed()))
            onView(
                allOf(withText(ACTIVE_DIAGNOSIS), isDescendantOfA(withId(R.id.rv_patients)))
            ).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @After
    fun clearSeededTasks() {
        // 申请任务按患者删掉，避免残留待办污染其它用例
        val doctorTaskDao = MyApp.instance.database.doctorTaskDao()
        runBlocking {
            doctorTaskDao.deleteBindingTaskByPatient(PENDING_ID)
            doctorTaskDao.deleteBindingTaskByPatient(ACTIVE_ID)
        }
    }

    // 走真实登录入口：选医生 → 填工号密码 → 进医生主界面
    private fun loginAsDoctor() {
        onView(withId(R.id.cardDoctorLogin)).perform(click())
        waitForView(R.id.fieldDoctorId)
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

    private fun waitForAdapterCount(recyclerViewId: Int, minCount: Int) {
        waitUntil(10_000L, "recyclerView=$recyclerViewId count>=$minCount") {
            onView(withId(recyclerViewId)).check { view, _ ->
                val count = (view as RecyclerView).adapter?.itemCount ?: 0
                assertTrue("列表条数不足：实际 $count，期望至少 $minCount", count >= minCount)
            }
        }
    }

    // 患者可能排在列表下方，滚到能看见为止后再断言文案
    private fun scrollListUntilText(recyclerViewId: Int, text: String, maxSwipes: Int = 8) {
        val target = allOf(withText(text), isDescendantOfA(withId(recyclerViewId)))
        repeat(maxSwipes) {
            try {
                onView(target).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                // 还没滚到，继续往下翻
            } catch (e: AssertionError) {
                // 找到了但没完全露出来，也继续翻
            }
            onView(withId(recyclerViewId)).perform(swipeUp())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        onView(target).check(matches(isDisplayed()))
    }

    // 直接读 TextView 的数字，用于取基线
    private fun intText(viewId: Int): Int {
        var text = ""
        onView(withId(viewId)).check { view, _ -> text = (view as TextView).text.toString() }
        return text.trim().toInt()
    }

    private fun waitForIntText(viewId: Int, expected: Int) {
        waitUntil(10_000L, "text(viewId=$viewId)==$expected") {
            assertEquals("viewId=$viewId 数字不一致", expected, intText(viewId))
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

    private fun waitForTextContains(text: String, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "text contains=$text") {
            onView(withText(containsString(text))).check(matches(isDisplayed()))
        }
    }

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

    // 一位待确认患者 + 恰好一条待处理申请：重复跑也只会留下一条
    private fun seedPendingPatient() {
        val database = MyApp.instance.database
        val now = System.currentTimeMillis()
        runBlocking {
            val existing = database.patientDao().getPatientById(PENDING_ID)
            if (existing == null) {
                database.patientDao().insertPatient(
                    Patient(
                        patientId = PENDING_ID,
                        account = PENDING_ID,
                        password = PASSWORD,
                        name = PENDING_NAME,
                        gender = "女",
                        age = 52,
                        doctorCode = DOCTOR_CODE,
                        doctorName = "张医生",
                        diagnosis = "脑卒中后遗症",
                        rehabStage = "痉挛期",
                        bindingStatus = "pending",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                // 上一轮确认过会变成 active，这里复位成待确认，保证用例可重复跑
                database.patientDao().updatePatient(
                    existing.copy(bindingStatus = "pending", doctorCode = DOCTOR_CODE)
                )
            }

            database.doctorTaskDao().deleteBindingTaskByPatient(PENDING_ID)
            database.doctorTaskDao().insertTask(
                DoctorTask(
                    doctorCode = DOCTOR_CODE,
                    taskType = "patient_binding",
                    patientId = PENDING_ID,
                    patientName = PENDING_NAME,
                    patientAccount = PENDING_ID,
                    status = "pending",
                    createdAt = now
                )
            )
        }
    }

    // 一位已绑定患者，阶段与进度都赋值，用来验证卡片不是布局默认值
    private fun seedActivePatient() {
        val database = MyApp.instance.database
        val now = System.currentTimeMillis()
        runBlocking {
            val existing = database.patientDao().getPatientById(ACTIVE_ID)
            if (existing == null) {
                database.patientDao().insertPatient(
                    Patient(
                        patientId = ACTIVE_ID,
                        account = ACTIVE_ID,
                        password = PASSWORD,
                        name = ACTIVE_NAME,
                        gender = "男",
                        age = 60,
                        doctorCode = DOCTOR_CODE,
                        doctorName = "张医生",
                        diagnosis = ACTIVE_DIAGNOSIS,
                        rehabStage = ACTIVE_STAGE,
                        overallProgress = ACTIVE_PROGRESS,
                        bindingStatus = "active",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                database.patientDao().updatePatient(
                    existing.copy(
                        bindingStatus = "active",
                        doctorCode = DOCTOR_CODE,
                        rehabStage = ACTIVE_STAGE,
                        overallProgress = ACTIVE_PROGRESS
                    )
                )
            }
        }
    }

    private companion object {
        const val DOCTOR_CODE = "1"
        const val PASSWORD = "123456"

        // 待确认的患者：确认后应进入患者管理列表
        const val PENDING_ID = "test_manage_pending"
        const val PENDING_NAME = "患者管理待确认"

        // 已绑定患者：卡片字段断言用
        const val ACTIVE_ID = "test_manage_active"
        const val ACTIVE_NAME = "患者管理已绑定"
        const val ACTIVE_DIAGNOSIS = "脑外伤恢复期"
        const val ACTIVE_STAGE = "软瘫期"
        const val ACTIVE_PROGRESS = 37

        // 底部导航标题写在 res/menu/dt_nav_menu.xml 里，按用户可见文案点击
        const val TAB_PATIENTS = "患者管理"
    }
}
