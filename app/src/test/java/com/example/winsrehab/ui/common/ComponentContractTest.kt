package com.example.winsrehab.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 基础控件的静态契约测试：不渲染，直接断言 res 里的组件样式与布局部件齐全、档位正确
class ComponentContractTest {

    // 单元测试工作目录是 app 模块，两种候选路径都兼容
    private val resDir: File by lazy {
        listOf(File("src/main/res"), File("app/src/main/res")).firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("找不到资源目录，当前目录=${File(".").absolutePath}")
    }

    private fun read(relative: String): String {
        val file = resDir.resolve(relative)
        assertTrue("缺少资源文件 $relative", file.isFile)
        return file.readText().replace(Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")
    }

    // 样式名到「属性名 -> 值」的表；框架属性带 android: 前缀，app 属性不带前缀
    private val styles: Map<String, Map<String, String>> by lazy { styleSheet("values/styles.xml") }

    // 主题名到「属性名 -> 值」的表，用于断言主按钮底色随主题切换
    private val themes: Map<String, Map<String, String>> by lazy { styleSheet("values/themes.xml") }

    private fun styleSheet(relative: String): Map<String, Map<String, String>> {
        val raw = read(relative)
        return Regex("<style name=\"([A-Za-z0-9_.]+)\"(?:\\s+parent=\"[^\"]*\")?[^>]*>(.*?)</style>", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(raw)
            .associate { match ->
                match.groupValues[1] to Regex("<item name=\"([A-Za-z0-9_:]+)\">([^<]*)</item>")
                    .findAll(match.groupValues[2])
                    .associate { it.groupValues[1] to it.groupValues[2].trim() }
            }
    }

    // 颜色选择器名到「包含的色值」的表，用于断言禁用态接通
    private val colorSelectors: Map<String, List<String>> by lazy {
        resDir.resolve("color").listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .associate { file ->
                file.nameWithoutExtension to Regex("@color/([A-Za-z0-9_]+)").findAll(file.readText())
                    .map { it.groupValues[1] }.toList()
            }
    }

    private fun style(name: String): Map<String, String> {
        val items = styles[name]
        assertNotNull("样式 $name 必须存在", items)
        return items!!
    }

    private fun theme(name: String): Map<String, String> {
        val items = themes[name]
        assertNotNull("主题 $name 必须存在", items)
        return items!!
    }

    // 3.1 按钮：基座 + 四语义 + 四小档，共 9 个样式名，一个都不能少
    @Test
    fun buttonStyleMatrixIsComplete() {
        val expected = setOf(
            "Widget.WinsRehab.Button",
            "Widget.WinsRehab.Button.Primary",
            "Widget.WinsRehab.Button.Secondary",
            "Widget.WinsRehab.Button.Danger",
            "Widget.WinsRehab.Button.Text",
            "Widget.WinsRehab.Button.Primary.Small",
            "Widget.WinsRehab.Button.Secondary.Small",
            "Widget.WinsRehab.Button.Danger.Small",
            "Widget.WinsRehab.Button.Text.Small"
        )
        val actual = styles.keys.filter { it == "Widget.WinsRehab.Button" || it.startsWith("Widget.WinsRehab.Button.") }.toSet()
        assertEquals("按钮样式矩阵必须与设计一致", expected, actual)

        // 标准档：Title16 + 48dp 高 + radius_16；小档：Body12 + 36dp 高 + radius_8
        val base = style("Widget.WinsRehab.Button")
        assertEquals("@style/TextAppearance.WinsRehab.Title16", base["android:textAppearance"])
        assertEquals("false", base["android:textAllCaps"])
        assertEquals("48dp", base["android:minHeight"])
        assertEquals("@dimen/radius_16", base["cornerRadius"])
        // 小档：Body12 + 36dp 高 + radius_8
        listOf("Primary", "Secondary", "Danger", "Text").forEach { semantic ->
            val small = style("Widget.WinsRehab.Button.$semantic.Small")
            assertEquals("$semantic 小档字号必须是 Body12", "@style/TextAppearance.WinsRehab.Body12", small["android:textAppearance"])
            assertEquals("$semantic 小档高度必须是 36dp", "36dp", small["android:minHeight"])
        }
        // 四语义各自有底色与文字色：文字按钮不铺底，只换文字色
        listOf("Secondary", "Danger").forEach { semantic ->
            val items = style("Widget.WinsRehab.Button.$semantic")
            assertTrue("$semantic 必须声明底色", items["android:backgroundTint"]?.startsWith("@color/btn_") == true)
            assertTrue("$semantic 必须声明文字色", items["android:textColor"]?.startsWith("@color/btn_") == true)
        }
        // 主按钮底色跟随主题色，不能写死某一端品牌色
        val primary = style("Widget.WinsRehab.Button.Primary")
        assertEquals("?attr/winsButtonPrimaryTint", primary["android:backgroundTint"])
        assertEquals("@color/btn_primary_text", primary["android:textColor"])
        assertEquals("@color/btn_primary_bg_doctor", theme("Theme.WinsRehab")["winsButtonPrimaryTint"])
        assertEquals("@color/btn_primary_bg_doctor", theme("Theme.WinsRehab.Doctor.WithBar")["winsButtonPrimaryTint"])
        assertEquals("@color/btn_primary_bg_patient", theme("Theme.WinsRehab.Patient")["winsButtonPrimaryTint"])
        assertEquals("@color/btn_text_fg", style("Widget.WinsRehab.Button.Text")["android:textColor"])
    }

    // 3.1 禁用态：实底切 disabled_bg、文字切 disabled_fg，全靠 selector 承载
    @Test
    fun buttonDisabledStateIsWired() {
        listOf("btn_primary_bg_doctor", "btn_primary_bg_patient", "btn_secondary_bg", "btn_danger_bg").forEach { name ->
            val colors = colorSelectors[name]
            assertNotNull("缺少底色选择器 $name", colors)
            assertTrue("$name 必须在禁用态切到 disabled_bg", colors!!.contains("disabled_bg"))
        }
        listOf("btn_primary_text", "btn_secondary_text", "btn_danger_text", "btn_text_fg").forEach { name ->
            val colors = colorSelectors[name]
            assertNotNull("缺少文字色选择器 $name", colors)
            assertTrue("$name 必须在禁用态切到 disabled_fg", colors!!.contains("disabled_fg"))
        }
    }

    // 3.2 输入框样式：四角统一 radius_8，并开启错误文案与辅助文案
    @Test
    fun textFieldStyleHasFourPartsSupport() {
        val items = style("Widget.WinsRehab.TextInputLayout")
        listOf(
            "boxCornerRadiusTopStart",
            "boxCornerRadiusTopEnd",
            "boxCornerRadiusBottomStart",
            "boxCornerRadiusBottomEnd"
        ).forEach { corner ->
            assertEquals("$corner 必须统一取 radius_8", "@dimen/radius_8", items[corner])
        }
        assertEquals("@color/input_box_stroke", items["boxStrokeColor"])
        assertEquals("@color/surface", items["boxBackgroundColor"])
        assertEquals("true", items["errorEnabled"])
        assertEquals("true", items["helperTextEnabled"])
        assertEquals("@style/TextAppearance.WinsRehab.Body12", items["errorTextAppearance"])
    }

    // 3.2 输入框布局：标签、必填标记、输入控件、辅助/错误文案四部分都在
    @Test
    fun textFieldLayoutHasFourParts() {
        val raw = read("layout/widget_text_field.xml")
        assertTrue("必须有标签文本", raw.contains("@+id/tvFieldLabel"))
        assertTrue("必须有必填标记", raw.contains("@+id/tvFieldRequired"))
        assertTrue("必填标记必须是红色星号", raw.contains("@color/danger") && raw.contains("android:text=\"*\""))
        assertTrue("输入控件必须套统一输入框样式", raw.contains("@+id/tilField") && raw.contains("@style/Widget.WinsRehab.TextInputLayout"))
        assertTrue("输入控件必须是 TextInputEditText", raw.contains("TextInputEditText"))
        assertTrue("必须有输入控件 id", raw.contains("@+id/etField"))
    }

    // 3.3 卡片：标准 / 可点击 / 带状态条 三形态，圆角与阴影只取既定档位
    @Test
    fun cardStylesCoverThreeForms() {
        val standard = style("Widget.WinsRehab.Card")
        assertEquals("@color/surface", standard["cardBackgroundColor"])
        assertEquals("@dimen/radius_12", standard["cardCornerRadius"])
        assertEquals("@dimen/elevation_1", standard["cardElevation"])

        val clickable = style("Widget.WinsRehab.Card.Clickable")
        assertEquals("true", clickable["android:clickable"])
        assertEquals("true", clickable["android:focusable"])
        assertEquals("?attr/selectableItemBackground", clickable["android:foreground"])

        val statusBar = style("Widget.WinsRehab.Card.StatusBar")
        assertEquals("带状态条卡片必须压平阴影", "@dimen/elevation_0", statusBar["cardElevation"])
    }

    // 3.4 徽标：布局只声明结构与统一样式，具体配色由 StatusBadge 运行期装配
    @Test
    fun statusBadgeIsSingleSource() {
        val raw = read("layout/widget_status_badge.xml")
        assertTrue("徽标根节点必须有固定 id", raw.contains("@+id/tvStatusBadge"))
        assertTrue("徽标必须引用统一样式", raw.contains("@style/Widget.WinsRehab.StatusBadge"))

        val items = style("Widget.WinsRehab.StatusBadge")
        assertEquals("@drawable/bg_chip", items["android:background"])
        assertEquals("@style/TextAppearance.WinsRehab.Body12", items["android:textAppearance"])
        assertEquals("bold", items["android:textStyle"])
        assertEquals("1", items["android:maxLines"])
    }

    // 3.1/3.3/3.4 页面验证：患者任务卡必须由新组件拼成，旧的 TextView 冒充按钮必须消失
    @Test
    fun taskCardIsWiredFromComponents() {
        val raw = read("layout/item_task_instance.xml")
        assertTrue("必须用 MaterialCardView 承载带状态条卡片样式", raw.contains("MaterialCardView") && raw.contains("@style/Widget.WinsRehab.Card.StatusBar"))
        assertTrue("必须有顶部状态条", raw.contains("@+id/statusStrip"))
        assertTrue("必须引用统一状态徽标布局", raw.contains("@layout/widget_status_badge"))
        assertTrue("开始按钮必须用统一样式", raw.contains("@+id/btnStartTask") && raw.contains("@style/Widget.WinsRehab.Button.Primary.Small"))
        assertTrue("开始按钮必须是 MaterialButton", raw.contains("<com.google.android.material.button.MaterialButton"))
        assertTrue("旧的 TextView 冒充按钮必须删除", !raw.contains("bg_btn_green_small"))
        assertTrue("旧的手写状态图标必须删除", !raw.contains("@+id/imgStatus"))
    }
}
