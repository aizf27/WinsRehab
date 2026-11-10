package com.example.winsrehab.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 界面取值表断言：颜色、字号、间距、圆角、阴影只能来自 res/values 下的取值表
class DesignTokenTableTest {

    // 单元测试工作目录是 app 模块，两种候选路径都兼容
    private val resDir: File by lazy {
        listOf(File("src/main/res"), File("app/src/main/res")).firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("找不到资源目录，当前目录=${File(".").absolutePath}")
    }

    // 启动图标美术不是界面取值，排除在令牌断言之外（显式列出，避免隐藏问题）
    private val artAllowList = setOf("ic_launcher_background.xml", "ic_launcher_foreground.xml")

    // 设计约定的颜色定义：24 个语义色 + 7 个浅色容器底
    private val expectedColors = mapOf(
        "brand_doctor_primary" to "#FF2563EB",
        "brand_doctor_primary_dark" to "#FF1D4ED8",
        "brand_patient_primary" to "#FF16A34A",
        "brand_patient_primary_light" to "#FF22C55E",
        "surface" to "#FFFFFFFF",
        "surface_variant" to "#FFF9FAFB",
        "on_surface" to "#FF1F2937",
        "on_surface_secondary" to "#FF4B5563",
        "on_surface_tertiary" to "#FF9CA3AF",
        "outline" to "#FFD1D5DB",
        "divider" to "#FFE5E7EB",
        "primary" to "#FF2563EB",
        "on_primary" to "#FFFFFFFF",
        "success" to "#FF16A34A",
        "warning" to "#FFEA580C",
        "danger" to "#FFDC2626",
        "info" to "#FF3B82F6",
        "disabled_bg" to "#FFE5E7EB",
        "disabled_fg" to "#FF9CA3AF",
        "scrim" to "#33000000",
        "status_pending" to "#FF2563EB",
        "status_completed" to "#FF16A34A",
        "status_skipped_rejected" to "#FF6B7280",
        "status_review" to "#FFEA580C",
        "primary_container" to "#FFDBEAFE",
        "success_container" to "#FFDCFCE7",
        "warning_container" to "#FFFFEDD5",
        "info_container" to "#FFCCFBF1",
        "accent_container" to "#FFFAE8FF",
        "notice_container" to "#FFFEF9C3",
        "on_primary_overlay" to "#33FFFFFF"
    )

    // 浅色容器底：只能配 on_surface 系文字，绝不能配 on_primary（白）
    private val containerColors = setOf(
        "primary_container", "success_container", "warning_container",
        "info_container", "accent_container", "notice_container", "surface_variant", "on_primary_overlay"
    )

    // 本变更必须彻底删除的旧颜色名
    private val legacyColorNames = setOf(
        "purple_200", "purple_500", "purple_700", "teal_200", "teal_700", "black", "white", "gray",
        "gray_300", "gray_400", "gray_500", "blue_deep", "blue_shallow", "green_ai", "green_deep",
        "green_shallow", "green_400", "green_500", "green_600", "green_700", "back_green", "icon_green",
        "blue_50", "blue_100", "blue_500", "blue_600", "blue_700", "gray_50", "gray_200", "gray_600",
        "gray_800", "gray_900", "red_500", "white_alpha_90", "purple_100", "purple_600", "warm_orange",
        "warm_orange_light", "warm_peach", "warm_red"
    )

    // 设计约定的 6 档文本外观
    private val expectedTextAppearances = mapOf(
        "Caption11" to "11sp",
        "Body12" to "12sp",
        "Body14" to "14sp",
        "Title16" to "16sp",
        "Headline20" to "20sp",
        "Display24" to "24sp"
    )

    // 设计约定的间距 / 圆角 / 阴影档位
    private val expectedSpacing = mapOf(
        "space_4" to "4dp", "space_8" to "8dp", "space_12" to "12dp", "space_16" to "16dp",
        "space_24" to "24dp", "space_32" to "32dp", "space_48" to "48dp"
    )
    private val expectedRadius = mapOf("radius_8" to "8dp", "radius_12" to "12dp", "radius_16" to "16dp")
    private val expectedElevation = mapOf("elevation_0" to "0dp", "elevation_1" to "1dp")

