package com.example.winsrehab.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
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
import com.example.winsrehab.ui.main.doctor.application.PatientApplicationActivity
import com.example.winsrehab.ui.main.doctor.login.DtLogActivity
import com.example.winsrehab.ui.main.patient.binding.WaitingBindingActivity
import com.example.winsrehab.ui.main.patient.login.PtLogActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// 补充冒烟：等待绑定页、患者申请页、医生与患者的信息编辑页，确认可启动、关键控件可见
@RunWith(AndroidJUnit4::class)
class ExtraPagesSmokeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // 两条登录路径都依赖这两个本地种子，先保证数据在
    @Before
    fun seedData() {
        seedDoctor()
        seedActivePatient()
    }

    // 等待绑定页：带账号直启，状态标题与状态徽标要出现在页面上
    @Test
    fun waitingBindingPageShowsStateSkeleton() {
        val intent = Intent(context, WaitingBindingActivity::class.java)
            .putExtra("account", PATIENT_ACCOUNT)
        val scenario = ActivityScenario.launch<WaitingBindingActivity>(intent)
        try {
            waitForView(R.id.tvStateTitle)
            onView(withId(R.id.statusBadge)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    // 患者申请页：带医生工号直启，顶部栏必须可见
    @Test
    fun patientApplicationPageShowsTopBar() {
        val intent = Intent(context, PatientApplicationActivity::class.java)
            .putExtra("doctorCode", DOCTOR_CODE)
        val scenario = ActivityScenario.launch<PatientApplicationActivity>(intent)
        try {
            onView(withId(R.id.topBar)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    // 医生信息编辑页：登录后从「我的」点「编辑」进入，科室等编辑行要可见
    @Test
    fun doctorInfoEditPageOpensFromDoctorInfo() {
        val scenario = ActivityScenario.launch(DtLogActivity::class.java)
        try {
            typeInto(R.id.fieldDoctorId, DOCTOR_CODE)
            typeInto(R.id.fieldDoctorPassword, PASSWORD)
            onView(withId(R.id.btnDoctorLogin)).perform(scrollTo(), click())

            waitForView(R.id.doctorHomeRoot)
            clickBottomTab(TAB_ME)
            waitForView(R.id.tvDoctorName)
            onView(withId(R.id.btnSave)).perform(scrollTo(), click())

            waitForView(R.id.layoutDepartment)
        } finally {
            scenario.close()
        }
    }

    // 患者信息编辑页：登录后从「个人中心」点「编辑」进入，昵称等编辑行要可见
    @Test
    fun patientInfoEditPageOpensFromPatientInfo() {
        val scenario = ActivityScenario.launch(PtLogActivity::class.java)
        try {
            typeInto(R.id.fieldAccount, PATIENT_ACCOUNT)
            typeInto(R.id.fieldPassword, PASSWORD)
            onView(withId(R.id.btnPtLogin)).perform(scrollTo(), click())

            waitForView(R.id.patient_main_root)
            clickBottomTab(TAB_PERSONAL)
            waitForView(R.id.tvPatientName)
            onView(withId(R.id.btnEdit)).perform(scrollTo(), click())

            waitForView(R.id.layoutNickname)
        } finally {
            scenario.close()
        }
    }

    // 同一套输入框组件内层 id 相同，靠外层输入框定位到具体那一格
    private fun typeInto(fieldId: Int, value: String) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(value), closeSoftKeyboard())
    }

    // 底部导航切页：按 tab 文案点，tab 一定在底栏容器里
    private fun clickBottomTab(title: String) {
        onView(allOf(withText(title), isDescendantOfA(withId(R.id.bottom_nav))))
            .perform(click())
    }

    // 异步加载页统一轮询等控件出现，超时带上最后一次失败原因
    private fun waitForView(viewId: Int, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastFailure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                lastFailure = e
            } catch (e: AssertionError) {
                lastFailure = e
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(200)
        }
        fail("等待界面超时：viewId=$viewId，耗时 ${timeoutMs}ms，最后一次失败：$lastFailure")
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

    // 已绑定患者：patientId 必须等于登录账号，临床端才查得到
    private fun seedActivePatient() {
        val patientDao = MyApp.instance.database.patientDao()
        val now = System.currentTimeMillis()
        runBlocking {
            val existing = patientDao.getPatientById(PATIENT_ACCOUNT)
            if (existing == null) {
                patientDao.insertPatient(
                    Patient(
                        patientId = PATIENT_ACCOUNT,
                        account = PATIENT_ACCOUNT,
                        password = PASSWORD,
                        name = "冒烟患者",
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
                patientDao.updatePatient(
                    existing.copy(bindingStatus = "active", doctorCode = DOCTOR_CODE)
                )
            }
        }
    }

    private companion object {
        const val DOCTOR_CODE = "1"
        const val PASSWORD = "123456"
        const val PATIENT_ACCOUNT = "test001"

        const val TAB_ME = "我的"
        const val TAB_PERSONAL = "个人中心"
    }
}
