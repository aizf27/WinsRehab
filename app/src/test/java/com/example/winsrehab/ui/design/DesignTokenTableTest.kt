package com.example.winsrehab.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

// 界面取值表断言：颜色、字号、间距、圆角、阴影只能来自 res/values 下的取值表
class DesignTokenTableTest {

    // 单元测试工作目录是 app 模块，两种候选路径都兼容
    private val resDir: File by lazy {
        listOf(File("src/main/res"), File("app/src/main/res")).firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("找不到资源目录，当前目录=${File(".").absolutePath}")
    }

    // 启动图标美术不是界面取值，排除在令牌断言之外（显式列出，避免隐藏问题）
    private val artAllowList = setOf("ic_launcher_background.xml", "ic_launcher_foreground.xml")

    // 设计约定的颜色定义：26 个语义色 + 7 个浅色容器底
    private val expectedColors = mapOf(
        "brand_doctor_primary" to "#FF2563EB",
        "brand_doctor_primary_dark" to "#FF1D4ED8",
        "brand_patient_primary" to "#FF16A34A",
        "brand_patient_primary_light" to "#FF22C55E",
        // 患者端深绿两档：主色与辅绿的绿底白字都过不了 4.5:1，绿底按钮与欢迎卡靠它们
        "brand_patient_primary_dark" to "#FF15803D",
        "brand_patient_primary_deep" to "#FF166534",
        "surface" to "#FFFFFFFF",
        "surface_variant" to "#FFF9FAFB",
        "on_surface" to "#FF1F2937",
        "on_surface_secondary" to "#FF4B5563",
        "on_surface_tertiary" to "#FF6B7280",
        "outline" to "#FFD1D5DB",
        "divider" to "#FFE5E7EB",
        "primary" to "#FF2563EB",
        "on_primary" to "#FFFFFFFF",
        "success" to "#FF16A34A",
        "warning" to "#FF9A3412",
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

    // 语义色里暂时还没有页面消费方的令牌：显式登记为后续任务组保留，并写清由谁消费
    // 登记标准是「已知下一组就要用」，不是「想不出来给谁用」；一旦被消费就必须从本表移除
    // 第 3 组已把 4 个预留令牌全部消费（禁用态进 res/color/btn_*_bg|text.xml，状态色进 ui/common 状态映射），当前无预留
    private val reservedColorTokens = mapOf<String, String>()

    // 禁用态配色：WCAG 1.4.3 明确豁免失效控件，disabled_bg / disabled_fg 这对组合不参与对比度考核
    private val disabledStateColors = setOf("disabled_bg", "disabled_fg")

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

    // 只有布局里才有「文字落在什么底上」这回事
    private fun layoutXml(): List<File> = uiXml().filter { it.parentFile.name == "layout" }

    // 取色方扫描范围：整个 app/src/main（xml 里是 @color/x，Kotlin 里是 R.color.x），不含取值表本身
    private fun consumerSources(): List<File> {
        val mainDir = resDir.parentFile ?: return emptyList()
        return mainDir.walkTopDown().filter { file ->
            file.isFile && file.extension in setOf("xml", "kt", "java") &&
                !(file.parentFile?.name == "values" && file.name == "colors.xml")
        }.toList()
    }

    // 字号来源豁免清单：登记一条就必须在注释里写清为什么不能走文本外观（当前无豁免）
    private val typographyExemptions = emptySet<String>()

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

    // 一个 xml 元素：标签、位置、属性表、祖先属性链（由近到远）
    private data class Element(
        val tag: String,
        val where: String,
        val attrs: Map<String, String>,
        val ancestors: List<Map<String, String>>
    )

    // 按出现顺序解析元素并维护祖先栈，用于向上找「最近的不透明底」
    private fun elementsOf(file: File): List<Element> {
        val raw = stripComments(readText(file))
        val relative = file.relativeTo(resDir).path.replace('\\', '/')
        val result = mutableListOf<Element>()
        val stack = mutableListOf<Map<String, String>>()
        Regex("<(/)?([A-Za-z][A-Za-z0-9_.]*)([^>]*)>").findAll(raw).forEach { match ->
            val body = match.groupValues[3]
            if (match.groupValues[1] == "/") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                return@forEach
            }
            val attrs = Regex("([A-Za-z]+:[A-Za-z]+)=\"([^\"]*)\"").findAll(body)
                .associate { it.groupValues[1] to it.groupValues[2].trim() }.toMutableMap()
            // style="..." 没有命名空间前缀，普通属性正则收不到，单独补上（字号与底常常写在样式里）
            Regex("(?:^|\\s)style=\"([^\"]*)\"").find(body)?.let { attrs["style"] = it.groupValues[1].trim() }
            result += Element(match.groupValues[2], "$relative:${lineOf(raw, match.range.first)}", attrs, stack.reversed().toList())
            // 自闭合标签不入栈，否则后面的兄弟元素会被算成它的子元素
            if (!body.trimEnd().endsWith("/")) stack += attrs
        }
        return result
    }

