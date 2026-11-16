package com.example.winsrehab.ui

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.winsrehab.MyApp
import com.example.winsrehab.R
import com.example.winsrehab.data.entity.Patient
import com.example.winsrehab.ui.main.doctor.login.DtLogActivity
import com.example.winsrehab.ui.main.patient.login.PtLogActivity
import com.example.winsrehab.ui.main.patient.main.PtMainActivity
import com.example.winsrehab.ui.main.selectLog.SelectLogActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

// 入口与身份页的界面自动化：启动页中文、登录校验统一文案、患者登录成功后进主界面
@RunWith(AndroidJUnit4::class)
class EntryPagesUiTest {

    // 启动页：只允许中文文案，旧的英文占位必须不存在
    @Test
    fun selectLogPageShowsChineseCopy() {
        val scenario = ActivityScenario.launch(SelectLogActivity::class.java)
        try {
            onView(withText(R.string.select_log_title)).check(matches(isDisplayed()))
            onView(withText(R.string.select_log_doctor)).check(matches(isDisplayed()))
            onView(withText(R.string.select_log_patient)).check(matches(isDisplayed()))
            onView(withId(R.id.cardDoctorLogin)).check(matches(isDisplayed()))
            onView(withId(R.id.cardPatientLogin)).check(matches(isDisplayed()))
            // 英文占位已经被中文替换，残留就是回归
            onView(withText("Welcome To You!")).check(doesNotExist())
            onView(withText("Select the login method")).check(doesNotExist())
        } finally {
            scenario.close()
        }
    }

    // 医生登录页：空输入点登录，两个输入框给出同一条错误文案
    @Test
    fun doctorLoginEmptyFieldsShowUnifiedError() {
        val scenario = ActivityScenario.launch(DtLogActivity::class.java)
        try {
            // 先确认表单已经完成布局，避免点击落到还没量好尺寸的按钮上
            onView(withId(R.id.fieldDoctorId)).check(matches(isDisplayed()))
            // 勾了「记住密码」会回填上次账号，这里先清空两个输入框，保证点登录时确实是空输入
            clearField(R.id.fieldDoctorId)
            clearField(R.id.fieldDoctorPassword)
            onView(withId(R.id.btnDoctorLogin)).perform(scrollTo(), click())

            // 两个输入框会同时显示同一句错误文案，这里固定在工号框下断言
            waitForChildText(R.id.fieldDoctorId, R.string.error_login_fields_empty)
        } finally {
            scenario.close()
        }
    }

    // 患者登录页：填好测试账号直接登录，能走到患者主界面
    @Test
    fun patientLoginEntersMainPage() {
        seedActivePatient()
        val scenario = ActivityScenario.launch(PtLogActivity::class.java)
        try {
            typeInto(R.id.fieldAccount, ACCOUNT)
            typeInto(R.id.fieldPassword, PASSWORD)
            onView(withId(R.id.btnPtLogin)).perform(scrollTo(), click())

            // 登录要过登录、绑定状态、资料完整三道异步检查，这里轮询等待主界面出现
            waitForView(R.id.patient_main_root)
        } finally {
            scenario.close()
        }
    }

    // 同一套输入框组件内层 id 相同，只能靠外层输入框定位到具体那一格
    private fun typeInto(fieldId: Int, value: String) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(value), closeSoftKeyboard())
    }

    // 清空某一格输入框，去掉「记住密码」回填的历史值
    private fun clearField(fieldId: Int) {
        onView(allOf(withId(R.id.etField), isDescendantOfA(withId(fieldId))))
            .perform(scrollTo(), click(), replaceText(""), closeSoftKeyboard())
    }

    // 轮询等待某个视图出现，超时直接失败并说明原因
    private fun waitForView(viewId: Int, timeoutMs: Long = 10_000L) {
        waitUntil(timeoutMs, "viewId=$viewId") {
            onView(withId(viewId)).check(matches(isDisplayed()))
        }
    }

    // 轮询等待某个输入框下的错误文案出现（错误文案有淡入动画，不能只查一次）
    private fun waitForChildText(parentId: Int, textResId: Int, timeoutMs: Long = 5_000L) {
        waitUntil(timeoutMs, "parentId=$parentId textResId=$textResId") {
            onView(allOf(withText(textResId), isDescendantOfA(withId(parentId))))
                .check(matches(isDisplayed()))
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
        fail("等待界面超时：$description，耗时 ${timeoutMs}ms，最后一次失败：$lastFailure")
    }

    // 准备一个已绑定医生的患者账号，保证登录后直接进主界面
    private fun seedActivePatient() {
        val patientDao = MyApp.instance.database.patientDao()
        runBlocking {
            if (patientDao.getPatientById(ACCOUNT) == null) {
                patientDao.insertPatient(
                    Patient(
                        patientId = ACCOUNT,
                        account = ACCOUNT,
                        password = PASSWORD,
                        name = "测试患者001",
                        gender = "男",
                        age = 45,
                        doctorCode = "1",
                        doctorName = "张医生",
                        diagnosis = "脑卒中后遗症",
                        rehabStage = "恢复期",
                        bindingStatus = "active",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private companion object {
        const val ACCOUNT = "test001"
        const val PASSWORD = "123456"
    }
}
