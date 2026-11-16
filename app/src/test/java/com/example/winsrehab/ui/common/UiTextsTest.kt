package com.example.winsrehab.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 三态与占位文案映射：口径正确、只有一处来源、康复页不再自带副本
class UiTextsTest {

    // 单元测试工作目录可能是仓库根也可能是 app 模块，两种都兼容
    private val appDir: File by lazy {
        listOf(File("."), File("app")).firstOrNull { File(it, "src/main/java").isDirectory }
            ?: throw IllegalStateException("找不到 app 模块，当前目录=${File(".").absolutePath}")
    }

    private fun source(relative: String): String {
        val file = appDir.resolve(relative)
        assertTrue("缺少源文件 $relative", file.isFile)
        return file.readText()
    }

    private fun rehabFormatter(): String =
        source("src/main/java/com/example/winsrehab/ui/main/patient/main/RehabFormatter.kt")

    // 占位符全项目唯一，取值固定
    @Test
    fun placeholderIsSingleSharedLiteral() {
        assertEquals("--", UiTexts.placeholderText())
    }

    // 空态说明「当前没有数据」，不能写成报错
    @Test
    fun emptyTextSaysNoDataInsteadOfError() {
        val text = UiTexts.emptyText()
        assertTrue("空态文案要说明没有数据，实际=$text", text.contains("没有"))
        assertFalse("空态文案不能带「失败」，实际=$text", text.contains("失败"))
        assertFalse("空态文案不能带「错误」，实际=$text", text.contains("错误"))
    }

    // 错误态必须带上调用方给的原因
    @Test
    fun errorTextCarriesReason() {
        assertEquals("加载失败：网络异常", UiTexts.errorText("网络异常"))
        assertTrue("错误文案必须包含原因", UiTexts.errorText("超时").contains("超时"))
    }

    // 加载态与重试入口也有统一文案
    @Test
    fun loadingAndRetryTextsAreShared() {
        assertEquals("加载中…", UiTexts.loadingText())
        assertEquals("重试", UiTexts.retryText())
    }

    // 文案类必须落在 ui/common 包，康复页格式化器里不能再留这三项
    @Test
    fun textsLiveInCommonPackageAndFormatterKeepsNoCopies() {
        assertEquals("com.example.winsrehab.ui.common.UiTexts", UiTexts::class.java.name)
        assertTrue("UiTexts.kt 必须在 ui/common 下", File(appDir, "src/main/java/com/example/winsrehab/ui/common/UiTexts.kt").isFile)

        val formatter = rehabFormatter()
        assertFalse("RehabFormatter 不能再有 placeholderText", formatter.contains("placeholderText"))
        assertFalse("RehabFormatter 不能再有 emptyText", formatter.contains("emptyText"))
        assertFalse("RehabFormatter 不能再有 errorText", formatter.contains("errorText"))
        assertFalse("RehabFormatter 内不能再出现 -- 字面量", formatter.contains("--"))
    }

    // 占位符字面量只允许在 UiTexts 里定义一次
    @Test
    fun placeholderLiteralIsDefinedOnce() {
        val texts = source("src/main/java/com/example/winsrehab/ui/common/UiTexts.kt")
        assertEquals("占位符字面量只能出现一次", 1, Regex("\"--\"").findAll(texts).count())
    }
}
