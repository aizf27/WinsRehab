package com.example.winsrehab.ui.common

import androidx.annotation.ColorRes
import com.example.winsrehab.R
import com.example.winsrehab.data.model.PlanStatus
import com.example.winsrehab.data.model.TaskStatus

// 任务状态 -> 视觉类型；这是全站状态视觉的唯一归属，其它页面不得再写第二套映射
enum class TaskVisual { PENDING, COMPLETED, SKIPPED, WAITING_REVIEW, REJECTED, UNKNOWN }

// 计划状态 -> 视觉类型；与任务共用同一套语义色，避免同一状态在两处长成两个样
enum class PlanVisual { DRAFT, ACTIVE, COMPLETED, CANCELLED, UNKNOWN }

// 状态 -> 文案 / 语义色 / 容器色的唯一映射表：全部是纯函数，方便 JVM 单测逐值断言
object TaskStatusStyle {

    // 兜底文案：未登记状态必须显式落到这里，不许静默沿用某个合法状态的样式
    const val UNKNOWN_LABEL = "未知状态"

    // 任务五态映射，未登记取值一律兜底到 UNKNOWN
    fun visualOf(status: String): TaskVisual = when (status) {
        TaskStatus.PENDING -> TaskVisual.PENDING
        TaskStatus.COMPLETED -> TaskVisual.COMPLETED
        TaskStatus.SKIPPED -> TaskVisual.SKIPPED
        TaskStatus.WAITING_REVIEW -> TaskVisual.WAITING_REVIEW
        TaskStatus.REJECTED -> TaskVisual.REJECTED
        else -> TaskVisual.UNKNOWN
    }

    // 计划四态映射，同样带 UNKNOWN 兜底
    fun planVisualOf(status: String): PlanVisual = when (status) {
        PlanStatus.DRAFT -> PlanVisual.DRAFT
        PlanStatus.ACTIVE -> PlanVisual.ACTIVE
        PlanStatus.COMPLETED -> PlanVisual.COMPLETED
        PlanStatus.CANCELLED -> PlanVisual.CANCELLED
        else -> PlanVisual.UNKNOWN
    }

    // 只有待完成任务可执行；未知状态保守视为不可操作
    fun isActionable(status: String): Boolean = status == TaskStatus.PENDING

    fun labelOf(status: String): String = labelOf(visualOf(status))

    fun labelOf(visual: TaskVisual): String = when (visual) {
        TaskVisual.PENDING -> "待完成"
        TaskVisual.COMPLETED -> "已完成"
        TaskVisual.SKIPPED -> "已跳过"
        TaskVisual.WAITING_REVIEW -> "待审核"
        TaskVisual.REJECTED -> "已驳回"
        TaskVisual.UNKNOWN -> UNKNOWN_LABEL
    }

    fun planLabelOf(status: String): String = planLabelOf(planVisualOf(status))

    fun planLabelOf(visual: PlanVisual): String = when (visual) {
        PlanVisual.DRAFT -> "草稿"
        PlanVisual.ACTIVE -> "进行中"
        PlanVisual.COMPLETED -> "已完成"
        PlanVisual.CANCELLED -> "已取消"
        PlanVisual.UNKNOWN -> UNKNOWN_LABEL
    }

    // 语义色：状态条 / 圆点这类装饰填充用它（这几档只做装饰，不做小字）
    @ColorRes
    fun colorOf(visual: TaskVisual): Int = when (visual) {
        TaskVisual.PENDING -> R.color.status_pending
        TaskVisual.COMPLETED -> R.color.status_completed
        TaskVisual.SKIPPED -> R.color.status_skipped_rejected
        TaskVisual.WAITING_REVIEW -> R.color.status_review
        TaskVisual.REJECTED -> R.color.status_skipped_rejected
        TaskVisual.UNKNOWN -> R.color.danger
    }

    @ColorRes
    fun planColorOf(visual: PlanVisual): Int = when (visual) {
        PlanVisual.DRAFT -> R.color.status_skipped_rejected
        PlanVisual.ACTIVE -> R.color.status_pending
        PlanVisual.COMPLETED -> R.color.status_completed
        PlanVisual.CANCELLED -> R.color.status_skipped_rejected
        PlanVisual.UNKNOWN -> R.color.danger
    }

    // 徽标容器底：只取浅色容器档，白字绝不落在这里
    @ColorRes
    fun containerColorOf(visual: TaskVisual): Int = when (visual) {
        TaskVisual.PENDING -> R.color.primary_container
        TaskVisual.COMPLETED -> R.color.success_container
        TaskVisual.SKIPPED -> R.color.surface_variant
        TaskVisual.WAITING_REVIEW -> R.color.warning_container
        TaskVisual.REJECTED -> R.color.notice_container
        TaskVisual.UNKNOWN -> R.color.surface_variant
    }

    @ColorRes
    fun planContainerColorOf(visual: PlanVisual): Int = when (visual) {
        PlanVisual.DRAFT -> R.color.surface_variant
        PlanVisual.ACTIVE -> R.color.primary_container
        PlanVisual.COMPLETED -> R.color.success_container
        PlanVisual.CANCELLED -> R.color.notice_container
        PlanVisual.UNKNOWN -> R.color.surface_variant
    }

    // 徽标文案色：只能是容器底上实测达标的 5 个色（on_surface 系 / warning / 两端深色）
    @ColorRes
    fun labelColorOf(visual: TaskVisual): Int = when (visual) {
        TaskVisual.PENDING -> R.color.brand_doctor_primary_dark
        TaskVisual.COMPLETED -> R.color.brand_patient_primary_deep
        TaskVisual.SKIPPED -> R.color.on_surface_secondary
        TaskVisual.WAITING_REVIEW -> R.color.warning
        TaskVisual.REJECTED -> R.color.warning
        TaskVisual.UNKNOWN -> R.color.on_surface
    }

    @ColorRes
    fun planLabelColorOf(visual: PlanVisual): Int = when (visual) {
        PlanVisual.DRAFT -> R.color.on_surface_secondary
        PlanVisual.ACTIVE -> R.color.brand_doctor_primary_dark
        PlanVisual.COMPLETED -> R.color.brand_patient_primary_deep
        PlanVisual.CANCELLED -> R.color.on_surface_secondary
        PlanVisual.UNKNOWN -> R.color.on_surface
    }
}
