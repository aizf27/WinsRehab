package com.example.winsrehab.ui.common

import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.example.winsrehab.R
import com.example.winsrehab.data.model.BindingStatus

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

    // 康复阶段徽标：三档阶段配语义色，未设置 / 脏值各走中性兜底，脏值只在这里落日志
    fun bindStage(badge: TextView, stage: String) {
        val visual = PatientStageStyle.visualOf(stage)
        if (visual == StageVisual.UNKNOWN) Log.e(TAG, "未登记的康复阶段，按待核对徽标渲染: $stage")
        apply(
            badge,
            PatientStageStyle.labelOf(visual),
            PatientStageStyle.labelColorOf(visual),
            PatientStageStyle.containerColorOf(visual)
        )
    }

    // 绑定状态徽标：等待确认 / 已通过 / 已拒绝 / 未绑定，配色沿用容器档
    fun bindBinding(badge: TextView, status: String) {
        when (status) {
            BindingStatus.PENDING -> apply(
                badge,
                badge.context.getString(R.string.binding_state_pending),
                R.color.warning,
                R.color.warning_container
            )
            BindingStatus.ACTIVE -> apply(
                badge,
                badge.context.getString(R.string.binding_state_approved),
                R.color.brand_patient_primary_deep,
                R.color.success_container
            )
            BindingStatus.REJECTED -> apply(
                badge,
                badge.context.getString(R.string.binding_state_rejected),
                R.color.warning,
                R.color.notice_container
            )
            else -> {
                // 兜底按「未绑定」渲染，未登记取值只在这里落日志
                Log.e(TAG, "未登记的绑定状态，按未绑定渲染: $status")
                apply(
                    badge,
                    badge.context.getString(R.string.binding_state_unbound),
                    R.color.on_surface_secondary,
                    R.color.surface_variant
                )
            }
        }
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
