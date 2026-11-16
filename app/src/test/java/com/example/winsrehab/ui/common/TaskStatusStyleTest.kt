package com.example.winsrehab.ui.common

import com.example.winsrehab.R
import com.example.winsrehab.data.model.PlanStatus
import com.example.winsrehab.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 状态映射的守卫测试：逐值双向断言，新增状态必须补映射，不允许静默落到合法状态样式
class TaskStatusStyleTest {

    // 任务五态：每个合法状态都映射到自己的视觉类型，且文案不是兜底文案
    @Test
    fun taskStatusesMapToOwnVisual() {
        assertEquals(5, TaskStatus.ALL.size)
        TaskStatus.ALL.forEach { status ->
            val visual = TaskStatusStyle.visualOf(status)
            assertNotEquals("合法状态 $status 不应落到 UNKNOWN", TaskVisual.UNKNOWN, visual)
            assertNotEquals("合法状态 $status 不应使用兜底文案", TaskStatusStyle.UNKNOWN_LABEL, TaskStatusStyle.labelOf(status))
        }
        // 反向：UNKNOWN 以外的每个视觉类型都必须被某个合法状态取到，不允许出现死映射
        val covered = TaskStatus.ALL.map { TaskStatusStyle.visualOf(it) }.toSet()
        assertEquals(TaskVisual.values().size - 1, covered.size)
    }

    // 计划四态：同样双向完备
    @Test
    fun planStatusesMapToOwnVisual() {
        assertEquals(4, PlanStatus.ALL.size)
        PlanStatus.ALL.forEach { status ->
            val visual = TaskStatusStyle.planVisualOf(status)
            assertNotEquals("合法计划状态 $status 不应落到 UNKNOWN", PlanVisual.UNKNOWN, visual)
            assertNotEquals("合法计划状态 $status 不应使用兜底文案", TaskStatusStyle.UNKNOWN_LABEL, TaskStatusStyle.planLabelOf(status))
        }
        val covered = PlanStatus.ALL.map { TaskStatusStyle.planVisualOf(it) }.toSet()
        assertEquals(PlanVisual.values().size - 1, covered.size)
    }

    // 文案必须与 spec 冻结的九个状态名一一对应（任务五态、计划四态分开断言）
    @Test
    fun labelsMatchSpec() {
        val taskLabels = mapOf(
            TaskStatus.PENDING to "待完成",
            TaskStatus.COMPLETED to "已完成",
            TaskStatus.SKIPPED to "已跳过",
            TaskStatus.WAITING_REVIEW to "待审核",
            TaskStatus.REJECTED to "已驳回"
        )
        taskLabels.forEach { (status, label) ->
            assertEquals("任务状态 $status 的文案", label, TaskStatusStyle.labelOf(status))
        }
        val planLabels = mapOf(
            PlanStatus.DRAFT to "草稿",
            PlanStatus.ACTIVE to "进行中",
            PlanStatus.COMPLETED to "已完成",
            PlanStatus.CANCELLED to "已取消"
        )
        planLabels.forEach { (status, label) ->
            assertEquals("计划状态 $status 的文案", label, TaskStatusStyle.planLabelOf(status))
        }
    }

    // 未登记取值必须落兜底：文案固定「未知状态」，语义色固定 danger 且带日志的原值为纯函数入参
    @Test
    fun unknownFallsBackToExplicitStyle() {
        listOf("", "unknown", "PENDING", "archived").forEach { raw ->
            assertEquals(TaskVisual.UNKNOWN, TaskStatusStyle.visualOf(raw))
            assertEquals(TaskStatusStyle.UNKNOWN_LABEL, TaskStatusStyle.labelOf(raw))
            assertEquals(PlanVisual.UNKNOWN, TaskStatusStyle.planVisualOf(raw))
            assertEquals(TaskStatusStyle.UNKNOWN_LABEL, TaskStatusStyle.planLabelOf(raw))
        }
        assertEquals("未知状态", TaskStatusStyle.UNKNOWN_LABEL)
        assertEquals(R.color.danger, TaskStatusStyle.colorOf(TaskVisual.UNKNOWN))
        assertEquals(R.color.danger, TaskStatusStyle.planColorOf(PlanVisual.UNKNOWN))
    }

