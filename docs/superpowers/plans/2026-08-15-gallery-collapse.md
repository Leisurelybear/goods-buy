# 图鉴折叠 + 组内横向滑动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化图鉴模式：分组组头可点击折叠/展开（会话内记忆），组内藏品从网格矩阵改为一行横向可滑动。

**Architecture:** 仅改动 `GalleryScreen.kt` 的 UI。主体从 `LazyVerticalGrid` 改为 `LazyColumn`，每个 `GalleryGroup` 作为 section（组头 item + 展开时的 `LazyRow` 组体）。折叠状态用 `remember { mutableStateOf(setOf<String>()) }` 保存「已折叠的组名集合」。ViewModel / 分组逻辑 / 数据层零改动。

**Tech Stack:** Kotlin, Jetpack Compose (Material3), kotlinx-datetime (无关), JUnit 4（无新增测试）。

**Spec:** `docs/superpowers/specs/2026-08-15-gallery-collapse-design.md`

---

**编译/测试命令（Windows PowerShell）：**
```powershell
$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"
& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"
```
- 全程使用 `.*> build.log` 重定向（避免 Gradle daemon 挂住管道）。
- 单测验证：`& .\gradlew.bat testDebugUnitTest *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`，`EXITCODE=0` 且 `BUILD SUCCESSFUL` 即通过。

---

### Task 1: GalleryScreen 改为 LazyColumn + 组内横向滑动 + 组头折叠

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt`

- [ ] **Step 1: 替换 import（增删）**

将以下 import 块替换：

删掉（不再需要 grid 的 span 用法）：
```kotlin
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
```

新增：
```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.clickable
```

保留 `androidx.compose.foundation.layout.*`、`androidx.compose.foundation.lazy.grid.GridCells`（不再使用，可一并删除）。

最终文件顶部 import 区应为：
```kotlin
package com.goodsbuy.app.ui.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.ui.collectible.list.LongPressMenu
import com.goodsbuy.app.ui.collectible.list.LongPressMenuState
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.preferences.PreferencesRepository
```

- [ ] **Step 2: 加折叠状态变量**

在 `var pendingDelete by remember { mutableStateOf<Collectible?>(null) }` 之后加：

```kotlin
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val toggleGroupCollapse: (String) -> Unit = { name ->
        collapsedGroups = if (name in collapsedGroups) collapsedGroups - name
        else collapsedGroups + name
    }
```

- [ ] **Step 3: 替换页面主体（EmptyState 之外的 `LazyVerticalGrid` 块）**

将当前 `LazyVerticalGrid(...) { uiState.groups.forEach { group -> ... } }` 整个块替换为：

```kotlin
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.groups.forEach { group ->
                        item(key = "header_${group.name}") {
                            GalleryGroupHeader(
                                group = group,
                                collapsed = group.name in collapsedGroups,
                                onClick = { toggleGroupCollapse(group.name) }
                            )
                        }
                        if (group.name !in collapsedGroups) {
                            item(key = "items_${group.name}") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(group.collectibles, key = { it.id }) { collectible ->
                                        CollectibleCard(
                                            collectible = collectible,
                                            onClick = { onNavigateToDetail(collectible.id) },
                                            cardSize = prefs.cardSize.dp,
                                            showName = prefs.showName,
                                            showPrice = prefs.showPrice,
                                            showStatus = prefs.showStatus,
                                            fontSize = prefs.fontSize,
                                            onLongPress = { menuState = LongPressMenuState(collectible) },
                                            isSelected = false,
                                            onSelect = null,
                                            batchMode = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
```

- [ ] **Step 4: 更新 `GalleryGroupHeader` 签名与实现**

将当前 `GalleryGroupHeader` 整体替换为：

```kotlin
@Composable
private fun GalleryGroupHeader(
    group: GalleryGroup,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (collapsed) "展开" else "折叠",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${group.count} 件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

注意：`Spacer(width = 8.dp)` 需要 `Modifier.width`，来自 `androidx.compose.foundation.layout.*`，已在 import 区。

- [ ] **Step 5: 编译验证**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`
Expected: `EXITCODE=0`，`BUILD SUCCESSFUL`。

- [ ] **Step 6: 单测 + 构建验证**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat testDebugUnitTest assembleDebug *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`
Expected: `EXITCODE=0`，`BUILD SUCCESSFUL`，全部 15 个单测通过（GalleryGrouping 6 / GalleryViewModel 6 / CollectibleNameUtils 3）。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt
git commit -m "feat: 图鉴分组可折叠，组内改为横向滑动"
```

---

## 自检记录（写作时执行）

- **Spec 覆盖**：组头折叠/展开（T1 S2-S4）、折叠状态会话内记忆（T1 S2，`remember` 局部状态，退出/切维度自动重置）、所有组含未分类可折叠（T1 S3 条件 `group.name !in collapsedGroups` 对全部组生效）、组内横向 LazyRow 沿用 cardSize（T1 S3）、交互不变（T1 S3-S4）、空态保留（T1 S3 外层 `if (uiState.groups.isEmpty() ...) EmptyState` 未动）。全部覆盖。
- **占位扫描**：无 TBD/TODO；所有代码步骤含完整代码。
- **类型一致性**：`collapsedGroups`、`toggleGroupCollapse`、`GalleryGroupHeader(group, collapsed, onClick)` 全文一致；`LazyRow`/`items`/`CollectibleCard` 参数与现有 CollectibleListScreen 用法一致；`Icons.Default.ExpandLess/ExpandMore` 为 Material 图标库标准图标（material-icons-extended 已引入）。
- **UI 语义**：`LazyRow` 内嵌 `LazyColumn` item 为 Compose 标准组合，无嵌套冲突（LazyRow 非 LazyVerticalGrid 同级）。
