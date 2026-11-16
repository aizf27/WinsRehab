package com.example.winsrehab.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 结构组件冒烟（静态契约）：三态、信息行、三种对话框、顶部栏、底部导航都能按约定拼出来
class StructureComponentsContractTest {

    private val rootDir: File by lazy {
        listOf(File("."), File("app")).firstOrNull { File(it, "src/main/res").isDirectory }
            ?: throw IllegalStateException("找不到 app 模块，当前目录=${File(".").absolutePath}")
    }

    private val resDir: File by lazy { rootDir.resolve("src/main/res") }

    private fun read(relative: String): String {
        val file = resDir.resolve(relative)
        assertTrue("缺少资源文件 $relative", file.isFile)
        return file.readText().replace(Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")
    }

    private fun source(relative: String): String {
        val file = rootDir.resolve(relative)
        assertTrue("缺少源文件 $relative", file.isFile)
        return file.readText()
    }

    private fun styleSheet(file: String): Map<String, Map<String, String>> =
        Regex("<style name=\"([A-Za-z0-9_.]+)\"(?:\\s+parent=\"[^\"]*\")?[^>]*>(.*?)</style>", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(read(file))
            .associate { match ->
                match.groupValues[1] to Regex("<item name=\"([A-Za-z0-9_:]+)\">([^<]*)</item>")
                    .findAll(match.groupValues[2])
                    .associate { it.groupValues[1] to it.groupValues[2].trim() }
            }

    private val styles: Map<String, Map<String, String>> by lazy { styleSheet("values/styles.xml") }
    private val themes: Map<String, Map<String, String>> by lazy { styleSheet("values/themes.xml") }

    private fun style(name: String): Map<String, String> {
        val items = styles[name]
        assertNotNull("样式 $name 必须存在", items)
        return items!!
    }

    // 4.1 三个状态布局：结构、文案控件、错误态重试入口齐备
    @Test
    fun threeStateLayoutsAreComplete() {
        val loading = read("layout/widget_state_loading.xml")
        assertTrue("加载态要有转圈", loading.contains("ProgressBar") && loading.contains("@+id/progressStateLoading"))
        assertTrue("加载态要有文案控件", loading.contains("@+id/tvStateLoading"))
        assertTrue("加载态文案必须有字号来源", loading.contains("android:textAppearance"))

        val empty = read("layout/widget_state_empty.xml")
        assertTrue("空态要有文案控件", empty.contains("@+id/tvStateEmpty"))
        assertTrue("空态引导按钮默认隐藏", empty.contains("@+id/btnStateEmptyAction") && empty.contains("android:visibility=\"gone\""))

        val error = read("layout/widget_state_error.xml")
        assertTrue("错误态要有原因文案", error.contains("@+id/tvStateError"))
        assertTrue("错误态文案用危险色", error.contains("@color/danger"))
        assertTrue("错误态必须带重试入口", error.contains("@+id/btnStateRetry"))
        assertTrue("重试按钮套统一按钮样式", error.contains("@style/Widget.WinsRehab.Button.Secondary.Small"))
    }

    // 4.2 信息行：左标签 + 右值，值可折两行且右对齐
    @Test
    fun infoRowIsLabelValuePair() {
        val raw = read("layout/widget_info_row.xml")
        assertTrue("必须有标签控件", raw.contains("@+id/tvInfoLabel"))
        assertTrue("必须有值控件", raw.contains("@+id/tvInfoValue"))
        assertTrue("标签用次要文字色", raw.contains("@color/on_surface_secondary"))
        assertTrue("值必须占满剩余宽度", raw.contains("android:layout_weight=\"1\""))
        assertTrue("值右对齐", raw.contains("android:gravity=\"end\""))
        assertTrue("值最多两行", raw.contains("android:maxLines=\"2\""))
        assertTrue("标签与值都有字号来源", Regex("android:textAppearance").findAll(raw).count() >= 2)
    }

    // 4.2 分区标题：新样式在 styles.xml，旧 SectionTitle 已从 themes.xml 移除且引用全部改口
    @Test
    fun sectionTitleIsRenamedToWidgetStyle() {
        val items = style("Widget.WinsRehab.SectionTitle")
        assertEquals("@color/on_surface", items["android:textColor"])
        assertEquals("bold", items["android:textStyle"])
        assertEquals("@dimen/space_12", items["android:layout_marginBottom"])
        // 这 8 处引用只写 style + text，宽高必须由样式提供，否则运行时 inflate 直接崩
        assertEquals("wrap_content", items["android:layout_width"])
        assertEquals("wrap_content", items["android:layout_height"])
        assertFalse("旧 SectionTitle 必须已从 themes.xml 删除", read("values/themes.xml").contains("name=\"SectionTitle\""))
        listOf("layout/fragment_dt_home.xml", "layout/fragment_dt_info_.xml", "layout/fragment_pt_info.xml").forEach { layout ->
            val raw = read(layout)
            assertTrue("$layout 必须改用新分区标题样式", raw.contains("@style/Widget.WinsRehab.SectionTitle"))
            assertFalse("$layout 不能残留旧样式引用", raw.contains("@style/SectionTitle"))
        }
    }

    // 4.3 对话框：本体与主题覆盖都在，三种语义入口都在 UiDialogs
    @Test
    fun dialogWrappersAreThemeRendered() {
        val dialog = style("Widget.WinsRehab.Dialog")
        assertEquals("对话框必须用 surface 实底", "@color/surface", dialog["backgroundTint"])

        val overlay = style("ThemeOverlay.WinsRehab.Dialog")
        assertEquals("@style/Widget.WinsRehab.Dialog", overlay["alertDialogStyle"])
        assertEquals("@style/ShapeAppearance.WinsRehab.Dialog", overlay["shapeAppearanceMediumComponent"])
        assertEquals("@style/TextAppearance.WinsRehab.Title16", overlay["materialAlertDialogTitleTextStyle"])
        assertEquals("@style/TextAppearance.WinsRehab.Body14", overlay["materialAlertDialogBodyTextStyle"])
        assertEquals("@style/Widget.WinsRehab.Button.Text", overlay["buttonBarPositiveButtonStyle"])
        assertEquals("@style/Widget.WinsRehab.Button.Text", overlay["buttonBarNegativeButtonStyle"])
        assertEquals("@style/ThemeOverlay.WinsRehab.Dialog", themes["Theme.WinsRehab"]!!["materialAlertDialogTheme"])
        assertEquals("对话框圆角统一 radius_16", "@dimen/radius_16", style("ShapeAppearance.WinsRehab.Dialog")["cornerSize"])

        val sheet = style("Widget.WinsRehab.BottomSheet")
        assertEquals("@color/surface", sheet["backgroundTint"])
        assertEquals("@style/ShapeAppearance.WinsRehab.BottomSheet", sheet["shapeAppearance"])
        assertEquals("去掉 Material 默认角部叠加", "@null", sheet["shapeAppearanceOverlay"])
        assertEquals("@dimen/radius_16", style("ShapeAppearance.WinsRehab.BottomSheet")["cornerSize"])
        assertEquals("@style/Widget.WinsRehab.BottomSheet", style("ThemeOverlay.WinsRehab.BottomSheet")["bottomSheetStyle"])

        val sheetLayout = read("layout/widget_dialog_sheet.xml")
        assertTrue("弹层骨架要有标题", sheetLayout.contains("@+id/tvSheetTitle"))
        assertTrue("弹层骨架要有内容容器", sheetLayout.contains("@+id/sheetContent"))

        val dialogs = source("src/main/java/com/example/winsrehab/ui/common/UiDialogs.kt")
        assertTrue("确认式入口", dialogs.contains("fun confirm("))
        assertTrue("单选式入口", dialogs.contains("fun choose(") && dialogs.contains("setSingleChoiceItems"))
        assertTrue("底部弹层入口", dialogs.contains("fun bottomSheet(") && dialogs.contains("BottomSheetDialog"))
        assertTrue("统一走 Material 构建器", dialogs.contains("MaterialAlertDialogBuilder"))
    }

    // 4.4 顶部栏：返回按钮 + 标题，返回图标是真箭头而不是旧日历图
    @Test
    fun topBarIsShared() {
        val raw = read("layout/widget_top_bar.xml")
        assertTrue("顶部栏高度取系统标题栏档", raw.contains("?attr/actionBarSize"))
        assertTrue("必须有返回按钮", raw.contains("@+id/btnTopBarBack"))
        assertTrue("返回按钮用新箭头图标", raw.contains("@drawable/ic_arrow_back"))
        assertTrue("返回按钮有无障碍描述", raw.contains("android:contentDescription"))
        assertTrue("必须有标题控件", raw.contains("@+id/tvTopBarTitle"))
        assertTrue("标题必须声明字号来源", raw.contains("android:textAppearance"))

        val icon = read("drawable/ic_arrow_back.xml")
        assertTrue("返回图标是 vector", icon.contains("<vector"))
        assertTrue("返回图标画的是左向箭头", icon.contains("M20,11H7.83"))
        assertFalse("不能复用画成日历的 ic_back", icon.contains("calendar"))
    }

    // 4.4 底部导航：双端共用样式，结构对齐，选中色仍由各端 selector 承载
    @Test
    fun bottomNavIsUnified() {
        val items = style("Widget.WinsRehab.BottomNav")
        assertEquals("@color/surface", items["android:background"])
        assertEquals("@dimen/elevation_1", items["android:elevation"])
        assertEquals("24dp", items["itemIconSize"])
        assertEquals("@style/TextAppearance.WinsRehab.Body12", items["itemTextAppearanceActive"])
        assertEquals("@style/TextAppearance.WinsRehab.Body12", items["itemTextAppearanceInactive"])

        val doctor = read("layout/activity_dt_home.xml")
        val patient = read("layout/activity_pt_main.xml")
        listOf("医生端" to doctor, "患者端" to patient).forEach { (who, raw) ->
            assertTrue("${who}底部导航必须套统一样式", raw.contains("@style/Widget.WinsRehab.BottomNav"))
        }
        assertEquals("医生端菜单 3 个入口", 3, Regex("<item\\s").findAll(read("menu/dt_nav_menu.xml")).count())
        assertEquals("患者端菜单 3 个入口", 3, Regex("<item\\s").findAll(read("menu/pt_nav_menu.xml")).count())
        assertTrue("医生端根布局不能再留外边距", !doctor.contains("android:layout_margin"))
        assertTrue("医生端根布局不能再铺 surface_variant", !doctor.contains("surface_variant"))
        assertTrue("两端导航底色必须一致", doctor.contains("@color/surface") && patient.contains("@color/surface"))

        assertTrue("医生端图标选中色", read("color/dt_bottom_nav_icon_selector.xml").contains("@color/brand_doctor_primary"))
        assertTrue("医生端文字选中色", read("color/dt_bottom_nav_text_selector.xml").contains("@color/brand_doctor_primary"))
        assertTrue("患者端图标选中色用亮绿", read("color/pt_bottom_nav_icon_selector.xml").contains("@color/brand_patient_primary"))
        assertTrue("患者端文字选中色用深绿", read("color/pt_bottom_nav_text_selector.xml").contains("@color/brand_patient_primary_dark"))
    }
}