    // style 定义：父样式名 + 自身写的属性（框架属性带 android: 前缀，app 属性不带前缀，两种都原样收录）
    private data class StyleDef(val parent: String?, val attrs: Map<String, String>)

    // styles.xml 与 themes.xml 必须一起解析：SectionTitle 这类样式定义在 themes.xml 里
    private val styleDefs: Map<String, StyleDef> by lazy {
        val defs = mutableMapOf<String, StyleDef>()
        listOf("styles.xml", "themes.xml").forEach { fileName ->
            val file = resDir.resolve("values/$fileName")
            if (!file.isFile) return@forEach
            val raw = stripComments(readText(file))
            Regex(
                "<style name=\"([A-Za-z0-9_.]+)\"(?:\\s+parent=\"([A-Za-z0-9_.]+)\")?[^>]*>(.*?)</style>",
                setOf(RegexOption.DOT_MATCHES_ALL)
            ).findAll(raw).forEach { match ->
                val styleName = match.groupValues[1]
                val explicitParent = match.groupValues[2].ifEmpty { null }
                // 点号隐式继承：Theme.WinsRehab.Doctor 的父样式是 Theme.WinsRehab
                val implicitParent = if (explicitParent == null && styleName.contains('.')) styleName.substringBeforeLast('.') else null
                val attrs = Regex("<item name=\"([A-Za-z0-9_:]+)\">([^<]*)</item>").findAll(match.groupValues[3])
                    .associate { it.groupValues[1] to it.groupValues[2].trim() }
                defs[styleName] = StyleDef(explicitParent ?: implicitParent, attrs)
            }
        }
        defs
    }

    // 沿 style 链找一个属性的取值（自身写的最优先）
    private fun attrFromStyleChain(attrs: Map<String, String>, key: String): String? =
        attrFromStyleName(attrs["style"]?.removePrefix("@style/"), key)

    // 从样式名出发沿父样式链找属性；Material 内置默认值不在 styles.xml 里，查不到就当没写
    private fun attrFromStyleName(startStyle: String?, key: String): String? {
        var style = startStyle
        var hops = 0
        while (style != null && hops < 12) {
            val def = styleDefs[style] ?: return null
            def.attrs[key]?.let { return it }
            style = def.parent
            hops++
        }
        return null
    }

    // 取属性：元素自身优先，没有再跟 style 链
    private fun resolveAttr(element: Element, key: String): String? =
        element.attrs[key] ?: attrFromStyleChain(element.attrs, key)

    // 字号来源：文本外观或直接字号都算「声明了字号」，裸字号的档位由另一条测试兜住
    private fun hasFontSource(element: Element): Boolean =
        resolveAttr(element, "android:textAppearance") != null || resolveAttr(element, "android:textSize") != null

    // 字号档位：文本外观查 6 档取值，直接字号取数字，导航标签看 itemTextAppearance*，都找不到按正文 14sp 保守判
    private fun fontSizeOf(element: Element): Int {
        resolveAttr(element, "android:textAppearance")?.let { ref ->
            val name = ref.removePrefix("@style/TextAppearance.WinsRehab.")
            expectedTextAppearances[name]?.let { return it.removeSuffix("sp").toInt() }
        }
        resolveAttr(element, "android:textSize")?.let { raw ->
            raw.removeSuffix("sp").removeSuffix("dp").toIntOrNull()?.let { return it }
        }
        navLabelTypography(element)?.let { return it.size }
        return 14
    }

