# UI 现代化重构（梦幻粉紫主题）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将谷的拜（GoodsBuy）从 Material 默认紫色模板 UI 全面重设计为「梦幻粉紫渐变 + 暗夜紫暗色」的现代化界面，并建立可扩展多主题架构。

**Architecture:** 主题体系先行（`ThemeConfig` 数据驱动，浅/深配色 + 品牌渐变成对），再建立组件库（HeroHeader/SearchBar/GradientCard/StatNumber/AppFAB/ListRowItem/SectionHeader/SkeletonBox，改造 CollectibleCard/StatusChip/EmptyState），最后逐屏套用新组件。业务逻辑、数据层、导航结构零改动。

**Tech Stack:** Kotlin + Jetpack Compose + Material 3（compose-bom 2024.02.00）、Hilt、Room。构建命令 `./gradlew assembleDebug`，单测 `./gradlew testDebugUnitTest`。

**规格依据：** `docs/superpowers/specs/2026-08-20-ui-redesign-design.md`

---

## 文件结构总览

**新建（ui/theme/）**
- `ThemeConfig.kt` — `AppGradient`、`ThemeConfig`、`DreamyPurpleTheme`、`AppThemes` 注册表、`LocalAppTheme`
- `AppShapes.kt` — 圆角配置

**重写（ui/theme/）**
- `Color.kt` — 保留状态色与盈亏色
- `Type.kt` — 扩展排版层级
- `Theme.kt` — `GoodsBuyTheme(theme, darkTheme)` + `LocalAppTheme` provider

**新建（ui/components/）**
- `HeroHeader.kt`、`SearchBar.kt`、`GradientCard.kt`、`StatNumber.kt`、`AppFAB.kt`、`ListRowItem.kt`、`SectionHeader.kt`、`SkeletonBox.kt`

**重写（ui/components/）**
- `StatusChip.kt`、`EmptyState.kt`、`CollectibleCard.kt`

**修改（屏幕/VM）**
- `MainActivity.kt`、`CollectibleListScreen.kt`、`CollectibleListUiState.kt`、`CollectibleListViewModel.kt`、`StatisticsScreen.kt`、`ProfileScreen.kt`、`GalleryScreen.kt`、`CollectibleDetailScreen.kt`、`CollectibleFormScreen.kt`、`ImportPreviewScreen.kt`、`EdgeFadeEditScreen.kt`、`ui/preferences/PreferencesRepository.kt`

**新建测试**
- `app/src/test/java/com/goodsbuy/app/ui/theme/ThemeRegistryTest.kt`

---

## Phase A：主题体系

### Task 1: ThemeConfig 数据模型 + 注册表 + 单测

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/theme/ThemeConfig.kt`
- Test: `app/src/test/java/com/goodsbuy/app/ui/theme/ThemeRegistryTest.kt`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.goodsbuy.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {

    @Test
    fun `byId returns DreamyPurple for known id`() {
        assertEquals(DreamyPurpleTheme, AppThemes.byId("dreamy_purple"))
    }

    @Test
    fun `byId falls back to DreamyPurple for unknown id`() {
        assertEquals(DreamyPurpleTheme, AppThemes.byId("not_exists"))
    }

    @Test
    fun `dreamy theme has distinct light and dark backgrounds`() {
        assertNotEquals(
            DreamyPurpleTheme.lightColors.background,
            DreamyPurpleTheme.darkColors.background
        )
    }

    @Test
    fun `dreamy theme defines brand gradients with distinct endpoints`() {
        assertNotEquals(
            DreamyPurpleTheme.brandGradient.start,
            DreamyPurpleTheme.brandGradient.end
        )
        assertTrue(DreamyPurpleTheme.all.contains(DreamyPurpleTheme))
    }
}
```

注意：`ThemeConfig` 持有 `AppThemes.all` 会造成循环引用（`all` 引用了 `DreamyPurpleTheme`）。修正：`AppThemes.all` 直接 `listOf(DreamyPurpleTheme)`，测试改为断言 `AppThemes.all.size >= 1`。

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.goodsbuy.app.ui.theme.ThemeRegistryTest"`
Expected: FAIL（`ThemeConfig` / `DreamyPurpleTheme` / `AppThemes` 不存在，编译错误）

- [ ] **Step 3: 实现 ThemeConfig.kt**

```kotlin
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
```

- [ ] **Step 4: 修正测试后运行确认通过**

把测试中 `assertTrue(DreamyPurpleTheme.all.contains(DreamyPurpleTheme))` 改为 `assertTrue(AppThemes.all.size >= 1)`，`import com.goodsbuy.app.ui.theme.AppThemes` 显式导入。

Run: `.\gradlew.bat testDebugUnitTest --tests "com.goodsbuy.app.ui.theme.ThemeRegistryTest"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/theme/ThemeConfig.kt app/src/test/java/com/goodsbuy/app/ui/theme/ThemeRegistryTest.kt
git commit -m "feat(theme): 多主题架构 ThemeConfig + 梦幻粉紫默认主题"
```

---

### Task 2: 重写 Type.kt（排版层级）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/theme/Type.kt`（整文件替换）

- [ ] **Step 1: 替换 Type.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/theme/Type.kt
git commit -m "feat(theme): 扩展排版层级（大标题/正文/数字）"
```

---

### Task 3: 新增 AppShapes.kt

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/theme/AppShapes.kt`

- [ ] **Step 1: 创建文件**

```kotlin
package com.goodsbuy.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 全 App 统一的圆角体系：卡片 20dp、按钮/胶囊 28dp、图片 12dp、chip 8dp。 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/theme/AppShapes.kt
git commit -m "feat(theme): 统一圆角体系 AppShapes"
```

---

### Task 4: 精简 Color.kt（保留状态/盈亏色）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/theme/Color.kt`（整文件替换）

- [ ] **Step 1: 替换 Color.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.theme

import androidx.compose.ui.graphics.Color

// 状态色（与 OrderStatus.colorHex 保持一致，供需要显式引用的地方使用）
val StatusPendingTail = Color(0xFFFF9800)
val StatusPendingShippingFee = Color(0xFF2196F3)
val StatusPendingSend = Color(0xFF9C27B0)
val StatusInTransit = Color(0xFF00BCD4)
val StatusOwned = Color(0xFF4CAF50)
val StatusHesitatingSell = Color(0xFFFFB74D)
val StatusListed = Color(0xFF42A5F5)
val StatusSold = Color(0xFF9E9E9E)
val StatusGift = Color(0xFFE91E63)
val StatusLost = Color(0xFFF44336)