    // 列表行右侧固定留白：与 space_48 同值不同语义，必须由下面这条测试锁住「同值」
    private val expectedRowTrailing = mapOf("row_trailing_reserve" to "48dp")

    // 解析出的颜色定义表（色值统一大写比较）
    private val colorDefs: Map<String, String> by lazy {
        parseElements(resDir.resolve("values/colors.xml"), "color").mapValues { it.value.uppercase() }
    }

    // 解析出的尺寸定义表
    private val dimenDefs: Map<String, String> by lazy {
        parseElements(resDir.resolve("values/dimens.xml"), "dimen")
    }

    // 资源里所有 xml 文件（含 values）
    private fun allXml(): List<File> = resDir.walkTopDown().filter { it.isFile && it.name.endsWith(".xml") }.toList()

    // 界面资源文件（排除 colors.xml 取值表本身与启动图标美术）
    private fun uiXml(): List<File> = allXml().filter { file ->
        val relative = file.relativeTo(resDir).path.replace('\\', '/')
        file.name !in artAllowList && relative != "values/colors.xml"
    }

    // 去掉 xml 注释，避免注释文字被当成取值扫描
    private fun stripComments(text: String): String =
        text.replace(Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")

    // 读取文件文本
    private fun readText(file: File): String = file.readText()

    // 解析 <color name="x">值</color> 这类定义
    private fun parseElements(file: File, tag: String): Map<String, String> {
        assertTrue("取值表文件缺失: ${file.path}", file.isFile)
        val regex = Regex("<$tag name=\"([A-Za-z0-9_]+)\">([^<]*)</$tag>")
        return regex.findAll(stripComments(readText(file)))
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
    }

    // 一条属性取值：支持 attr="value" 与 <item name="attr">value</item> 两种写法
    private data class AttributeValue(val where: String, val attr: String, val value: String)

    // 提取某个文件里的全部属性取值（跳过 name="..." 这类资源名）
    private fun attributesOf(file: File): List<AttributeValue> {
        val raw = stripComments(readText(file))
        val result = mutableListOf<AttributeValue>()
        val relative = file.relativeTo(resDir).path.replace('\\', '/')
        Regex("([A-Za-z]+:[A-Za-z]+)=\"([^\"]*)\"").findAll(raw).forEach { match ->
            val attr = match.groupValues[1].substringAfter(':')
            result += AttributeValue("$relative:${lineOf(raw, match.range.first)}", attr, match.groupValues[2].trim())
        }
        Regex("<item name=\"([A-Za-z0-9_:]+)\">([^<]*)</item>").findAll(raw).forEach { match ->
            val attr = match.groupValues[1].substringAfter(':')
            result += AttributeValue("$relative:${lineOf(raw, match.range.first)}", attr, match.groupValues[2].trim())
        }
        return result
    }

    // 计算偏移量所在行号（从 1 开始）
    private fun lineOf(text: String, offset: Int): Int = text.take(offset).count { it == '\n' } + 1

    // 逐个解析 styles.xml 的 <style> 块（整文件跳过会漏掉非文本外观 style 里的裸字号）
    private fun styleBlocks(): List<Pair<String, String>> {
        val raw = stripComments(readText(resDir.resolve("values/styles.xml")))
        return Regex("<style name=\"([A-Za-z0-9_.]+)\"[^>]*>(.*?)</style>", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(raw)
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
    }

    @Test
    fun `颜色表只保留 31 个语义色`() {
        assertEquals("颜色定义必须为 31 个（24 语义色 + 7 容器底），实际=${colorDefs.keys.sorted()}", 31, colorDefs.size)
        assertEquals("颜色名必须与设计一致", expectedColors.keys.sorted(), colorDefs.keys.sorted())
    }

    @Test
    fun `语义色取值与设计一致`() {
        val mismatches = expectedColors.filter { (name, value) -> colorDefs[name] != value }
            .map { (name, value) -> "$name 期望=$value 实际=${colorDefs[name]}" }
        assertTrue("语义色取值不符: $mismatches", mismatches.isEmpty())
    }

    @Test
    fun `旧颜色名已全部删除`() {
        val leftovers = legacyColorNames.filter { colorDefs.containsKey(it) }
        assertTrue("旧的 40 个颜色名必须删除，仍有: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `界面资源不出现字面色值`() {
        val hits = mutableListOf<String>()
        // 3/4 位缩写也要拦：#ffff 这类写法曾经漏网
        val hexPattern = Regex("#(?:[0-9A-Fa-f]{3,4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\\b")
        uiXml().forEach { file ->
            val raw = stripComments(readText(file))
            hexPattern.findAll(raw).forEach { match ->
                val relative = file.relativeTo(resDir).path.replace('\\', '/')
                hits += "$relative:${lineOf(raw, match.range.first)} -> ${match.value}"
            }
        }
        assertTrue("字面色值必须收敛到 colors.xml，仍有: $hits", hits.isEmpty())
    }

    @Test
    fun `界面资源不使用平台白黑`() {
        val hits = mutableListOf<String>()
        uiXml().forEach { file ->
            val raw = stripComments(readText(file))
            val relative = file.relativeTo(resDir).path.replace('\\', '/')
            Regex("@android:color/(white|black)\\b").findAll(raw).forEach { match ->
                // 图标矢量图的 fillColor 白由 app:tint 覆盖，属已登记的例外
                val prefix = raw.substring(maxOf(0, match.range.first - 19), match.range.first)
                val isIconFill = prefix == "android:fillColor=\"" &&
                    relative.startsWith("drawable/") &&
                    (file.name.startsWith("ic_") || file.name.startsWith("baseline_"))
                if (!isIconFill) hits += "$relative:${lineOf(raw, match.range.first)} -> ${match.value}"
            }
        }
        assertTrue("平台白黑必须换成 surface / on_surface / on_primary，仍有: $hits", hits.isEmpty())
    }

    @Test
    fun `颜色引用都能在取值表或选择器里解析`() {
        val selectorNames = resDir.resolve("color").listFiles()
            ?.filter { it.isFile && it.name.endsWith(".xml") }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            ?: emptySet()
        val defined = colorDefs.keys + selectorNames
        val missing = mutableListOf<String>()
        allXml().forEach { file ->
            val raw = stripComments(readText(file))
            Regex("@color/([A-Za-z0-9_]+)").findAll(raw).forEach { match ->
                val name = match.groupValues[1]
                if (name !in defined) {
                    val relative = file.relativeTo(resDir).path.replace('\\', '/')
                    missing += "$relative:${lineOf(raw, match.range.first)} -> @color/$name"
                }
            }
        }
        assertTrue("存在断链的颜色引用: $missing", missing.isEmpty())
    }

    @Test
    fun `字号只来自 6 档文本外观`() {
        val bareSizes = mutableListOf<String>()
        // 裸字号既可能是属性写法，也可能是 <item name="android:textSize"> 写法，两种都要拦
        for (file in uiXml()) {
            if (file.name == "styles.xml") continue
            for (attr in attributesOf(file)) {
                if (attr.attr == "textSize") bareSizes += "${attr.where} -> ${attr.value}"
            }
        }
        // styles.xml 一个文件里含多个 style，必须逐项按 style 名判定，不能整文件跳过
        val allowedSizes = expectedTextAppearances.values.toSet()
        styleBlocks().forEach { (styleName, body) ->
            Regex("<item name=\"android:textSize\">([^<]*)</item>").findAll(body).forEach { item ->
                val size = item.groupValues[1].trim()
                if (size !in allowedSizes) bareSizes += "values/styles.xml -> $styleName=$size"
            }
        }
        assertTrue("字号必须走文本外观，仍有裸字号: $bareSizes", bareSizes.isEmpty())

        // styles.xml 的 style 元素带属性，单独解析出 6 档文本外观
        val styleSizes = Regex("<style name=\"TextAppearance\\.WinsRehab\\.([A-Za-z0-9]+)\">(.*?)</style>",
            setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(stripComments(readText(resDir.resolve("values/styles.xml"))))
            .associate { match ->
                match.groupValues[1] to
                    Regex("android:textSize\">([^<]*)<").find(match.groupValues[2])?.groupValues?.get(1)?.trim()
            }
        assertEquals("文本外观必须恰好 6 档，实际=${styleSizes.keys}", expectedTextAppearances.keys.sorted(), styleSizes.keys.sorted())
        assertEquals("文本外观字号取值不符", expectedTextAppearances, styleSizes)

        val unknownRefs = mutableListOf<String>()
        uiXml().forEach { file ->
            attributesOf(file).forEach { attr ->
                if (attr.attr == "textAppearance" && attr.value.startsWith("@style/TextAppearance.WinsRehab.")) {
                    val name = attr.value.removePrefix("@style/TextAppearance.WinsRehab.")
                    if (name !in expectedTextAppearances.keys) unknownRefs += "${attr.where} -> ${attr.value}"
                }
            }
        }
        assertTrue("引用了未定义的文本外观: $unknownRefs", unknownRefs.isEmpty())
    }

    @Test
    fun `容器底档不能当文字颜色`() {
        val violations = mutableListOf<String>()
        uiXml().forEach { file ->
            attributesOf(file).filter { it.attr == "textColor" }.forEach { attr ->
                val name = attr.value.removePrefix("@color/")
                if (attr.value.startsWith("@color/") && name in containerColors) {
                    violations += "${attr.where} -> textColor=${attr.value}"
                }
            }
        }
        assertTrue("浅色容器底只能作背景，文字请用 on_surface 系: $violations", violations.isEmpty())
    }

    @Test
    fun `间距属性只取档位值`() {
        val pattern = Regex("space_(4|8|12|16|24|32|48)")
        val spacingAttrs = setOf("left", "right", "top", "bottom")
        val violations = mutableListOf<String>()
        for (file in uiXml()) {
            for (attr in attributesOf(file)) {
                val isSpacing = Regex("layout_margin.*|padding.*|drawablePadding|contentPadding").matches(attr.attr) ||
                    attr.attr in spacingAttrs
                if (!isSpacing) continue
                // 列表行右侧 48dp 是「同值不同语义」的专用档，只允许用在 marginEnd
                val isRowTrailing = attr.attr == "layout_marginEnd" && attr.value == "@dimen/row_trailing_reserve"
                val ok = isRowTrailing || attr.value == "0dp" ||
                    (attr.value.startsWith("@dimen/") && pattern.matches(attr.value.removePrefix("@dimen/")))
                if (!ok) violations += "${attr.where} -> ${attr.attr}=${attr.value}"
            }
        }
        assertTrue("间距只能用 space_* 档位或 0dp，违规: $violations", violations.isEmpty())
    }

    @Test
    fun `圆角只取 3 档`() {
        val violations = mutableListOf<String>()
        uiXml().forEach { file ->
            attributesOf(file).filter { it.attr == "radius" || it.attr == "cardCornerRadius" }.forEach { attr ->
                if (attr.value !in expectedRadius.keys.map { "@dimen/$it" }) {
                    violations += "${attr.where} -> ${attr.attr}=${attr.value}"
                }
            }
        }
        assertTrue("圆角只能是 radius_8/12/16，违规: $violations", violations.isEmpty())
    }

    @Test
    fun `阴影只取 2 档`() {
        val violations = mutableListOf<String>()
        uiXml().forEach { file ->
            attributesOf(file)
                .filter { it.attr == "elevation" || it.attr == "cardElevation" || it.attr == "translationZ" }
                .forEach { attr ->
                    if (attr.value !in expectedElevation.keys.map { "@dimen/$it" }) {
                        violations += "${attr.where} -> ${attr.attr}=${attr.value}"
                    }
                }
        }
        assertTrue("阴影只能是 elevation_0/1，违规: $violations", violations.isEmpty())
    }

    @Test
    fun `尺寸取值表档位齐全且取值正确`() {
        val expected = expectedSpacing + expectedRadius + expectedElevation + expectedRowTrailing
        assertEquals("dimens.xml 只应有 13 个档位，实际=${dimenDefs.keys.sorted()}", 13, dimenDefs.size)
        assertEquals("dimens.xml 档位取值不符", expected, dimenDefs)
        assertEquals(
            "row_trailing_reserve 必须与 space_48 同值，否则视觉留白会错位",
            dimenDefs["space_48"], dimenDefs["row_trailing_reserve"]
        )
    }
}