    // 导航标签排版：BottomNavigationView 默认 12sp/常规字重，改写在 itemTextAppearanceActive/Inactive 指向的样式里
    private data class NavLabelTypography(val size: Int, val bold: Boolean)

    // Active 与 Inactive 两档都取：字号取最小（门槛更严），两档都是 bold 才算 bold
    private fun navLabelTypography(element: Element): NavLabelTypography? {
        val isNavLabel = element.attrs.containsKey("app:itemTextColor") || element.attrs.containsKey("app:tabTextColor")
        if (!isNavLabel) return null
        // Material 组件标签默认 12sp / 常规字重；外观没被显式声明时按这个默认判，别当正文 14sp 猜
        val materialDefault = NavLabelTypography(12, false)
        val styleNames = listOf("app:itemTextAppearanceActive", "app:itemTextAppearanceInactive", "app:tabTextAppearance")
            .mapNotNull { element.attrs[it]?.removePrefix("@style/") }
        val sizes = styleNames.mapNotNull { attrFromStyleName(it, "android:textSize") }
            .mapNotNull { it.removeSuffix("sp").removeSuffix("dp").toIntOrNull() }
        val size = sizes.minOrNull() ?: return materialDefault
        val bold = styleNames.all { attrFromStyleName(it, "android:textStyle") == "bold" }
        return NavLabelTypography(size, bold)
    }

    // drawable 里的底色候选：solid 与 gradient 各档都要收（渐变按最差一端算）
    // <stroke android:color> 是描边色不是底，必须先剔掉，否则描边会被当成第二个底误报
    private val drawableBottoms: Map<String, List<String>> by lazy {
        resDir.resolve("drawable").listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .associate { file ->
                val raw = stripComments(readText(file)).replace(Regex("<stroke\\b[^>]*>"), "")
                val refs = Regex("android:(?:color|startColor|endColor|centerColor)=\"(@color/[A-Za-z0-9_]+)\"")
                    .findAll(raw).map { it.groupValues[1].removePrefix("@color/") }.toList()
                file.nameWithoutExtension to refs
            }
    }

    // 颜色选择器：文字色可能指向 res/color 下的 selector，逐状态取出真实色值
    private val selectorDefs: Map<String, List<String>> by lazy {
        resDir.resolve("color").listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .associate { file ->
                val raw = stripComments(readText(file))
                file.nameWithoutExtension to Regex("android:color=\"@color/([A-Za-z0-9_]+)\"")
                    .findAll(raw).map { it.groupValues[1] }.toList()
            }
    }

    // @color/ 引用 → 真实色名列表；指向 selector 时把每个状态的色都列出来（状态间色差要逐个考核）
    private fun colorRefToNames(raw: String): List<String>? {
        if (!raw.startsWith("@color/")) return null
        val name = raw.removePrefix("@color/")
        return selectorDefs[name] ?: listOf(name)
    }

    // ?attr/xxx 引用 → 所有主题里该属性的取值（医生蓝 / 患者深绿各一套，逐个底色都要考核）
    private fun themeAttrColors(raw: String): List<String>? {
        if (!raw.startsWith("?attr/")) return null
        val attr = raw.removePrefix("?attr/")
        val refs = styleDefs.values.mapNotNull { it.attrs[attr] ?: it.attrs["android:$attr"] }.distinct()
        val colors = refs.flatMap { colorRefToNames(it).orEmpty() }
        return colors.ifEmpty { null }
    }

