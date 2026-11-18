package com.example.winsrehab.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.winsrehab.R
import com.example.winsrehab.ui.main.patient.Video.TrainingVideoActivity
import com.example.winsrehab.ui.main.patient.Video.VideoPlayerActivity
import com.example.winsrehab.ui.main.patient.psychology.PsychologyActivity
import org.junit.Test
import org.junit.runner.RunWith

// 独立功能页冒烟：确认页面可启动、顶栏与关键控件可见
@RunWith(AndroidJUnit4::class)
class IndependentFeaturePagesUiTest {

    @Test
    fun psychologyPageShowsTitleAndMessageInput() {
        val scenario = ActivityScenario.launch(PsychologyActivity::class.java)
        try {
            onView(withId(R.id.tvTopBarTitle)).check(matches(withText(R.string.page_psychology_title)))
            onView(withId(R.id.etMessage)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun trainingVideoPageShowsTitleAndCategoryFilter() {
        val scenario = ActivityScenario.launch(TrainingVideoActivity::class.java)
        try {
            onView(withId(R.id.tvTopBarTitle)).check(matches(withText(R.string.page_training_videos_title)))
            onView(withId(R.id.spinnerCategory)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun videoPlayerPageShowsErrorForMissingPath() {
        val intent = Intent().setClassName(
            "com.example.winsrehab",
            VideoPlayerActivity::class.java.name
        ).putExtra("title", "测试视频")
        val scenario = ActivityScenario.launch<VideoPlayerActivity>(intent)
        try {
            onView(withId(R.id.tvTopBarTitle)).check(matches(withText("测试视频")))
            onView(withId(R.id.statePlayerError)).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