    // 兜底语义色必须和任何一个合法状态的语义色都不同（保证异常状态一眼可辨）
    @Test
    fun unknownSemanticColorIsDistinguishable() {
        val taskColors = listOf(TaskVisual.PENDING, TaskVisual.COMPLETED, TaskVisual.SKIPPED, TaskVisual.WAITING_REVIEW, TaskVisual.REJECTED)
            .map { TaskStatusStyle.colorOf(it) }
        assertFalse("兜底色与合法任务状态色重复", TaskStatusStyle.colorOf(TaskVisual.UNKNOWN) in taskColors)
        val planColors = listOf(PlanVisual.DRAFT, PlanVisual.ACTIVE, PlanVisual.COMPLETED, PlanVisual.CANCELLED)
            .map { TaskStatusStyle.planColorOf(it) }
        assertFalse("兜底色与合法计划状态色重复", TaskStatusStyle.planColorOf(PlanVisual.UNKNOWN) in planColors)
        // 合法任务状态里「已跳过」与「已驳回」共用废弃灰是设计约定，因此只要求 4 种不同色
        assertEquals("任务状态语义色应为 4 种（已跳过与已驳回按设计共用废弃灰）", 4, taskColors.toSet().size)
    }

    // 只有待完成任务可执行，其余（含未知）都不可执行
    @Test
    fun onlyPendingIsActionable() {
        assertTrue(TaskStatusStyle.isActionable(TaskStatus.PENDING))
        listOf(TaskStatus.COMPLETED, TaskStatus.SKIPPED, TaskStatus.WAITING_REVIEW, TaskStatus.REJECTED, "unknown", "")
            .forEach { assertFalse("$it 不应可执行", TaskStatusStyle.isActionable(it)) }
    }

    // 徽标文案色只能取 D1 允许落在容器底上的 5 个色
    @Test
    fun badgeLabelColorsFollowContrastWhitelist() {
        val whitelist = setOf(
            R.color.on_surface,
            R.color.on_surface_secondary,
            R.color.warning,
            R.color.brand_patient_primary_deep,
            R.color.brand_doctor_primary_dark
        )
        TaskVisual.values().forEach { assertTrue("$it 文案色越出白名单", TaskStatusStyle.labelColorOf(it) in whitelist) }
        PlanVisual.values().forEach { assertTrue("$it 文案色越出白名单", TaskStatusStyle.planLabelColorOf(it) in whitelist) }
    }

    // 徽标容器底只能取浅色容器档（白字、深色实底一律不许当容器）
    @Test
    fun badgeContainersStayInContainerTier() {
        val containers = setOf(
            R.color.surface_variant,
            R.color.primary_container,
            R.color.success_container,
            R.color.warning_container,
            R.color.notice_container
        )
        TaskVisual.values().forEach { assertTrue("$it 容器底不在容器档", TaskStatusStyle.containerColorOf(it) in containers) }
        PlanVisual.values().forEach { assertTrue("$it 容器底不在容器档", TaskStatusStyle.planContainerColorOf(it) in containers) }
    }

    // 每个状态的「容器底 + 文案色」组合必须唯一，否则两种状态会长得一模一样
    @Test
    fun badgeStylePairsAreUniquePerStatus() {
        val taskPairs = TaskVisual.values().map { TaskStatusStyle.containerColorOf(it) to TaskStatusStyle.labelColorOf(it) }
        assertEquals("任务徽标配色组合应两两不同", TaskVisual.values().size, taskPairs.toSet().size)
        val planPairs = PlanVisual.values().map { TaskStatusStyle.planContainerColorOf(it) to TaskStatusStyle.planLabelColorOf(it) }
        assertEquals("计划徽标配色组合应两两不同", PlanVisual.values().size, planPairs.toSet().size)
    }
}