    // 样式链里写的底：样式内的 app 属性不带前缀（cardBackgroundColor 这类），两种写法都要认
    private fun bottomFromStyleChain(startStyle: String?): List<String>? {
        var style = startStyle
        var hops = 0
        while (style != null && hops < 12) {
            val def = styleDefs[style] ?: return null
            (def.attrs["android:backgroundTint"] ?: def.attrs["backgroundTint"])?.let { tint ->
                return themeAttrColors(tint) ?: colorRefToNames(tint)
            }
            (def.attrs["android:background"] ?: def.attrs["background"])?.let { background ->
                if (background.startsWith("@color/")) return colorRefToNames(background)
                if (background.startsWith("@drawable/")) {
                    val ends = drawableBottoms[background.removePrefix("@drawable/")].orEmpty()
                    if (ends.isNotEmpty()) return ends
                }
            }
            (def.attrs["app:cardBackgroundColor"] ?: def.attrs["cardBackgroundColor"])?.let { return colorRefToNames(it) }
            style = def.parent
            hops++
        }
        return null
    }

    // 由近到远找最近的不透明底，返回底色名列表（渐变返回多端）；null 表示整条祖先链都没写底
    private fun bottomColorsOf(element: Element): List<String>? {
        for (attrs in listOf(element.attrs) + element.ancestors) {
            // backgroundTint 会覆盖 shape 自带的填充色，所以优先取 tint
            attrs["android:backgroundTint"]?.let { colorRefToNames(it) }?.let { return it }
            attrs["android:background"]?.let { background ->
                if (background.startsWith("@color/")) colorRefToNames(background)?.let { return it }
                if (background.startsWith("@drawable/")) {
                    val ends = drawableBottoms[background.removePrefix("@drawable/")].orEmpty()
                    if (ends.isNotEmpty()) return ends
                }
            }
            attrs["app:cardBackgroundColor"]?.let { colorRefToNames(it) }?.let { return it }
            // 组件样式把底写在 styles.xml 里，样式链不参与就会误判成「找不到不透明底」
            bottomFromStyleChain(attrs["style"]?.removePrefix("@style/"))?.let { return it }
        }
        return null
    }

    // #AARRGGBB / #RRGGBB → 0..1 的 ARGB 四通道
    private fun argb(hex: String): DoubleArray {
        val value = hex.removePrefix("#").uppercase()
        val full = when (value.length) {
            6 -> "FF$value"
            8 -> value
            else -> throw IllegalArgumentException("颜色格式不认识: $hex")
        }
        return DoubleArray(4) { full.substring(it * 2, it * 2 + 2).toInt(16) / 255.0 }
    }

    // WCAG 相对亮度
    private fun relativeLuminance(color: DoubleArray): Double {
        fun channel(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color[1]) + 0.7152 * channel(color[2]) + 0.0722 * channel(color[3])
    }

    // 两色对比度（WCAG 公式，1.0 ~ 21.0）
    private fun contrastRatio(foreground: DoubleArray, background: DoubleArray): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    // 半透明前景叠到实底上：视图 alpha 与颜色 alpha 同一套算法
    private fun overlay(foreground: DoubleArray, background: DoubleArray, alpha: Double): DoubleArray =
        DoubleArray(4) { i -> if (i == 0) 1.0 else foreground[i] * alpha + background[i] * (1 - alpha) }

