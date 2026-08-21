# 设置页信息架构重组实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将「我的 → 显示设置」重组为单页五组（外观/首页行为/编辑与草稿/图鉴/日志），统一 ListRowItem 行控件，补主题选择器占位。

**Architecture:** 纯 UI 层改动，仅重写 `ProfileScreen.kt` 的设置分支；新增三个私有 Composable（`SettingsGroup`/`SettingSwitchRow`/`StepperRow`）复用现有 `SectionHeader`/`ListRowItem`。数据流不变（`prefs` 本地 state + `preferencesRepository.save()`）。无新领域逻辑，项目无 Compose UI 测试设施，故以构建 + 手动验收代替 TDD。

**Tech Stack:** Kotlin + Jetpack Compose M3（bom 2024.02.00）。构建 `.\gradlew.bat assembleDebug`，单测 `.\gradlew.bat testDebugUnitTest`。

**规格依据：** `docs/superpowers/specs/2026-08-21-settings-ia-design.md`

---

## 文件结构总览

- 修改：`app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`（唯一改动文件）
  - 主页分支：设置行箭头改 ChevronRight、关于行去 onClick、设置副标题更新
  - 设置分支：整体替换为五组结构
  - 删除底部 `SettingToggleRow`；新增 `SettingsGroup`/`SettingSwitchRow`/`StepperRow`/`SettingsContent` 四个私有 Composable
- 复用不改：`ui/components/ListRowItem.kt`（签名 `title/subtitle/trailing/leading/onClick`）、`ui/components/SectionHeader.kt`、`ui/theme/ThemeConfig.kt` 的 `AppThemes`

---

### Task 1: 主页入口行修复

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: 替换 import 区**

删除以下不再使用的 import（逐行删）：

```kotlin
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.graphicsLayer
```

新增：

```kotlin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import com.goodsbuy.app.ui.preferences.GridPreferences
import com.goodsbuy.app.ui.components.SectionHeader
import com.goodsbuy.app.ui.theme.AppThemes
import com.goodsbuy.app.util.AppLogger
import android.content.Intent
import androidx.core.content.FileProvider
```

注意：`androidx.compose.ui.platform.LocalContext` 已在 L25 导入，**不要重复添加**；
Task 2 中把 L287 的全限定 `androidx.compose.ui.platform.LocalContext.current` 改为短名 `LocalContext.current` 即可。
`Icons.Default.Close` 与 `ArrowBack` 保留（草稿删除钮/返回键仍在用）。

- [ ] **Step 2: 修「设置」行箭头与副标题**

将主页分支中（约 L359-371）：

```kotlin
                        ListRowItem(
                            title = "设置",
                            subtitle = "外观、数据、日志等",
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.graphicsLayer(rotationZ = 180f)
                                )
                            },
                            onClick = { showSettings = true }
                        )
```

改为：

```kotlin
                        ListRowItem(
                            title = "设置",
                            subtitle = "外观 · 首页 · 图鉴 · 日志",
                            trailing = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { showSettings = true }
                        )
```

- [ ] **Step 3: 「关于谷的拜」改为纯展示**

将（约 L402-406）：

```kotlin
                        ListRowItem(
                            title = "关于谷的拜",
                            subtitle = "v${BuildConfig.VERSION_NAME}",
                            onClick = {}
                        )
```

改为（去掉 onClick，ListRowItem 不渲染点击态）：

```kotlin
                        ListRowItem(
                            title = "关于谷的拜",
                            subtitle = "v${BuildConfig.VERSION_NAME}"
                        )
```

- [ ] **Step 4: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL（若报 Unresolved `ChevronRight` 等，检查 Step 1 import 是否已加）

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt
git commit -m "refactor(profile): 设置行箭头改 ChevronRight + 关于行去空 onClick"
```

---

### Task 2: 设置页五组重写

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`

前置：Task 1 已合入（import 已就绪）。

