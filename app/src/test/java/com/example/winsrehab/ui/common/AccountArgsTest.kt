package com.example.winsrehab.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 入参解析工具的三种情形：只有导航参数、只有宿主 Intent、两边都没有
class AccountArgsTest {

    // 情形一：导航参数有值，直接用它
    @Test
    fun navValueWins() {
        assertEquals("test001", AccountArgs.resolve("test001", "other"))
        assertEquals("1", AccountArgs.resolve("1", null))
    }

    // 情形二：导航参数缺失（或只有空白），回落宿主 Intent
    @Test
    fun intentValueFallback() {
        assertEquals("test001", AccountArgs.resolve(null, "test001"))
        assertEquals("test001", AccountArgs.resolve("", "test001"))
        assertEquals("test001", AccountArgs.resolve("   ", "test001"))
    }

    // 情形三：两边都没有有效值，返回 null 让调用方决定降级行为
    @Test
    fun nullWhenBothMissing() {
        assertNull(AccountArgs.resolve(null, null))
        assertNull(AccountArgs.resolve("", ""))
        assertNull(AccountArgs.resolve("  ", "  "))
    }

    // 入参两侧都带空格时按去空格后的结果返回，避免把空白账号透给仓库层
    @Test
    fun trimsWhitespace() {
        assertEquals("test001", AccountArgs.resolve("  test001  ", null))
        assertEquals("1", AccountArgs.resolve(null, " 1 "))
    }
}