    @Test
    fun `颜色表只保留 33 个语义色`() {
        assertEquals("颜色定义必须为 33 个（26 语义色 + 7 容器底），实际=${colorDefs.keys.sorted()}", 33, colorDefs.size)
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

    // 零引用令牌要么在本轮被消费掉，要么在 reservedColorTokens 里登记「下一组谁来用」，不留无主令牌
    @Test
    fun `零引用语义色必须登记为后续组保留`() {
        val texts = consumerSources().map { readText(it) }
        assertTrue("扫不到 app/src/main 源码，无法判定消费方", texts.isNotEmpty())
        val unused = expectedColors.keys.filter { name ->
            val byXml = "@color/$name"
            val byCode = "R.color.$name"
            texts.none { text -> text.contains(byXml) || text.contains(byCode) }
        }
        val unregistered = unused - reservedColorTokens.keys
        assertTrue("以下语义色零引用且未登记保留理由（要么消费掉，要么在 reservedColorTokens 登记）: $unregistered", unregistered.isEmpty())

        // 登记了却写不出消费方，等于没登记
        val blankReasons = reservedColorTokens.filterValues { it.isBlank() }.keys
        assertTrue("保留登记必须写清由谁消费: $blankReasons", blankReasons.isEmpty())

        // 已被消费的令牌要从登记表移除，避免保留清单长期漂移
        val stale = reservedColorTokens.keys - unused
        assertTrue("以下令牌已有页面消费，请从 reservedColorTokens 移除: $stale", stale.isEmpty())
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

    @Test
    fun `文本控件必须声明字号来源`() {
        val violations = mutableListOf<String>()
        layoutXml().forEach { file ->
            elementsOf(file).forEach { element ->
                // 有文字或提示的控件才有字号需求，空串与纯装饰元素跳过
                val hasText = !element.attrs["android:text"].isNullOrBlank() || !element.attrs["android:hint"].isNullOrBlank()
                if (!hasText) return@forEach
                val label = "${element.where} <${element.tag}>"
                if (label in typographyExemptions) return@forEach
                if (!hasFontSource(element)) violations += label
            }
        }
        assertTrue("有文字/提示的控件必须声明文本外观（或明确的字号档），缺少: $violations", violations.isEmpty())
    }

    // 一个前景色：角色（报错用）+ 颜色名 + 是否为导航标签（Material 组件的标签底由控件自身背景决定）
    private data class TextForeground(val role: String, val colorName: String, val navLabel: Boolean = false)

    // 元素上的文字前景色：textColor / textColorHint / 导航标签色都算，指向 selector 时逐状态展开
    private fun textColorsOf(element: Element): List<TextForeground> {
        val result = mutableListOf<TextForeground>()
        listOf("android:textColor" to "文字", "android:textColorHint" to "提示").forEach { (key, role) ->
            val raw = resolveAttr(element, key) ?: return@forEach
            if (!raw.startsWith("@color/")) return@forEach
            val name = raw.removePrefix("@color/")
            val states = selectorDefs[name]
            if (states != null) states.forEach { result += TextForeground(role, it) } else result += TextForeground(role, name)
        }
        // 底部导航/标签页的选中与未选中标签色走 app 命名空间，选中态常低于 4.5:1，必须纳入考核
        listOf("app:itemTextColor", "app:tabTextColor").forEach { key ->
            resolveAttr(element, key)?.takeIf { it.startsWith("@color/") }?.let { raw ->
                val name = raw.removePrefix("@color/")
                val states = selectorDefs[name]
                if (states != null) states.forEach { result += TextForeground("导航标签", it, navLabel = true) }
                else result += TextForeground("导航标签", name, navLabel = true)
            }
        }
        return result
    }

    @Test
    fun `任意文字必须落在达标底上`() {
        val violations = mutableListOf<String>()
        val defaultBottom = colorDefs["surface"] ?: "#FFFFFFFF"
        layoutXml().forEach { file ->
            elementsOf(file).forEach { element ->
                val size = fontSizeOf(element)
                // 导航标签的字重写在 itemTextAppearance* 里，元素身上查不到
                val bold = resolveAttr(element, "android:textStyle") == "bold" || navLabelTypography(element)?.bold == true
                // 正文（≤16sp 或非 bold）4.5:1；大字（≥18sp 或 ≥14sp bold）允许 3.0:1
                val threshold = if (size >= 18 || (bold && size >= 14)) 3.0 else 4.5
                val viewAlpha = element.attrs["android:alpha"]?.toDoubleOrNull()
                val label = "${element.where} <${element.tag}> ${size}sp${if (bold) " bold" else ""}（门槛 %.1f:1）".format(threshold)

                // 前景清单：文字色 / 提示色 / 导航标签色（经 style 链与 selector 展开）+ 白图标
                val foregrounds = textColorsOf(element).toMutableList()
                if (element.attrs["app:tint"] == "@color/on_primary") {
                    foregrounds += TextForeground("白图标", "on_primary")
                }
                if (foregrounds.isEmpty()) return@forEach

                // 白字/白图标必须显式铺底：页面默认面就是 surface（1.00:1），找不到底即判不合格
                val declared = bottomColorsOf(element)
                // 导航标签的底由宿主控件自己的 android:background 决定，Material 组件没写背景时按 surface 判
                val needsExplicitBottom = foregrounds.any { it.colorName == "on_primary" && !it.navLabel }
                if (declared == null && needsExplicitBottom) {
                    violations += "$label ${foregrounds.joinToString("、") { it.role }} 找不到不透明底"
                    return@forEach
                }
                // 非白字找不到显式底时按页面默认 surface 判（窗口默认底就是白）
                val bottoms = declared ?: listOf("surface")

                for (bottomName in bottoms) {
                    val bottomHex = colorDefs[bottomName]
                    if (bottomHex == null) {
                        violations += "$label 底色 $bottomName 不在取值表"
                        continue
                    }
                    var bottom = argb(bottomHex)
                    var bottomNote = bottomName
                    if (bottom[0] < 1.0) {
                        // 半透明底按与页面默认 surface 的混合结果判；白字不给过
                        bottom = overlay(bottom, argb(defaultBottom), bottom[0])
                        bottomNote = "$bottomName（叠在 surface 上）"
                    }
                    for (fgEntry in foregrounds) {
                        // 失效控件豁免（WCAG 1.4.3）：selector 展开会把禁用态配色混进来，这组不判对比度
                        if (bottomName in disabledStateColors || fgEntry.colorName in disabledStateColors) continue
                        val role = "${fgEntry.role} @color/${fgEntry.colorName}"
                        val fgName = fgEntry.colorName
                        val fgHex = colorDefs[fgName]
                        if (fgHex == null) {
                            violations += "$label $role 不在取值表"
                            continue
                        }
                        if (fgName == "on_primary" && rawAlphaOf(bottomHex) < 1.0) {
                            violations += "$label $role 落在半透明底 $bottomName 上，半透明底不承载白字"
                            continue
                        }
                        // 视图 alpha 与颜色 alpha 都会把字调浅，按混合后的实际颜色判
                        val fg = argb(fgHex)
                        val alpha = (viewAlpha ?: 1.0) * fg[0]
                        val foreground = if (alpha >= 1.0) fg else overlay(fg, bottom, alpha)
                        val ratio = contrastRatio(foreground, bottom)
                        if (ratio < threshold) {
                            violations += "$label $role 落在 $bottomNote 上仅 %.2f:1".format(ratio)
                        }
                    }
                }
            }
        }
        assertTrue("文字与图标必须落在达标底色上，违规: $violations", violations.isEmpty())
    }

    // design.md D1 规定「图片底一律判不合格」，但上面的对比度闸门查不到图片底；登录页 banner 文字因此单列例外：
    // 必须彻底去底（@null）、用深色字 + 白色描边兜住图片暗部，这里把例外固化成断言，防止有人把底板加回来
    @Test
    fun `图片底上的文字必须去底并白色描边`() {
        val violations = mutableListOf<String>()
        listOf("activity_dt_log.xml", "activity_pt_log.xml").forEach { fileName ->
            val file = resDir.resolve("layout/$fileName")
            if (!file.isFile) {
                violations += "$fileName 不存在"
                return@forEach
            }
            val element = elementsOf(file).firstOrNull { it.attrs["android:id"] == "@+id/tvBannerText" }
            if (element == null) {
                violations += "$fileName 缺少 @+id/tvBannerText"
                return@forEach
            }
            if (element.attrs["android:background"] != "@null") {
                violations += "$fileName 的 banner 文字必须去底（android:background=\"@null\"）"
            }
            if (element.attrs["android:textColor"] != "@color/on_surface") {
                violations += "$fileName 的 banner 文字必须用 on_surface 深色"
            }
            if (element.attrs["android:shadowColor"] != "@color/on_primary") {
                violations += "$fileName 的 banner 文字必须声明白色描边（@color/on_primary）"
            }
            val radius = element.attrs["android:shadowRadius"]?.removeSuffix("dp")?.toIntOrNull() ?: 0
            if (radius < 3) {
                violations += "$fileName 的 banner 文字描边半径不足 3dp（当前 $radius）"
            }
        }
        assertTrue("图片底文字必须去底并描边，违规: $violations", violations.isEmpty())
    }

    // 取色值自身的不透明度，用于判定「半透明底不承载白字」
    private fun rawAlphaOf(hex: String): Double = argb(hex)[0]
}