- [ ] **Step 1: TopAppBar 标题改名**

将 L109：

```kotlin
                title = { Text(if (showSettings) "显示设置" else if (showDrafts) "草稿箱" else "我的") },
```

改为：

```kotlin
                title = { Text(if (showSettings) "设置" else if (showDrafts) "草稿箱" else "我的") },
```

- [ ] **Step 2: 整体替换设置分支**

将 `if (showSettings) { ... }` 整块（原 L124-326，从 `// Settings screen` 注释到该分支收尾的 `}`）替换为：

```kotlin
            if (showSettings) {
                SettingsContent(
                    prefs = prefs,
                    onPrefsChange = { prefs = it; preferencesRepository.save(it) },
                    onDeleteLogsRequest = { showDeleteLogsDialog = true }
                )
            } else if (showDrafts) {
```

（即原 `} else if (showDrafts) {` 之前的全部设置内容由 `SettingsContent` 承接；注意保持 else-if 链完整。）

- [ ] **Step 3: 删除 SettingToggleRow 并追加四个私有 Composable**

删除文件末尾的：

```kotlin
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

在文件末尾追加：

```kotlin
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionHeader(title = title)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListRowItem(
        title = label,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    )
}

@Composable
private fun StepperRow(
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    ListRowItem(
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, enabled = enabled && canDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "减少", tint = MaterialTheme.colorScheme.primary)
                }
                Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onIncrease, enabled = enabled && canIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "增加", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

