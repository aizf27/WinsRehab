package com.example.winsrehab.ui

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.winsrehab.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

// Material 底栏每个 tab 都有大小两个同文案 TextView，按文案点会一次命中两个
// 这里直接对 BottomNavigationView 选中对应菜单项，绕开文字节点
fun clickBottomTabItem(title: String) {
    onView(allOf(withId(R.id.bottom_nav), isDisplayed())).perform(selectNavItem(title))
}

// 自定义动作：按标题找到菜单项并选中，选中事件会正常触发导航
private fun selectNavItem(title: String): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> =
        allOf(isAssignableFrom(BottomNavigationView::class.java), isDisplayed())

    override fun getDescription(): String = "选中底部导航项 $title"

    override fun perform(uiController: UiController, view: View) {
        val nav = view as BottomNavigationView
        val item = (0 until nav.menu.size()).map { nav.menu.getItem(it) }
            .firstOrNull { it.title?.toString() == title }
        check(item != null) { "底部导航没有 $title 这一项" }
        nav.selectedItemId = item.itemId
        uiController.loopMainThreadForAtLeast(300)
    }
}
