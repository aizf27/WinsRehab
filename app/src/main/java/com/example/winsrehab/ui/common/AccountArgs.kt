package com.example.winsrehab.ui.common

import android.content.Intent
import android.os.Bundle

// 页面入参统一解析：导航参数优先，宿主 Activity 的 Intent 兜底，两边都取不到就返回 null
object AccountArgs {

    const val KEY_ACCOUNT = "account"
    const val KEY_DOCTOR_CODE = "doctorCode"

    // 患者账号：BottomNav 切页不带 nav 参数时靠 Intent 兜底
    fun account(arguments: Bundle?, intent: Intent?): String? =
        resolve(arguments?.getString(KEY_ACCOUNT), intent?.getStringExtra(KEY_ACCOUNT))

    // 医生工号：情况同上，这里单独给个语义化入口
    fun doctorCode(arguments: Bundle?, intent: Intent?): String? =
        resolve(arguments?.getString(KEY_DOCTOR_CODE), intent?.getStringExtra(KEY_DOCTOR_CODE))

    // 纯函数核心：先看导航参数再看宿主 Intent，空串与空白一律当没传（方便单测）
    fun resolve(navValue: String?, intentValue: String?): String? {
        val fromNav = navValue?.trim().orEmpty()
        if (fromNav.isNotEmpty()) return fromNav

        val fromIntent = intentValue?.trim().orEmpty()
        if (fromIntent.isNotEmpty()) return fromIntent

        return null
    }
}
