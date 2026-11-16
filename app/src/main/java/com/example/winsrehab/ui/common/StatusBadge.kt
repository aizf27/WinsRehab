package com.example.winsrehab.ui.common

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

// 状态徽标装配器：把 TaskStatusStyle 的纯映射贴到视图上；未登记状态只在这里落日志
object StatusBadge {

    private const val TAG = "StatusBadge"

    // 任务徽标：文案 + 文案色 + 容器底一次性装配，页面不再自己拼颜色
    fun bindTask(badge: TextView, status: String) {
        val visual = TaskStatusStyle.visualOf(status)
        if (visual == TaskVisual.UNKNOWN) Log.e(TAG, "未登记的任务状态，按兜底徽标渲染: $status")
        apply(
            badge,
            TaskStatusStyle.labelOf(visual),
            TaskStatusStyle.labelColorOf(visual),
            TaskStatusStyle.containerColorOf(visual)
        )
    }

    // 计划徽标：与任务徽标同一套装配逻辑，只是映射表换成计划四态
    fun bindPlan(badge: TextView, status: String) {
        val visual = TaskStatusStyle.planVisualOf(status)
        if (visual == PlanVisual.UNKNOWN) Log.e(TAG, "未登记的计划状态，按兜底徽标渲染: $status")
        apply(
            badge,
            TaskStatusStyle.planLabelOf(visual),
            TaskStatusStyle.planLabelColorOf(visual),
            TaskStatusStyle.planContainerColorOf(visual)
        )
    }

    // 三件事：写文案、设文字色、设容器底色；徽标始终可见（状态一定有值）
    private fun apply(
        badge: TextView,
        label: String,
        @ColorRes labelColor: Int,
        @ColorRes containerColor: Int
    ) {
        badge.text = label
        badge.setTextColor(ContextCompat.getColor(badge.context, labelColor))
        badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(badge.context, containerColor))
        badge.visibility = View.VISIBLE
    }
}
