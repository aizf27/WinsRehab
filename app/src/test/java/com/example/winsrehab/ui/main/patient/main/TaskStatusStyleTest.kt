package com.example.winsrehab.ui.main.patient.main

import com.example.winsrehab.data.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// TaskStatusStyle：五种状态映射与可操作性
class TaskStatusStyleTest {

    @Test
    fun visualMappingForAllStatuses() {
        assertEquals(TaskVisual.PENDING, TaskStatusStyle.visualOf(TaskStatus.PENDING))
        assertEquals(TaskVisual.COMPLETED, TaskStatusStyle.visualOf(TaskStatus.COMPLETED))
        assertEquals(TaskVisual.SKIPPED, TaskStatusStyle.visualOf(TaskStatus.SKIPPED))
        assertEquals(TaskVisual.WAITING_REVIEW, TaskStatusStyle.visualOf(TaskStatus.WAITING_REVIEW))
        assertEquals(TaskVisual.REJECTED, TaskStatusStyle.visualOf(TaskStatus.REJECTED))
        assertEquals(TaskVisual.UNKNOWN, TaskStatusStyle.visualOf("whatever"))
    }

    @Test
    fun onlyPendingIsActionable() {
        assertTrue(TaskStatusStyle.isActionable(TaskStatus.PENDING))
        assertFalse(TaskStatusStyle.isActionable(TaskStatus.COMPLETED))
        assertFalse(TaskStatusStyle.isActionable(TaskStatus.SKIPPED))
        assertFalse(TaskStatusStyle.isActionable(TaskStatus.WAITING_REVIEW))
        assertFalse(TaskStatusStyle.isActionable(TaskStatus.REJECTED))
        assertFalse(TaskStatusStyle.isActionable("unknown"))
    }

    @Test
    fun labelsFallbackToUnknown() {
        assertEquals("待完成", TaskStatusStyle.labelOf(TaskStatus.PENDING))
        assertEquals("已完成", TaskStatusStyle.labelOf(TaskStatus.COMPLETED))
        assertEquals("未知", TaskStatusStyle.labelOf("whatever"))
    }
}
