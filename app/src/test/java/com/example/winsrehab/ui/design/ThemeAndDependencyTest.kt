package com.example.winsrehab.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// 双端主题与依赖收敛的守卫：基础主题锁浅色、界面只挂双端主题、Compose 清零、Material 显式声明
class ThemeAndDependencyTest {

    private val themesXml = File("src/main/res/values/themes.xml").readText()
    private val manifest = File("src/main/AndroidManifest.xml").readText()
    private val appGradle = File("build.gradle.kts").readText()
    private val catalog = File("../gradle/libs.versions.toml").readText()

    // 取某个 style 标签的开标签内容（含 parent 等属性）
    private fun styleHeader(name: String): String {
        val match = Regex("<style name=\"$name\"([^>]*)>").find(themesXml)
        assertTrue("themes.xml 缺少 $name", match != null)
        return match!!.groupValues[1]
    }

    // 取某个 style 内部 colorPrimary 的取值
    private fun colorPrimaryOf(name: String): String {
        val match = Regex("<style name=\"$name\"[^>]*>(.*?)</style>", RegexOption.DOT_MATCHES_ALL).find(themesXml)
        assertTrue("themes.xml 缺少 $name", match != null)
        val item = Regex("<item name=\"colorPrimary\">([^<]+)</item>").find(match!!.groupValues[1])
        assertTrue("$name 未声明 colorPrimary", item != null)
        return item!!.groupValues[1]
    }

    @Test
    fun `基础主题锁定 Material 浅色`() {
        assertTrue(
            "基础主题必须以 MaterialComponents.Light 为父主题",
            styleHeader("Theme.WinsRehab").contains("parent=\"Theme.MaterialComponents.Light.NoActionBar\"")
        )
        assertFalse("仓库只有一套 values/，声明 DayNight 会跟随系统变深", themesXml.contains("DayNight"))
    }

    @Test
    fun `双端主题主色不同且取自取值表`() {
        val doctor = colorPrimaryOf("Theme.WinsRehab.Doctor")
        val patient = colorPrimaryOf("Theme.WinsRehab.Patient")
        assertNotEquals("医生端与患者端主色必须不同", doctor, patient)
        listOf(doctor, patient).forEach {
            assertTrue("主色必须引用取值表里的品牌色：$it", it.startsWith("@color/brand_"))
        }
        listOf("Theme.WinsRehab.Doctor", "Theme.WinsRehab.Patient").forEach {
            assertTrue("$it 必须有父主题 Theme.WinsRehab", styleHeader(it).trim().isEmpty())
        }
    }

    @Test
    fun `界面主题声明只指向双端主题`() {
        assertFalse("Manifest 不应再出现 Theme.AppCompat", manifest.contains("Theme.AppCompat"))
        val allowed = setOf(
            "@style/Theme.WinsRehab",
            "@style/Theme.WinsRehab.Doctor",
            "@style/Theme.WinsRehab.Patient",
            "@style/Theme.WinsRehab.Doctor.WithBar"
        )
        Regex("android:theme=\"([^\"]+)\"").findAll(manifest).forEach {
            assertTrue("未登记的主题引用：${it.groupValues[1]}", allowed.contains(it.groupValues[1]))
        }
        assertEquals("activity 声明应为 11 条", 11, Regex("<activity\\b").findAll(manifest).count())
    }

    @Test
    fun `仓库不再有 Compose 痕迹`() {
        val markers = listOf("androidx.compose", "setContent(", "@Composable", "WinsRehabTheme")
        val hits = mutableListOf<String>()
        File("src/main").walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }
            .forEach { file ->
                val text = file.readText()
                markers.forEach { marker -> if (text.contains(marker)) hits.add("${file.path} 含 $marker") }
            }
        assertTrue("仍有 Compose 痕迹：$hits", hits.isEmpty())
        assertFalse("构建文件不应再有 compose 开关", appGradle.contains("compose"))
        assertFalse("版本目录不应再有 Compose 条目", catalog.contains("androidx.compose"))
    }

    @Test
    fun `Material 组件库显式声明固定版本`() {
        assertTrue("版本目录需固定 material 版本", catalog.contains("material = \"1.12.0\""))
        assertTrue(
            "版本目录需有 material 库条目",
            catalog.contains("group = \"com.google.android.material\", name = \"material\"")
        )
        assertTrue("构建文件需显式依赖 material", appGradle.contains("implementation(libs.material)"))
    }
}