val ProfitGreen = Color(0xFF4CAF50)
val LossRed = Color(0xFFF44336)
```

（删除默认模板的 Purple40/Purple80/PurpleGrey/Pink 常量；全项目仅 `Theme.kt` 引用过，下一步会重写。）

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: 当前会失败——因为 `Theme.kt` 还引用 `Purple40`。本任务与 Task 5 必须一起提交，先继续 Task 5。

---

### Task 5: 重写 Theme.kt（主题入口 + LocalAppTheme）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/theme/Theme.kt`（整文件替换）

- [ ] **Step 1: 替换 Theme.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GoodsBuyTheme(
    theme: ThemeConfig = DreamyPurpleTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) theme.darkColors else theme.lightColors
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL（Task 4 + Task 5 一起消除 Purple 引用）

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/theme/Color.kt app/src/main/java/com/goodsbuy/app/ui/theme/Theme.kt
git commit -m "feat(theme): 重写主题入口，支持 ThemeConfig 切换与品牌渐变"
```

---

## Phase B：组件库

### Task 6: HeroHeader 品牌渐变横幅

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/HeroHeader.kt`

- [ ] **Step 1: 创建文件**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppTheme

/** 页面顶部品牌渐变横幅（大圆角卡），承载标题、副标题与看板内容。 */
@Composable
fun HeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val gradient = if (isSystemInDarkTheme()) theme.darkBrandGradient else theme.brandGradient
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(gradient.start, gradient.end)))
            .padding(20.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            content()
        }
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/HeroHeader.kt
git commit -m "feat(ui): HeroHeader 品牌渐变横幅组件"
```

---

### Task 7: SearchBar 胶囊搜索框

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/SearchBar.kt`

- [ ] **Step 1: 创建文件**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.components.colorAnimSpec

/** 胶囊形搜索框：浅色底、无边框、带清除按钮。 */
@Composable
fun SearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onClear: (() -> Unit)? = null
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = value.text.isNotEmpty() && onClear != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { onClear?.invoke() }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}
```

注意：`SearchBar.kt` 内部使用 `MaterialTheme.colorScheme.surfaceContainerHigh`。该 token 来自 `ThemeConfig` 中 `.copy(surfaceContainerHigh=...)`，若编译报「Unresolved reference」，说明当前 material3 版本没有该属性——把 `.background(MaterialTheme.colorScheme.surfaceContainerHigh)` 降级为 `surfaceVariant`，并在 `ThemeConfig.kt` 中删除 `.copy(surfaceContainerHigh=...)` 部分（保留 `surfaceContainer`，它存在于 1.1.0+）。

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/SearchBar.kt
git commit -m "feat(ui): SearchBar 胶囊搜索框组件"
```

---

### Task 8: GradientCard + StatNumber

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/GradientCard.kt`
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/StatNumber.kt`

- [ ] **Step 1: 创建 GradientCard.kt**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppTheme

/** 渐变底信息卡，默认使用当前主题品牌渐变。 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val brand = if (isSystemInDarkTheme()) theme.darkBrandGradient else theme.brandGradient
    val colors = gradient ?: listOf(brand.start, brand.end)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}
```

- [ ] **Step 2: 创建 StatNumber.kt**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFeatureSettings

/** 看板/统计数字：加粗、等宽数字（tnum），默认 ¥ 前缀，保留滚动动画。 */
@Composable
fun StatNumber(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    decimals: Int = 0,
    prefix: String = "\u00a5"
) {
    val tabular = style.copy(fontFeatureSettings = FontFeatureSettings("tnum"))
    AnimatedNumber(
        targetValue = value,
        modifier = modifier,
        style = tabular,
        color = color,
        decimals = decimals,
        prefix = prefix
    )
}
```

注意：`FontFeatureSettings` 位于 `androidx.compose.ui.text.font.FontFeatureSettings`，接受 `"tnum"` 字符串；若该 API 在此版本不可用，改用 `style.copy(fontFeatureSettings = "tnum")`（TextStyle 属性本身存在），或直接去掉 `fontFeatureSettings` 只保留加粗。

- [ ] **Step 3: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/GradientCard.kt app/src/main/java/com/goodsbuy/app/ui/components/StatNumber.kt
git commit -m "feat(ui): GradientCard 渐变卡 + StatNumber 等宽数字组件"
```

---

### Task 9: AppFAB 渐变悬浮按钮

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/AppFAB.kt`

- [ ] **Step 1: 创建文件**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppTheme

/** 主题渐变悬浮按钮（品牌渐变 + 28dp 圆角）。 */
@Composable
fun AppFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val brand = if (isSystemInDarkTheme()) theme.darkBrandGradient else theme.brandGradient
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(brand.start, brand.end)))
            .clickable(
                onClick = onClick,
                indication = rememberRipple(color = androidx.compose.ui.graphics.Color.White)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/AppFAB.kt
git commit -m "feat(ui): AppFAB 渐变悬浮按钮组件"
```

---

### Task 10: ListRowItem 设置行

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/ListRowItem.kt`

- [ ] **Step 1: 创建文件**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.components.colorAnimSpec

/** 设置/列表行：彩色圆形图标底 + 标题 + 可选副标题 + 可选尾部 + 箭头。 */
@Composable
fun ListRowItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/ListRowItem.kt
git commit -m "feat(ui): ListRowItem 设置行组件"
```

---

### Task 11: SectionHeader + SkeletonBox

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/SectionHeader.kt`
- Create: `app/src/main/java/com/goodsbuy/app/ui/components/SkeletonBox.kt`

- [ ] **Step 1: 创建 SectionHeader.kt**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/** 分区标题：左对齐标题 + 可选副标题。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: 创建 SkeletonBox.kt**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme

/** 加载骨架屏：柔和 shimmer 占位块。 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val x by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "skeleton_x"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(x, 0f),
                    end = Offset(x + 300f, 300f)
                )
            )
    )
}
```

