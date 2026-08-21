package com.goodsbuy.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 品牌渐变（一个主题一套，用于 Hero 横幅、渐变卡片、FAB）。 */
data class AppGradient(val start: Color, val end: Color)

/**
 * 一个主题 = 一套完整配置。未来新增皮肤只需在 [AppThemes] 里加一份配置，
 * 业务代码零改动（通过 [LocalAppTheme] 读取品牌渐变）。
 */
data class ThemeConfig(
    val id: String,
    val label: String,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
    val brandGradient: AppGradient,
    val darkBrandGradient: AppGradient
)

/** 默认主题：梦幻粉紫（浅色）+ 暗夜紫（深色）。 */
val DreamyPurpleTheme = ThemeConfig(
    id = "dreamy_purple",
    label = "梦幻粉紫",
    lightColors = lightColorScheme(
        primary = Color(0xFFC77DFF),
        secondary = Color(0xFFFF8FAB),
        tertiary = Color(0xFFFFB3D9),
        background = Color(0xFFFFF0F5),
        surface = Color(0xFFFFF5F9),
        surfaceVariant = Color(0xFFF7E6EF),
        onSurface = Color(0xFF4A4A5A),
        onSurfaceVariant = Color(0xFF7A7286),
        primaryContainer = Color(0xFFF0DFFF),
        onPrimaryContainer = Color(0xFF3D1F63),
        secondaryContainer = Color(0xFFFFDDE8),
        onSecondaryContainer = Color(0xFF5E1F3A)
    ).copy(surfaceContainer = Color(0xFFFBEAF2), surfaceContainerHigh = Color(0xFFF6E2EE)),
    darkColors = darkColorScheme(
        primary = Color(0xFFB388FF),
        secondary = Color(0xFFFF9EBB),
        tertiary = Color(0xFFFFB3D9),
        background = Color(0xFF1F1730),
        surface = Color(0xFF251B3A),
        surfaceVariant = Color(0xFF2A1E3F),
        onSurface = Color(0xFFF0EAFB),
        onSurfaceVariant = Color(0xFFB8AFCB),
        primaryContainer = Color(0xFF3A2A5A),
        onPrimaryContainer = Color(0xFFF0DFFF),
        secondaryContainer = Color(0xFF4A2A45),
        onSecondaryContainer = Color(0xFFFFDDE8)
    ).copy(surfaceContainer = Color(0xFF2E2342), surfaceContainerHigh = Color(0xFF362A4C)),
    brandGradient = AppGradient(Color(0xFFFF9EBB), Color(0xFFB388FF)),
    darkBrandGradient = AppGradient(Color(0xFF6A3E9C), Color(0xFFB388FF))
)

/** 主题注册表：所有可用主题。未来在「我的 → 显示设置」加选择器后由此遍历。 */
object AppThemes {
    val all: List<ThemeConfig> = listOf(DreamyPurpleTheme)

    fun byId(id: String): ThemeConfig = all.firstOrNull { it.id == id } ?: DreamyPurpleTheme
}

/** CompositionLocal：全局读取当前主题（品牌渐变、id、label）。 */
val LocalAppTheme = staticCompositionLocalOf { DreamyPurpleTheme }
