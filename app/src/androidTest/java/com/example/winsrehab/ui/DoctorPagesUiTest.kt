package com.example.winsrehab.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Doctor
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.ui.main.selectLog.SelectLogActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

// 医生端页面接线：路径 2（患者卡 → 创建计划页带对患者）+ 我的页患者数与患者管理页同口径
@RunWith(AndroidJUnit4::class)
class DoctorPagesUiTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun seedData() {
        seedDoctor()
        seedActivePatient()
    }

    // 路径 2：患者管理点患者卡 → 创建计划页顶部显示的必须是这位患者
    @Test
    fun patientCardOpensCreatePlanWithSamePatient() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()

            clickBottomTab(TAB_PATIENTS)
            waitForSelectedTab(R.id.dt_pt_manageFragment)
            waitForAdapterCount(R.id.rv_patients)

            // 点患者姓名所在的卡片，触摸会落到卡片本身的点击监听
            scrollListUntilText(R.id.rv_patients, PATIENT_NAME)
            onView(allOf(withText(PATIENT_NAME), isDescendantOfA(withId(R.id.rv_patients))))
                .perform(click())

            // 创建计划页带的是同一位患者，且页面确实起来了
            waitUntil(10_000L, "创建计划页显示患者姓名") {
                onView(withId(R.id.tv_patient_name)).check(matches(withText(PATIENT_NAME)))
            }
        } finally {
            scenario.close()
        }
    }

    // 我的页的「管理患者数」与患者管理页的「共 N 位患者」必须一致
    @Test
    fun infoPagePatientCountMatchesManagePage() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            loginAsDoctor()

            // 我的页：患者数来自首页同一份患者列表，等它从占位符换成数字
            clickBottomTab(TAB_ME)
            waitForSelectedTab(R.id.dt_info_Fragment)
            val mineCount = waitForPatientCountText()

            // 患者管理页：总数由同一个 patientDao 查询得出
            clickBottomTab(TAB_PATIENTS)
            waitForSelectedTab(R.id.dt_pt_manageFragment)
            waitForAdapterCount(R.id.rv_patients)
            val manageCount = waitForManageTotal()

            assertEquals("我的页患者数与患者管理页总数不一致", manageCount, mineCount)
        } finally {
            scenario.close()
        }
    }

    // 我的页患者数形如「3 人」，占位符「--」时继续等
    private fun waitForPatientCountText(): Int {
        var result = -1
        waitUntil(10_000L, "我的页患者数变成数字") {
            var text = ""
            onView(
                allOf(withId(R.id.tvInfoValue), isDescendantOfA(withId(R.id.row_patient_count)))
            ).check { view, _ -> text = (view as TextView).text.toString() }
            result = text.filter { it.isDigit() }.toIntOrNull()
                ?: throw AssertionError("患者数还不是数字：$text")
        }
        return result
    }

    // 患者管理页总数形如「共 3 位患者」
    private fun waitForManageTotal(): Int {
        var result = -1
        waitUntil(10_000L, "患者管理页总数可解析") {
            var text = ""
            onView(withId(R.id.tv_patient_total)).check { view, _ ->
                text = (view as TextView).text.toString()
            }
            result = text.filter { it.isDigit() }.toIntOrNull()
                ?: throw AssertionError("总数还不是数字：$text")
        }
        return result
    }

    // 走真实登录入口：选医生 → 填工号密码 → 进医生主界面
    private fun loginAsDoctor() {
        onView(withId(R.id.cardDoctorLogin)).perform(click())
        waitForView(R.id.fieldDoctorId)
        typeInto(R.id.fieldDoctorId, DOCTOR_CODE)
        typeInto(R.id.fieldDoctorPassword, PASSWORD)
        onView(withId(R.id.btnDoctorLogin)).perform(scrollTo(), click())
        waitForView(R.id.doctorHomeRoot)
    }

    // 底栏有大小两个同文案 TextView，统一走共享动作按菜单项选中
    private fun clickBottomTab(title: String) {
        clickBottomTabItem(title)
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

    private fun waitForAdapterCount(recyclerViewId: Int) {
        waitUntil(10_000L, "recyclerView=$recyclerViewId 有数据") {
            onView(withId(recyclerViewId)).check { view, _ ->
                val count = (view as RecyclerView).adapter?.itemCount ?: 0
                if (count < 1) throw AssertionError("列表为空")
            }
        }
    }

    // 患者可能排在列表下方，滚到能看见为止
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

    private fun typeInto(fieldId: Int, value: String) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(value), closeSoftKeyboard())
    }

    private fun waitForView(viewId: Int, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "viewId=$viewId") {
            onView(withId(viewId)).check(matches(isDisplayed()))
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

    // 医生工号 1 用测试账号，已有就跳过
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

    // 一位已绑定患者：患者管理列表有卡片可点，创建计划页有患者可显示
    private fun seedActivePatient() {
        val patientDao = MyApp.instance.database.patientDao()
        val now = System.currentTimeMillis()
        runBlocking {
            val existing = patientDao.getPatientById(PATIENT_ID)
            if (existing == null) {
                patientDao.insertPatient(
                    Patient(
                        patientId = PATIENT_ID,
                        account = PATIENT_ID,
                        password = PASSWORD,
                        name = PATIENT_NAME,
                        gender = "男",
                        age = 58,
                        doctorCode = DOCTOR_CODE,
                        doctorName = "张医生",
                        diagnosis = "脑卒中恢复期",
                        rehabStage = "恢复期",
                        bindingStatus = "active",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                // 复跑时复位成已绑定，避免前一轮改坏
                patientDao.updatePatient(
                    existing.copy(bindingStatus = "active", doctorCode = DOCTOR_CODE)
                )
            }
        }
    }

    private companion object {
        const val DOCTOR_CODE = "1"
        const val PASSWORD = "123456"

        const val PATIENT_ID = "test_pages_active"
        const val PATIENT_NAME = "页面接线患者"

        const val TAB_PATIENTS = "患者管理"
        const val TAB_ME = "我的"
    }
}
