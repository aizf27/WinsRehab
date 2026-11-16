package com.example.winsrehab.ui.common

import androidx.annotation.ColorRes
import com.example.winsrehab.R

// 康复阶段 -> 视觉类型；软瘫期 / 痉挛期 / 恢复期三档，未设置与脏值各自兜底
enum class StageVisual { FLACCID, SPASTIC, RECOVERY, UNSET, UNKNOWN }

// 康复阶段视觉的唯一映射表：文案与配色都是纯函数，方便 JVM 单测逐值断言
object PatientStageStyle {

    // 患者表里阶段字段的默认值，代表医生还没填
    const val STAGE_UNSET = "未设置"

    // 兜底文案：未设置和脏值必须肉眼可区分，不许静默长得像某个合法阶段
    const val LABEL_UNSET = "阶段未设置"
    const val LABEL_UNKNOWN = "阶段待核对"

    // 三档阶段按文案精确匹配，前后空格容错；空串等同未设置
    fun visualOf(stage: String): StageVisual = when (stage.trim()) {
        "软瘫期" -> StageVisual.FLACCID
        "痉挛期" -> StageVisual.SPASTIC
        "恢复期" -> StageVisual.RECOVERY
        STAGE_UNSET, "" -> StageVisual.UNSET
        else -> StageVisual.UNKNOWN
    }

    fun labelOf(stage: String): String = labelOf(visualOf(stage))

    fun labelOf(visual: StageVisual): String = when (visual) {
        StageVisual.FLACCID -> "软瘫期"
        StageVisual.SPASTIC -> "痉挛期"
        StageVisual.RECOVERY -> "恢复期"
        StageVisual.UNSET -> LABEL_UNSET
        StageVisual.UNKNOWN -> LABEL_UNKNOWN
    }

    // 徽标文案色：只取 D1 白名单里、和下列容器底配对实测达标的几档
    @ColorRes
    fun labelColorOf(visual: StageVisual): Int = when (visual) {
        StageVisual.FLACCID -> R.color.brand_doctor_primary_dark
        StageVisual.SPASTIC -> R.color.warning
        StageVisual.RECOVERY -> R.color.brand_patient_primary_deep
        StageVisual.UNSET, StageVisual.UNKNOWN -> R.color.on_surface_secondary
    }

    // 徽标容器底：只取浅色容器档，白字绝不落在这里
    @ColorRes
    fun containerColorOf(visual: StageVisual): Int = when (visual) {
        StageVisual.FLACCID -> R.color.primary_container
        StageVisual.SPASTIC -> R.color.warning_container
        StageVisual.RECOVERY -> R.color.success_container
        StageVisual.UNSET, StageVisual.UNKNOWN -> R.color.surface_variant
    }
}