@Composable
private fun SettingsContent(
    prefs: GridPreferences,
    onPrefsChange: (GridPreferences) -> Unit,
    onDeleteLogsRequest: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    SettingsGroup(title = "外观") {
        ListRowItem(
            title = "主题",
            subtitle = "当前：${AppThemes.byId(prefs.themeId).label}",
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { showThemeDialog = true }
        )
        HorizontalDivider()
        StepperRow(
            title = "每行展示数量",
            value = "${prefs.columns}",
            canDecrease = prefs.columns > 1,
            canIncrease = prefs.columns < 4,
            onDecrease = { onPrefsChange(prefs.copy(columns = prefs.columns - 1)) },
            onIncrease = { onPrefsChange(prefs.copy(columns = prefs.columns + 1)) }
        )
        HorizontalDivider()
        StepperRow(
            title = "卡片大小",
            value = "${prefs.cardSize}dp",
            canDecrease = prefs.cardSize > 100,
            canIncrease = prefs.cardSize < 200,
            onDecrease = { onPrefsChange(prefs.copy(cardSize = prefs.cardSize - 20)) },
            onIncrease = { onPrefsChange(prefs.copy(cardSize = prefs.cardSize + 20)) }
        )
        HorizontalDivider()
        ListRowItem(
            title = "字体大小",
            trailing = {
                Row {
                    listOf("小", "中", "大").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = prefs.fontSize == idx,
                            onClick = { onPrefsChange(prefs.copy(fontSize = idx)) },
                            label = { Text(label) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        )
        HorizontalDivider()
        SettingSwitchRow("显示名称", prefs.showName) { onPrefsChange(prefs.copy(showName = it)) }
        HorizontalDivider()
        SettingSwitchRow("显示价格", prefs.showPrice) { onPrefsChange(prefs.copy(showPrice = it)) }
        HorizontalDivider()
        SettingSwitchRow("显示状态", prefs.showStatus) { onPrefsChange(prefs.copy(showStatus = it)) }
    }

    SettingsGroup(title = "首页行为") {
        SettingSwitchRow("显示排序栏", prefs.showSortControl) { onPrefsChange(prefs.copy(showSortControl = it)) }
        HorizontalDivider()
        SettingSwitchRow(
            "多图自动轮询",
            prefs.homeImageAutoRotate,
            subtitle = "开启后，首页当前屏幕中的多图片藏品会自动切换封面"
        ) { onPrefsChange(prefs.copy(homeImageAutoRotate = it)) }
        HorizontalDivider()
        StepperRow(
            title = "轮询间隔",
            subtitle = "每张图片停留的时间",
            value = "${prefs.homeImageRotationIntervalSeconds} 秒",
            canDecrease = prefs.homeImageRotationIntervalSeconds > 1,
            canIncrease = prefs.homeImageRotationIntervalSeconds < 60,
            onDecrease = { onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds - 1)) },
            onIncrease = { onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds + 1)) },
            enabled = prefs.homeImageAutoRotate
        )
    }

    SettingsGroup(title = "编辑与草稿") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("草稿自动保存间隔", style = MaterialTheme.typography.titleMedium)
            Text(
                "停止编辑后保存，推荐 0.5 秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val delayOptions = PreferencesRepository.DRAFT_AUTO_SAVE_DELAY_OPTIONS.map { delayMillis ->
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
    }

    SettingsGroup(title = "图鉴") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("入口位置", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("藏品柜", "我的").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = if (index == 0) prefs.galleryEntryHome else !prefs.galleryEntryHome,
                        onClick = { onPrefsChange(prefs.copy(galleryEntryHome = index == 0)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    val ctx = LocalContext.current
    val logFile = AppLogger.getLogFile()
    val crashFile = AppLogger.getCrashLogFile()
    SettingsGroup(title = "日志") {
        SettingSwitchRow("启用记录", prefs.loggingEnabled, subtitle = "记录运行日志用于排查问题") {
            onPrefsChange(prefs.copy(loggingEnabled = it))
            AppLogger.setEnabled(it)
        }
        if (logFile != null || crashFile != null) {
            HorizontalDivider()
            ListRowItem(
                title = "导出日志",
                subtitle = "分享 app.log / crash.log",
                onClick = {
                    val files = listOfNotNull(logFile, crashFile)
                    val uris = ArrayList(files.map {
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
                    })
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
                }
            )
            HorizontalDivider()
            ListRowItem(
                title = "删除日志",
                subtitle = "删除后仍会继续记录新日志",
                onClick = onDeleteLogsRequest
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    AppThemes.all.forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onPrefsChange(prefs.copy(themeId = theme.id))
                                showThemeDialog = false
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.themeId == theme.id,
                                onClick = {
                                    onPrefsChange(prefs.copy(themeId = theme.id))
                                    showThemeDialog = false
                                }
                            )
                            Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("完成") }
            }
        )
    }
}
```

依赖确认（均已存在）：`ColumnScope` 来自 `androidx.compose.foundation.layout.*`（已导入）；`HorizontalDivider`/`FilterChip`/`SegmentedButton`/`SingleChoiceSegmentedButtonRow`/`AlertDialog`/`RadioButton` 来自 `androidx.compose.material3.*`（已导入）；`PreferencesRepository.DRAFT_AUTO_SAVE_DELAY_OPTIONS` 已存在；`AppLogger.setEnabled/getLogFile/getCrashLogFile` 已存在。

- [ ] **Step 4: 构建验证**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt
git commit -m "feat(profile): 设置页重组为五组卡片 + 主题选择器占位"
```

---

### Task 3: 最终验证

- [ ] **Step 1: 全量单测**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL（58 个测试全过，本改动不新增测试）

- [ ] **Step 2: 手动验收清单（对照规格）**

1. 五组各改一项 → 杀进程重开均持久化
2. 主题对话框可打开/选择/关闭并持久化
3. 关闭「多图自动轮询」→ 轮询间隔行置灰
4. 无日志文件时不显示导出/删除；开启并产生文件后出现
5. 「关于谷的拜」不可点、无箭头；「设置」行箭头为 ChevronRight
6. 步进器边界置灰：每行 1–4、卡片 100–200、轮询 1–60

- [ ] **Step 3: 收尾提交（如有格式微调）**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt
git commit -m "chore(profile): 设置页重组收尾微调"
```

（若无改动则跳过。）