- [ ] **Step 3: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/SectionHeader.kt app/src/main/java/com/goodsbuy/app/ui/components/SkeletonBox.kt
git commit -m "feat(ui): SectionHeader 分区标题 + SkeletonBox 骨架屏"
```

---

### Task 12: 重写 StatusChip（圆点 + 浅底胶囊）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/components/StatusChip.kt`（整文件替换）

- [ ] **Step 1: 替换 StatusChip.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.OrderStatus

/** 状态 chip：彩色圆点 + 浅色底胶囊。 */
@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = Color(status.colorHex),
        animationSpec = colorAnimSpec,
        label = "status_chip_color"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            status.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/StatusChip.kt
git commit -m "feat(ui): StatusChip 改彩色圆点浅底胶囊"
```

---

### Task 13: 重写 EmptyState

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/components/EmptyState.kt`（整文件替换）

- [ ] **Step 1: 替换 EmptyState.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** 空状态：圆角插画卡 + 引导文案。 */
@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String = "还没有藏品，点击 + 添加") {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "empty_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "empty_offset"
    )

    Box(
        modifier = modifier.fillMaxSize().padding(32.dp).alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

（注意：原实现用 `Modifier.offset(y = offsetY.dp)` 上移入场；本实现用 alpha + 卡片圆角。如需保留上移动画，在 `Box` 加 `.offset { IntOffset(0, offsetY.dp.roundToPx()) }` 并 `import androidx.compose.ui.unit.roundToPx`。）

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/EmptyState.kt
git commit -m "feat(ui): EmptyState 改圆角插画卡"
```

---

### Task 14: 重写 CollectibleCard（渐变遮罩 + 状态白底胶囊）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/components/CollectibleCard.kt`（整文件替换）

- [ ] **Step 1: 替换 CollectibleCard.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.goodsbuy.app.domain.model.Collectible
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectibleCard(
    collectible: Collectible,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 140.dp,
    showName: Boolean = true,
    showPrice: Boolean = true,
    showStatus: Boolean = true,
    fontSize: Int = 1,
    homeImageAutoRotate: Boolean = false,
    homeImageRotationIntervalSeconds: Int = 3,
    onLongPress: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    batchMode: Boolean = false
) {
    val nameSize = when (fontSize) { 0 -> 11f; 1 -> 12f; else -> 14f }
    val priceSize = when (fontSize) { 0 -> 9f; 1 -> 10f; else -> 12f }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = pressScaleSpring,
        label = "card_scale"
    )
    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = fadeAnimSpec,
        label = "card_selection"
    )
    val statusColor by animateColorAsState(
        targetValue = Color(collectible.status.colorHex),
        animationSpec = colorAnimSpec,
        label = "card_status_color"
    )

    Card(
        modifier = modifier
            .width(cardSize)
            .aspectRatio(0.75f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = { if (batchMode) onSelect?.invoke() else onClick() },
                onLongClick = { if (onLongPress != null) onLongPress() },
                enabled = !batchMode || onSelect != null
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (collectible.imagePaths.isNotEmpty()) {
                CardImage(
                    collectible = collectible,
                    autoRotate = homeImageAutoRotate,
                    rotationIntervalSeconds = homeImageRotationIntervalSeconds
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (showStatus) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xE6FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = collectible.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (selectionAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000).copy(alpha = 0.5f * selectionAlpha)),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { onSelect?.invoke() },
                        modifier = Modifier.padding(6.dp).size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = selectionAlpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (showName) {
                            Text(
                                text = collectible.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = nameSize.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (showPrice) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\u00a5${collectible.purchasePrice}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = priceSize.sp),
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                    if (collectible.imagePaths.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = Color(0x99FFFFFF)) {
                                Text(
                                    "+${collectible.imagePaths.size - 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardImage(
    collectible: Collectible,
    autoRotate: Boolean,
    rotationIntervalSeconds: Int
) {
    val imagePaths = collectible.imagePaths
    if (!autoRotate || imagePaths.size <= 1) {
        AsyncImage(
            model = imagePaths.first(),
            contentDescription = collectible.name,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        return
    }

    var imageIndex by remember(collectible.id, imagePaths) { mutableIntStateOf(0) }
    LaunchedEffect(collectible.id, imagePaths, rotationIntervalSeconds) {
        imageIndex = 0
        val intervalMillis = rotationIntervalSeconds.coerceIn(1, 60) * 1_000L
        while (true) {
            delay(intervalMillis)
            imageIndex = (imageIndex + 1) % imagePaths.size
        }
    }

    AnimatedContent(
        targetState = imagePaths[imageIndex],
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "card_image_rotation"
    ) { path ->
        AsyncImage(
            model = path,
            contentDescription = collectible.name,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/CollectibleCard.kt
git commit -m "feat(ui): CollectibleCard 渐变遮罩 + 白底状态胶囊"
```

---

## Phase C：逐屏改造

### Task 15: 藏品柜首页（HeroHeader + SearchBar + FAB + 看板摘要）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListUiState.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListViewModel.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt`（整文件替换）
- Modify: `app/src/main/java/com/goodsbuy/app/MainActivity.kt`（Scaffold 容器色）

- [ ] **Step 1: 给 UiState 增加 summary 字段**

替换 `CollectibleListUiState.kt` 中的 `data class CollectibleListUiState(...)`，在末尾新增字段：

```kotlin
import com.goodsbuy.app.domain.model.DashboardSummary
// ...
data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val isBatchMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val sortField: SortField = SortField.CREATED_AT,
    val sortAscending: Boolean = false,
    val summary: DashboardSummary? = null
)
```

- [ ] **Step 2: VM 注入 GetDashboardSummaryUseCase 并计算 summary**

在 `CollectibleListViewModel.kt`：
1. 构造函数新增参数 `private val getDashboardSummary: GetDashboardSummaryUseCase`（`import com.goodsbuy.app.domain.usecase.GetDashboardSummaryUseCase`）。
2. `uiState` 的 combine 里，在 `.map { list -> sortList(...) }` 之后追加 summary 计算，将 map 改为：

```kotlin
                flow.map { list -> sortList(list, sortField, asc) }
                    .map { list ->
                        CollectibleListUiState(
                            collectibles = list,
                            isLoading = false,
                            searchQuery = query,
                            selectedStatusFilter = status,
                            sortField = sortField,
                            sortAscending = asc,
                            summary = getDashboardSummary(list)
                        )
                    }
```

- [ ] **Step 3: 替换 CollectibleListScreen.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.collectible.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.collectible.list.SortField
import com.goodsbuy.app.ui.components.AppFAB
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.SearchBar
import com.goodsbuy.app.ui.components.StatNumber
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CollectibleListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    onNavigateToGallery: () -> Unit = {},
    preferencesRepository: PreferencesRepository,
    viewModel: CollectibleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingDeletion by viewModel.pendingDeletion.collectAsState()
    val menuState by viewModel.longPressMenuState.collectAsState()
    val prefs = preferencesRepository.preferencesState.value
    val snackbarHostState = remember { SnackbarHostState() }

    var searchText by remember { mutableStateOf(TextFieldValue(uiState.searchQuery)) }
    LaunchedEffect(uiState.searchQuery) {
        if (searchText.text != uiState.searchQuery) {
            searchText = TextFieldValue(uiState.searchQuery, selection = androidx.compose.ui.text.TextRange(uiState.searchQuery.length))
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Collectible?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(pendingDeletion?.token) {
        val pending = pendingDeletion ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "已删除 ${pending.collectibles.size} 件藏品",
            actionLabel = "撤销",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
    }

    if (menuState != null) {
        LongPressMenu(
            state = menuState!!,
            onDismiss = { viewModel.hideLongPressMenu() },
            onQuickStatus = { status -> viewModel.quickUpdateStatus(menuState!!.collectible, status) },
            onEdit = { onNavigateToForm(menuState!!.collectible.id) },
            onDuplicate = { viewModel.duplicateCollectible(menuState!!.collectible) },
            onDelete = {
                pendingDelete = menuState?.collectible
                showDeleteConfirm = true
            },
            onBatchSelect = { viewModel.enterBatchMode(menuState!!.collectible.id) }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; pendingDelete = null },
            title = { Text("确认删除") },
            text = {
                if (uiState.isBatchMode) {
                    Text("确定要删除选中的 ${uiState.selectedIds.size} 件藏品吗？删除后可在提示出现时撤销。")
                } else {
                    Text("确定要删除「${pendingDelete?.name}」吗？删除后可在提示出现时撤销。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (uiState.isBatchMode) {
                        viewModel.batchDelete()
                        viewModel.exitBatchMode()
                    } else {
                        pendingDelete?.id?.let { viewModel.batchDeleteSingle(it) }
                    }
                    pendingDelete = null
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; pendingDelete = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = uiState.isBatchMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "topbar_mode"
            ) { isBatch ->
                if (isBatch) {
                    TopAppBar(
                        title = { Text("批量操作  (${uiState.selectedIds.size} 项)") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.exitBatchMode() }) {
                                Icon(Icons.Default.Close, contentDescription = "取消")
                            }
                        },
                        actions = {
                            val allSelected = uiState.selectedIds.size == uiState.collectibles.size && uiState.collectibles.isNotEmpty()
                            TextButton(onClick = {
                                if (allSelected) {
                                    uiState.selectedIds.forEach { viewModel.toggleSelect(it) }
                                } else {
                                    uiState.collectibles.forEach { c ->
                                        if (!uiState.selectedIds.contains(c.id)) viewModel.toggleSelect(c.id)
                                    }
                                }
                            }) {
                                Text(if (allSelected) "取消全选" else "全选")
                            }
                            TextButton(onClick = {
                                pendingDelete = null
                                showDeleteConfirm = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text("谷的拜", style = MaterialTheme.typography.titleLarge) },
                        actions = {
                            if (prefs.galleryEntryHome) {
                                IconButton(onClick = onNavigateToGallery) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = "打开图鉴")
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.isBatchMode) {
                val summary = uiState.summary
                HeroHeader(
                    title = "我的藏品柜",
                    subtitle = "共 ${uiState.collectibles.size} 件",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (summary != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column {
                                Text("持仓价值", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                                StatNumber(
                                    value = summary.holdingValue,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    prefix = "\u00a5"
                                )
                            }
                            Column {
                                Text("盈亏率", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                                Text(
                                    text = if (summary.totalProfit >= 0) "+${String.format("%.1f", summary.totalProfitRate)}%" else "${String.format("%.1f", summary.totalProfitRate)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (summary.totalProfit >= 0) Color(0xFFE8FFEE) else Color(0xFFFFE4E4),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            SearchBar(
                value = searchText,
                onValueChange = { searchText = it; viewModel.onSearchQueryChange(it.text) },
                placeholder = "搜索藏品名称、IP、角色…",
                onClear = { searchText = TextFieldValue(""); viewModel.onSearchQueryChange("") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { viewModel.onStatusFilterChange(null) },
                        label = { Text("全部") }
                    )
                }
                items(OrderStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.selectedStatusFilter == status.name,
                        onClick = { viewModel.onStatusFilterChange(status.name) },
                        label = { Text(status.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (prefs.showSortControl && !uiState.isBatchMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        OutlinedButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("排序: ${uiState.sortField.label}")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortField.entries.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    onClick = {
                                        viewModel.onSortFieldChange(field)
                                        sortMenuExpanded = false
                                    },
                                    trailingIcon = if (uiState.sortField == field) {
                                        { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.onSortDirectionToggle() }) {
                        Icon(
                            if (uiState.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "切换升序/降序",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedContent(
                        targetState = uiState.sortAscending,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "sort_dir"
                    ) { ascending ->
                        Text(
                            if (ascending) "升序" else "降序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            AnimatedContent(
                targetState = uiState.collectibles.isEmpty() && !uiState.isLoading,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "list_or_empty"
            ) { showEmpty ->
                if (showEmpty) {
                    EmptyState()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (uiState.isBatchMode) 3 else prefs.columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.collectibles, key = { it.id }) { collectible ->
                            CollectibleCard(
                                collectible = collectible,
                                onClick = { onNavigateToDetail(collectible.id) },
                                cardSize = prefs.cardSize.dp,
                                showName = prefs.showName,
                                showPrice = prefs.showPrice,
                                showStatus = prefs.showStatus,
                                fontSize = prefs.fontSize,
                                homeImageAutoRotate = prefs.homeImageAutoRotate,
                                homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds,
                                onLongPress = { if (!uiState.isBatchMode) viewModel.showLongPressMenu(collectible) },
                                isSelected = uiState.selectedIds.contains(collectible.id),
                                onSelect = { viewModel.toggleSelect(collectible.id) },
                                batchMode = uiState.isBatchMode,
                                modifier = Modifier.animateItemPlacement(tween(250))
                            )
                        }
                    }
                }
            }
        }
    }

    if (!uiState.isBatchMode) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            AppFAB(
                onClick = { onNavigateToForm(null) },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = Color.White)
            }
        }
    }
}
```

- [ ] **Step 4: MainActivity Scaffold 容器色**

在 `MainActivity.kt` 的 `MainScreen` 的 `Scaffold(...)` 增加参数：

```kotlin
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ...
```

- [ ] **Step 5: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListUiState.kt app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListViewModel.kt app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt app/src/main/java/com/goodsbuy/app/MainActivity.kt
git commit -m "feat(ui): 藏品柜首页 HeroHeader+SearchBar+FAB+看板摘要"
```

---

### Task 16: 统计页（Hero + 2x2 渐变卡 + 分类行）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/statistics/StatisticsScreen.kt`（整文件替换）

- [ ] **Step 1: 替换 StatisticsScreen.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.components.AnimatedInt
import com.goodsbuy.app.ui.components.GradientCard
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ProfitLossText
import com.goodsbuy.app.ui.components.SectionHeader
import com.goodsbuy.app.ui.components.StatNumber
import com.goodsbuy.app.ui.theme.LossRed
import com.goodsbuy.app.ui.theme.ProfitGreen

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilter by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedStatus) {
        viewModel.changeStatusFilter(selectedStatus)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroHeader(
                title = "统计",
                subtitle = "${uiState.summary.totalCount} 件藏品"
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text("累计盈亏", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        text = if (uiState.summary.totalProfit >= 0) "+" else "",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFFFF8FAB), Color(0xFFFFB3D9))
                ) {
                    Text("总投入", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    StatNumber(
                        value = uiState.summary.totalInvestment,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        decimals = 0
                    )
                }
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFFB388FF), Color(0xFF9B5CFF))
                ) {
                    Text("总营收", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    StatNumber(
                        value = uiState.summary.totalRevenue,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        decimals = 0
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(ProfitGreen, Color(0xFF66BB6A))
                ) {
                    Text("累计盈亏", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    StatNumber(
                        value = uiState.summary.totalProfit,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        decimals = 0
                    )
                }
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFF42A5F5), Color(0xFF5C6BC0))
                ) {
                    Text("持仓市值", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    StatNumber(
                        value = uiState.summary.holdingValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        decimals = 0
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("藏品总数", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            AnimatedInt(
                                targetValue = uiState.summary.totalCount,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                " (持有${uiState.summary.ownedCount}/已售${uiState.summary.soldCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("盈亏比例", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${String.format("%.1f", uiState.summary.totalProfitRate)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.summary.totalProfit >= 0) ProfitGreen else LossRed
                        )
                    }
                }
            }
        }

        item {
            MonthlyTrendChart(stats = uiState.monthlyStats)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "分类统计")
                IconButton(onClick = { showFilter = !showFilter }) {
                    Icon(Icons.Default.FilterList, contentDescription = "筛选状态")
                }
            }

            AnimatedVisibility(
                visible = showFilter,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("全部") }
                    )
                    OrderStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status.name,
                            onClick = { selectedStatus = status.name },
                            label = { Text(status.displayName) }
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.categoryType == "ip", onClick = { viewModel.changeCategoryType("ip") }, label = { Text("按IP") })
                FilterChip(selected = uiState.categoryType == "series", onClick = { viewModel.changeCategoryType("series") }, label = { Text("按系列") })
                FilterChip(selected = uiState.categoryType == "category", onClick = { viewModel.changeCategoryType("category") }, label = { Text("按品类") })
            }
        }

        items(uiState.categoryStats, key = { it.categoryName }) { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stat.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text("${stat.count}件 \u00b7 投入\u00a5${String.format("%.0f", stat.investment)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ProfitLossText(profitLoss = ProfitLoss(stat.investment, stat.revenue, stat.profit, if (stat.investment > 0) (stat.profit / stat.investment) * 100 else 0.0))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
```

注意：Hero 里「累计盈亏」数字使用了 `Text` 而非 `StatNumber`（避免滚动动画与占位文本 `+` 冲突），如需动画可后续优化。该文本当前直接渲染，请把它合并为带符号的单行：

```kotlin
Text(
    text = buildString {
        if (uiState.summary.totalProfit >= 0) append("+")
        append("\u00a5")
        append(String.format("%.0f", uiState.summary.totalProfit))
    },
    style = MaterialTheme.typography.displaySmall,
    color = Color.White,
    fontWeight = FontWeight.Bold
)
```

用上面这段替换原 Hero 里的两段 Text（「+」与数字合并为一行）。

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/statistics/StatisticsScreen.kt
git commit -m "feat(ui): 统计页 Hero+2x2 渐变卡+分类行"
```

---

### Task 17: 我的页（设置中心 + ListRowItem）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`（整文件替换）

- [ ] **Step 1: 替换 ProfileScreen.kt 全部内容**

```kotlin
package com.goodsbuy.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.BuildConfig
import com.goodsbuy.app.ui.backup.ImportPreviewScreen
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ListRowItem
import com.goodsbuy.app.ui.components.SectionHeader
import kotlinx.coroutines.launch
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToForm: (Long?) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var showSettings by remember { mutableStateOf(false) }
    var showDrafts by remember { mutableStateOf(false) }
    var showDeleteLogsDialog by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImportedUri(it)
            viewModel.previewImport(it)
        }
    }

    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(
            outputUri = uri,
            onSuccess = { scope.launch { snackbarHostState.showSnackbar("导出成功") } },
            onFailure = { message -> scope.launch { snackbarHostState.showSnackbar("导出失败: $message") } }
        )
    }

    val currentImportPreview by viewModel.importPreview.collectAsState()
    val currentImportMode by viewModel.importMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val drafts by viewModel.drafts.collectAsState()

    if (currentImportPreview != null) {
        ImportPreviewScreen(
            preview = currentImportPreview!!,
            importMode = currentImportMode,
            onModeChange = { viewModel.setImportMode(it) },
            onConfirm = {
                scope.launch {
                    viewModel.confirmImport(
                        onSuccess = { count -> snackbarHostState.showSnackbar("成功导入 $count 条藏品") },
                        onFailure = { message -> snackbarHostState.showSnackbar("导入失败: $message") }
                    )
                }
            },
            onDismiss = { viewModel.clearPreview() },
            isImporting = isImporting,
            importProgress = importProgress
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (showSettings) "显示设置" else if (showDrafts) "草稿箱" else "我的") },
                navigationIcon = {
                    if (showSettings || showDrafts) {
                        IconButton(onClick = { showSettings = false; showDrafts = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSettings) {
                SettingsContent(
                    prefs = prefs,
                    onPrefsChange = { prefs = it; preferencesRepository.save(it) },
                    showDeleteLogsDialog = { showDeleteLogsDialog = true }
                )
            } else if (showDrafts) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (drafts.isEmpty()) {
                            Text("暂无草稿", style = MaterialTheme.typography.bodyLarge)
                            Text("在添加或编辑藏品时离开页面，内容会自动保存在这里。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            drafts.forEach { draft ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(draft.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(if (draft.id == null) "新藏品草稿" else "编辑藏品草稿", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { onNavigateToForm(draft.id) }) { Text("继续编辑") }
                                    IconButton(onClick = { viewModel.deleteDraft(draft.key) }) {
                                        Icon(Icons.Default.Close, contentDescription = "删除草稿")
                                    }
                                }
                                if (draft != drafts.last()) HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HeroHeader(
                        title = "我的",
                        subtitle = "谷的拜 v${BuildConfig.VERSION_NAME}"
                    )

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SectionHeader(title = "数据")
                            ListRowItem(
                                icon = Icons.Default.CloudDownload,
                                title = "导出备份",
                                subtitle = "导出所有藏品及图片",
                                onClick = {
                                    val fileName = "谷的拜备份_${System.currentTimeMillis()}.zip"
                                    exportLauncher.launch(fileName)
                                }
                            )
                            ListRowItem(
                                icon = Icons.Default.CloudUpload,
                                title = "导入备份",
                                subtitle = "从 ZIP 文件恢复数据",
                                onClick = { importLauncher.launch("application/zip") }
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SectionHeader(title = "管理")
                            ListRowItem(
                                icon = Icons.Default.Edit,
                                title = "草稿箱",
                                subtitle = "${drafts.size} 条未完成草稿",
                                onClick = { showDrafts = true; viewModel.refreshDrafts() }
                            )
                            ListRowItem(
                                icon = Icons.Default.Settings,
                                title = "显示设置",
                                subtitle = "每行数量、卡片大小、字体、轮询等",
                                onClick = { showSettings = true }
                            )
                            if (!prefs.galleryEntryHome) {
                                ListRowItem(
                                    icon = Icons.Default.PhotoLibrary,
                                    title = "图鉴模式",
                                    subtitle = "按 IP/系列分类查看",
                                    onClick = { onNavigateToGallery() }
                                )
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SectionHeader(title = "其他")
                            ListRowItem(
                                icon = Icons.Default.Info,
                                title = "关于谷的拜",
                                subtitle = "v${BuildConfig.VERSION_NAME}"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteLogsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLogsDialog = false },
            title = { Text("删除日志") },
            text = { Text("将删除 app.log 和 crash.log 以释放空间，删除后仍会继续记录新日志。") },
            confirmButton = {
                TextButton(onClick = {
                    com.goodsbuy.app.util.AppLogger.deleteLogs()
                    showDeleteLogsDialog = false
                    scope.launch { snackbarHostState.showSnackbar("日志已删除") }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLogsDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 显示设置内容（从原 ProfileScreen 迁移，换新组件）。 */
@Composable
private fun SettingsContent(
    prefs: com.goodsbuy.app.ui.preferences.GridPreferences,
    onPrefsChange: (com.goodsbuy.app.ui.preferences.GridPreferences) -> Unit,
    showDeleteLogsDialog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("显示设置", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("每行展示数量", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (prefs.columns > 1) { onPrefsChange(prefs.copy(columns = prefs.columns - 1)) }
                    }) { Text("-", style = MaterialTheme.typography.headlineMedium) }
                    Text("${prefs.columns}", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        if (prefs.columns < 4) { onPrefsChange(prefs.copy(columns = prefs.columns + 1)) }
                    }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("卡片大小", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (prefs.cardSize > 100) { onPrefsChange(prefs.copy(cardSize = prefs.cardSize - 20)) }
                    }) { Text("-", style = MaterialTheme.typography.headlineMedium) }
                    Text("${prefs.cardSize}dp", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        if (prefs.cardSize < 200) { onPrefsChange(prefs.copy(cardSize = prefs.cardSize + 20)) }
                    }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("字体大小", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val labels = listOf("小", "中", "大")
                    labels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = prefs.fontSize == idx,
                            onClick = { onPrefsChange(prefs.copy(fontSize = idx)) },
                            label = { Text(label) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            SettingToggleRow("显示名称", prefs.showName) { onPrefsChange(prefs.copy(showName = it)) }
            SettingToggleRow("显示价格", prefs.showPrice) { onPrefsChange(prefs.copy(showPrice = it)) }
            SettingToggleRow("显示状态", prefs.showStatus) { onPrefsChange(prefs.copy(showStatus = it)) }
            SettingToggleRow("显示排序栏", prefs.showSortControl) { onPrefsChange(prefs.copy(showSortControl = it)) }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("草稿自动保存间隔", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "停止编辑后保存，推荐 0.5 秒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val delayOptions = com.goodsbuy.app.ui.preferences.PreferencesRepository.DRAFT_AUTO_SAVE_DELAY_OPTIONS.map { delayMillis ->
                    delayMillis to if (delayMillis == 500L) "0.5 秒" else "${delayMillis / 1_000} 秒"
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    delayOptions.forEachIndexed { index, (delayMillis, label) ->
                        SegmentedButton(
                            selected = prefs.draftAutoSaveDelayMillis == delayMillis,
                            onClick = { onPrefsChange(prefs.copy(draftAutoSaveDelayMillis = delayMillis)) },
                            shape = SegmentedButtonDefaults.itemShape(index, delayOptions.size),
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider()

            SettingToggleRow("首页多图自动轮询", prefs.homeImageAutoRotate) {
                onPrefsChange(prefs.copy(homeImageAutoRotate = it))
            }
            Text(
                "开启后，首页当前屏幕中的多图片藏品会自动切换封面",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("轮询间隔", style = MaterialTheme.typography.bodyLarge)
                    Text("每张图片停留的时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (prefs.homeImageRotationIntervalSeconds > 1) {
                                onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds - 1))
                            }
                        },
                        enabled = prefs.homeImageRotationIntervalSeconds > 1
                    ) { Text("−", style = MaterialTheme.typography.headlineMedium) }
                    Text("${prefs.homeImageRotationIntervalSeconds} 秒", style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = {
                            if (prefs.homeImageRotationIntervalSeconds < 60) {
                                onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds + 1))
                            }
                        },
                        enabled = prefs.homeImageRotationIntervalSeconds < 60
                    ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                }
            }

            HorizontalDivider()

            Text("图鉴入口", style = MaterialTheme.typography.bodyLarge)
            Text(
                "选择图鉴入口显示的位置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("藏品柜", "我的").forEachIndexed { index, label ->
                    val selected = if (index == 0) prefs.galleryEntryHome else !prefs.galleryEntryHome
                    SegmentedButton(
                        selected = selected,
                        onClick = { onPrefsChange(prefs.copy(galleryEntryHome = index == 0)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(label) }
                    )
                }
            }

            SettingToggleRow("启用日志记录", prefs.loggingEnabled) {
                onPrefsChange(prefs.copy(loggingEnabled = it))
                com.goodsbuy.app.util.AppLogger.setEnabled(it)
            }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val logFile = com.goodsbuy.app.util.AppLogger.getLogFile()
            val crashFile = com.goodsbuy.app.util.AppLogger.getCrashLogFile()
            if (logFile != null || crashFile != null) {
                ListRowItem(
                    icon = Icons.Default.CloudDownload,
                    title = "导出日志",
                    subtitle = "分享 app.log / crash.log",
                    onClick = {
                        val files = listOfNotNull(logFile, crashFile)
                        val uris = ArrayList(files.map {
                            androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
                        })
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "*/*"
                            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(android.content.Intent.createChooser(shareIntent, "分享日志文件"))
                    }
                )
                ListRowItem(
                    icon = Icons.Default.Delete,
                    title = "删除日志",
                    subtitle = "删除后仍会继续记录新日志",
                    onClick = showDeleteLogsDialog,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt
git commit -m "feat(ui): 我的页改设置中心（HeroHeader+ListRowItem 分组）"
```

---

### Task 18: 图鉴页（卡片式分组头 + 胶囊搜索）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt`（只改两处）

- [ ] **Step 1: 搜索框改为 SearchBar**

在 `GalleryScreen` 函数体顶部、`var selectedGroupName` 附近（约 64 行处）添加状态与同步逻辑：

```kotlin
    var searchText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(uiState.searchQuery)) }
    LaunchedEffect(uiState.searchQuery) {
        if (searchText.text != uiState.searchQuery) {
            searchText = androidx.compose.ui.text.input.TextFieldValue(
                uiState.searchQuery,
                selection = androidx.compose.ui.text.TextRange(uiState.searchQuery.length)
            )
        }
    }
```

然后把原 `OutlinedTextField` 整段（约 193-211 行）替换为：

```kotlin
                SearchBar(
                    value = searchText,
                    onValueChange = { searchText = it; viewModel.setSearchQuery(it.text) },
                    placeholder = "搜索 IP/系列…",
                    onClear = { viewModel.setSearchQuery("") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
```

补充 import：`import com.goodsbuy.app.ui.components.SearchBar`。`remember`、`LaunchedEffect` 已通过 `androidx.compose.runtime.*` 通配导入。

- [ ] **Step 2: 分组头卡片化**

把 `GalleryGroupHeader` 的 `Row`（约 308-336 行）整体替换为带圆角底色+封面缩略图的形式：

```kotlin
@Composable
private fun GalleryGroupHeader(
    group: GalleryGroup,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onOpen: () -> Unit
) {
    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(220),
        label = "chevron_rotation"
    )
    val cover = group.collectibles.firstOrNull()?.imagePaths?.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen, role = Role.Button)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (cover != null) {
                androidx.compose.ui.layout.ContentScale.Crop.let { cs ->
                    coil.compose.AsyncImage(
                        model = cover,
                        contentDescription = group.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = cs
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                text = "${group.count} 件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleCollapse) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

补充 import：`androidx.compose.foundation.background`、`androidx.compose.foundation.shape.RoundedCornerShape`、`androidx.compose.ui.draw.clip`（若未存在）。

- [ ] **Step 3: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt
git commit -m "feat(ui): 图鉴页胶囊搜索 + 卡片式分组头"
```

---

### Task 19: 详情页（大图 Hero + 分区卡片）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/detail/CollectibleDetailScreen.kt`（只改两处）

- [ ] **Step 1: 快捷状态卡改 StatusChip 圆点行**

在 `CollectibleDetailScreen.kt` 中，把「快捷状态修改」Card 内的 `FilterChip(...)`（约 122-129 行）替换为：

```kotlin
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderStatus.entries.forEach { status ->
                                StatusChip(
                                    status = status,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { viewModel.updateStatus(status) }
                                        .then(
                                            if (collectible.status == status) {
                                                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }
```

补充 import：`com.goodsbuy.app.ui.components.StatusChip`、`androidx.compose.foundation.clickable`（若未存在）、`androidx.compose.foundation.background`、`androidx.compose.foundation.shape.RoundedCornerShape`、`androidx.compose.ui.draw.clip`。

- [ ] **Step 2: 分区标题用 SectionHeader，卡片圆角跟随主题**

把详情页各 `Card` 内的标题 `Text("基础信息" / "购入信息" / "卖出信息" / "盈亏情况", style = titleMedium, fontWeight = Bold)` 保留（其样式已由 Type.kt 升级为 SemiBold titleMedium），无需改动。仅需确认 `DetailImageGallery` 的「藏品图片」标题也随 Type 升级即可。

- [ ] **Step 3: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/detail/CollectibleDetailScreen.kt
git commit -m "feat(ui): 详情页快捷状态改圆点 StatusChip 行"
```

---

### Task 20: 表单页（分区标题 + 胶囊保存按钮）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt`（5 处定点修改）

现状：保存按钮在 `TopAppBar` 的 `actions`（约 148-160 行，`IconButton(onClick = viewModel::save)`）；分区标题为裸 `Text("藏品图片"/"基础信息"/"卖出信息"/"状态", style = titleMedium)`。

- [ ] **Step 1: 顶部保存按钮改胶囊 Button**

把 148-160 行的 `IconButton(...)` 整段替换为：

```kotlin
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving && !uiState.isSaved,
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                if (uiState.isSaved) Icons.Default.Check else Icons.Default.Save,
                                contentDescription = "保存",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存", style = MaterialTheme.typography.labelLarge)
                        }
                    }
```

补充 import：`androidx.compose.foundation.shape.RoundedCornerShape`（若未存在）、`androidx.compose.foundation.layout.PaddingValues`（`layout.*` 已通配）。`Button`、`ButtonDefaults` 来自 `material3.*` 通配导入。

- [ ] **Step 2: 四个分区标题改用 SectionHeader**

分别替换：

```kotlin
// 原：Text("藏品图片", style = MaterialTheme.typography.titleMedium)
SectionHeader(title = "藏品图片")

// 原：Text("基础信息", style = MaterialTheme.typography.titleMedium)
SectionHeader(title = "基础信息")

// 原：Text("卖出信息", style = MaterialTheme.typography.titleMedium)
SectionHeader(title = "卖出信息")

// 原：Text("状态", style = MaterialTheme.typography.titleMedium)
SectionHeader(title = "状态")
```

补充 import：`import com.goodsbuy.app.ui.components.SectionHeader`。

- [ ] **Step 3: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt
git commit -m "feat(ui): 表单页分区标题 + 胶囊保存按钮"
```

---

### Task 21: 收尾屏幕跟随主题

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/backup/ImportPreviewScreen.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/form/EdgeFadeEditScreen.kt`

- [ ] **Step 1: 确认两屏无硬编码色块**

Run: `rg -n "Color\\(0x" app/src/main/java/com/goodsbuy/app/ui/backup/ImportPreviewScreen.kt app/src/main/java/com/goodsbuy/app/ui/collectible/form/EdgeFadeEditScreen.kt`

预期：无 `Color(0x...)` 硬编码（若有，替换为主题色 token）。两屏为功能型页面，新主题会自动套用，无需结构性改动。

- [ ] **Step 2: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/backup/ImportPreviewScreen.kt app/src/main/java/com/goodsbuy/app/ui/collectible/form/EdgeFadeEditScreen.kt
git commit -m "chore(ui): 导入预览/渐隐编辑跟随新主题"
```

---

## Phase D：收尾

### Task 22: 主题切换平滑过渡 + 偏好预留

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/preferences/PreferencesRepository.kt`

- [ ] **Step 1: GoodsBuyTheme 内配色平滑过渡**

替换 `Theme.kt` 的 `GoodsBuyTheme` 为（使用 `animateColorAsState` 过渡 key 配色，主题切换时平滑变化）：

```kotlin
package com.goodsbuy.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue

@Composable
fun GoodsBuyTheme(
    theme: ThemeConfig = DreamyPurpleTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val target = if (darkTheme) theme.darkColors else theme.lightColors
    val background by animateColorAsState(target.background, label = "theme_background")
    val surface by animateColorAsState(target.surface, label = "theme_surface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, label = "theme_surface_variant")
    val primary by animateColorAsState(target.primary, label = "theme_primary")
    val onSurface by animateColorAsState(target.onSurface, label = "theme_on_surface")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, label = "theme_on_surface_variant")

    val animated = target.copy(
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        primary = primary,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = animated,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

- [ ] **Step 2: PreferencesRepository 预留主题字段**

在 `GridPreferences` 增加字段并在保存/读取中落库（本次不暴露 UI）：

`GridPreferences` 加：
```kotlin
    val themeId: String = "dreamy_purple"
```

`_state` 初始化加：
```kotlin
            themeId = prefs.getString(PREF_THEME_ID, "dreamy_purple") ?: "dreamy_purple",
```

`save()` 加：
```kotlin
            putString(PREF_THEME_ID, normalizedPrefs.themeId)
```

companion object 加：
```kotlin
        private const val PREF_THEME_ID = "theme_id"
```

- [ ] **Step 3: 构建 + 全量测试**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

Run: `.\gradlew.bat testDebugUnitTest`
Expected: 全部 PASS（含新增 ThemeRegistryTest 与既有 9 个测试）

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/theme/Theme.kt app/src/main/java/com/goodsbuy/app/ui/preferences/PreferencesRepository.kt
git commit -m "feat(theme): 主题切换平滑过渡 + 主题偏好字段预留"
```

---

### Task 23: 最终验证与收尾

**Files:**
- 无代码改动

- [ ] **Step 1: 全量构建 + 测试**

Run: `.\gradlew.bat clean assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL + 全部测试 PASS

- [ ] **Step 2: 检查规格覆盖**

对照 `docs/superpowers/specs/2026-08-20-ui-redesign-design.md` 逐项确认：
- 多主题架构（Task 1/22）✓
- 梦幻粉紫 + 暗夜紫配色（Task 1）✓
- Typography/Shapes（Task 2/3）✓
- 组件库全部 8 个新组件 + 3 个改造（Task 6-14）✓
- 六个屏幕（Task 15-21）✓
- 动效（骨架屏 Task 11、主题过渡 Task 22、数字/卡片动画沿用）✓

- [ ] **Step 3: 提交（若有遗漏）**

```bash
git add -A
git commit -m "chore: UI 重构收尾"
```

---

## 验收清单

- [ ] `.\gradlew.bat assembleDebug` 成功
- [ ] `.\gradlew.bat testDebugUnitTest` 全部通过
- [ ] 浅色 + 深色两套主题下，六个屏幕全部可达、无文字对比度问题
- [ ] 无新增业务功能、无数据层改动、无备份格式改动、无新依赖
- [ ] 底部导航 3 个 Tab 结构未变