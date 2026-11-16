package com.example.winsrehab.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 康复阶段视觉映射单测：三档阶段 + 未设置 + 脏值，逐值断言文案与配色
class PatientStageStyleTest {

    @Test
    fun threeStagesMapToTheirOwnVisual() {
        assertEquals(StageVisual.FLACCID, PatientStageStyle.visualOf("软瘫期"))
        assertEquals(StageVisual.SPASTIC, PatientStageStyle.visualOf("痉挛期"))
        assertEquals(StageVisual.RECOVERY, PatientStageStyle.visualOf("恢复期"))
    }

    @Test
    fun stageTextIsTrimmedBeforeMatching() {
        assertEquals(StageVisual.RECOVERY, PatientStageStyle.visualOf(" 恢复期 "))
    }

    // 未设置是患者表默认值，属于合法取值，不能被当成脏值
    @Test
    fun unsetIsLegalAndDirtyValueIsUnknown() {
        assertEquals(StageVisual.UNSET, PatientStageStyle.visualOf(PatientStageStyle.STAGE_UNSET))
        assertEquals(StageVisual.UNSET, PatientStageStyle.visualOf(""))
        assertEquals(StageVisual.UNSET, PatientStageStyle.visualOf("   "))
        assertEquals(StageVisual.UNKNOWN, PatientStageStyle.visualOf("急性期"))
    }

    // 每个视觉都有非空且互不相同的文案，兜底两档也要肉眼可分
    @Test
    fun everyVisualHasDistinctLabel() {
        val labels = StageVisual.values().map { PatientStageStyle.labelOf(it) }
        labels.forEach { assertTrue("文案不能为空", it.isNotBlank()) }
        assertEquals(StageVisual.values().size, labels.distinct().size)
        assertNotEquals(
            PatientStageStyle.labelOf(StageVisual.UNSET),
            PatientStageStyle.labelOf(StageVisual.UNKNOWN)
        )
    }

    // 三档语义色的容器底两两不同，否则徽标看不出阶段差别
    @Test
    fun threeStagesUseDifferentContainerColors() {
        val containers = listOf(StageVisual.FLACCID, StageVisual.SPASTIC, StageVisual.RECOVERY)
            .map { PatientStageStyle.containerColorOf(it) }
        assertEquals(3, containers.distinct().size)
    }

    // 每个视觉都必须落到真实色资源，不能返回 0 让徽标变透明
    @Test
    fun everyVisualHasBothColors() {
        StageVisual.values().forEach { visual ->
            assertTrue("$visual 缺文案色", PatientStageStyle.labelColorOf(visual) != 0)
            assertTrue("$visual 缺容器色", PatientStageStyle.containerColorOf(visual) != 0)
        }
    }
}
