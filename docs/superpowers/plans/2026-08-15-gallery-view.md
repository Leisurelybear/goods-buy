# 图鉴模式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增「图鉴模式」独立页面：按 IP / 系列对藏品分组，组头展示名称与件数统计；入口可在「首页 / 我的」之间切换（默认我的）。

**Architecture:** 新增 `GalleryViewModel` 用全量藏品 Flow（现有 `getAllCollectibles()`）按维度分组，分组为纯函数便于单测；新增独立路由 `gallery`，由「我的」页或「首页」藏品柜页跳入。零数据库改动。

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Room (只读复用), Hilt, kotlinx-datetime, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-15-gallery-view-design.md`

---

## 任务总览

| Task | 内容 | 文件 |
|------|------|------|
| 1 | 分组纯函数 + 类型 + 单元测试（TDD） | 新建 `GalleryUiState.kt`、`GalleryGrouping.kt`、`GalleryGroupingTest.kt` |
| 2 | GalleryViewModel | 新建 `GalleryViewModel.kt` |
| 3 | LongPressMenu 支持隐藏批量按钮 | 修改 `LongPressMenu.kt` |
| 4 | GalleryScreen 界面 | 新建 `GalleryScreen.kt` |
| 5 | 路由注册 | 修改 `Screen.kt`、`NavGraph.kt` |
| 6 | 入口偏好 galleryEntryHome | 修改 `PreferencesRepository.kt` |
| 7 | 首页入口 | 修改 `CollectibleListScreen.kt` |
| 8 | 我的入口 + 设置切换 | 修改 `ProfileScreen.kt` |
| 9 | 全量构建 + 单测验证 | — |

**编译/测试命令（Windows PowerShell）：**
```powershell
$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"
& .\gradlew.bat testDebugUnitTest --tests "com.goodsbuy.app.ui.gallery.GalleryGroupingTest" *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"
```
- 单测结果读 `build.log`，`EXITCODE=0` 且 `BUILD SUCCESSFUL` 即通过。
- 全程使用 `.*> build.log` 重定向（避免 Gradle daemon 挂住管道）；不要给非 test 任务传 `--tests`。

---

### Task 1: 分组纯函数 + 类型 + 单元测试（TDD）

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryUiState.kt`
- Create: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryGrouping.kt`
- Test: `app/src/test/java/com/goodsbuy/app/ui/gallery/GalleryGroupingTest.kt`

- [ ] **Step 1: 先写类型定义 `GalleryUiState.kt`**

```kotlin
package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible

enum class GroupBy { IP, SERIES }

data class GalleryGroup(
    val name: String,
    val count: Int,
    val collectibles: List<Collectible>
)

data class GalleryUiState(
    val groupBy: GroupBy = GroupBy.IP,
    val groups: List<GalleryGroup> = emptyList(),
    val isLoading: Boolean = true
)
```

- [ ] **Step 2: 写失败测试 `GalleryGroupingTest.kt`**

```kotlin
package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryGroupingTest {

    private fun collectible(
        id: Long,
        ipName: String = "",
        seriesName: String = "",
        createdAt: Long = id
    ) = Collectible(
        id = id, name = "藏品$id", category = "", type = "",
        ipName = ipName, seriesName = seriesName, characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = 0.0, purchaseQuantity = 1, purchaseShipping = 0.0,
        expectedPrice = 0.0, sellPrice = null, sellQuantity = null, sellShipping = null,
        isFreeShipping = false, sellDate = null, buyerInfo = null, sellRemark = null,
        status = OrderStatus.OWNED, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = createdAt, updatedAt = createdAt
    )

    @Test
    fun `groups by IP with correct counts`() {
        val list = listOf(
            collectible(1, ipName = "原神"),
            collectible(2, ipName = "原神"),
            collectible(3, ipName = "蔚蓝档案"),
            collectible(4, ipName = "")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("原神", "蔚蓝档案", "未分类"), groups.map { it.name })
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
    }

    @Test
    fun `groups by series with correct counts`() {
        val list = listOf(
            collectible(1, seriesName = "阿里乌斯"),
            collectible(2, seriesName = "阿里乌斯"),
            collectible(3, seriesName = "千年"),
            collectible(4, seriesName = "")
        )
        val groups = groupCollectibles(list, GroupBy.SERIES)
        assertEquals(listOf("阿里乌斯", "千年", "未分类"), groups.map { it.name })
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
    }

    @Test
    fun `orders groups by count descending and uncategorized last`() {
        val list = listOf(
            collectible(1, ipName = "A"), collectible(2, ipName = "A"), collectible(3, ipName = "A"),
            collectible(4, ipName = "B"),
            collectible(5, ipName = ""), collectible(6, ipName = "  ")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("A", "B", "未分类"), groups.map { it.name })
        assertEquals(listOf(3, 1, 2), groups.map { it.count })
    }

    @Test
    fun `returns empty groups for empty list`() {
        assertEquals(emptyList<GalleryGroup>(), groupCollectibles(emptyList(), GroupBy.IP))
        assertEquals(emptyList<GalleryGroup>(), groupCollectibles(emptyList(), GroupBy.SERIES))
    }

    @Test
    fun `blank keys are treated as uncategorized`() {
        val list = listOf(
            collectible(1, ipName = " "),
            collectible(2, ipName = "原神")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("原神", "未分类"), groups.map { it.name })
        assertEquals(2, groups[1].count)
    }

    @Test
    fun `items within group sorted by series then createdAt desc`() {
        val list = listOf(
            collectible(1, ipName = "IP1", seriesName = "S2", createdAt = 100),
            collectible(2, ipName = "IP1", seriesName = "S1", createdAt = 200),
            collectible(3, ipName = "IP1", seriesName = "S1", createdAt = 300)
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf(3L, 2L, 1L), groups.single().collectibles.map { it.id })
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: 编译命令（见上，`--tests "com.goodsbuy.app.ui.gallery.GalleryGroupingTest"`）
Expected: `EXITCODE=1`，`build.log` 中有编译错误 `unresolved reference: groupCollectibles`（函数还不存在）。

- [ ] **Step 4: 实现 `GalleryGrouping.kt`**

```kotlin
package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible

const val UNCATEGORIZED_NAME = "未分类"

private val GROUP_ITEM_COMPARATOR =
    compareBy<Collectible> { it.seriesName }.thenByDescending { it.createdAt }

fun groupCollectibles(collectibles: List<Collectible>, groupBy: GroupBy): List<GalleryGroup> {
    val buckets = LinkedHashMap<String, MutableList<Collectible>>()
    collectibles.forEach { c ->
        val raw = when (groupBy) {
            GroupBy.IP -> c.ipName
            GroupBy.SERIES -> c.seriesName
        }
        val key = raw.trim().ifBlank { UNCATEGORIZED_NAME }
        buckets.getOrPut(key) { mutableListOf() }.add(c)
    }
    val uncategorized = buckets.remove(UNCATEGORIZED_NAME)
    val grouped = buckets.entries
        .sortedByDescending { it.value.size }
        .map { (name, items) ->
            GalleryGroup(name, items.size, items.sortedWith(GROUP_ITEM_COMPARATOR))
        }
    return if (uncategorized != null) {
        grouped + GalleryGroup(
            UNCATEGORIZED_NAME,
            uncategorized.size,
            uncategorized.sortedWith(GROUP_ITEM_COMPARATOR)
        )
    } else {
        grouped
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: 编译命令（同上）
Expected: `EXITCODE=0`，`build.log` 末尾 `BUILD SUCCESSFUL`，6 个测试全过。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryUiState.kt app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryGrouping.kt app/src/test/java/com/goodsbuy/app/ui/gallery/GalleryGroupingTest.kt
git commit -m "feat: 图鉴模式分组纯函数与单元测试"
```

---

### Task 2: GalleryViewModel

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryViewModel.kt`

- [ ] **Step 1: 创建 `GalleryViewModel.kt`**

```kotlin
package com.goodsbuy.app.ui.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.util.CollectibleNameUtils
import com.goodsbuy.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _groupBy = MutableStateFlow(GroupBy.IP)

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.getAllCollectibles(),
        _groupBy
    ) { list, groupBy ->
        GalleryUiState(
            groupBy = groupBy,
            groups = groupCollectibles(list, groupBy),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState())

    fun setGroupBy(groupBy: GroupBy) {
        _groupBy.value = groupBy
    }

    fun quickUpdateStatus(collectible: Collectible, newStatus: OrderStatus) {
        viewModelScope.launch {
            val updated = collectible.copy(
                status = newStatus,
                sellDate = if (newStatus == OrderStatus.SOLD) System.currentTimeMillis() else collectible.sellDate
            )
            repository.updateCollectible(updated)
        }
    }

    fun duplicateCollectible(collectible: Collectible) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val ts = "%04d-%02d-%02d %02d:%02d".format(
                now.date.year, now.date.monthNumber, now.date.dayOfMonth, now.hour, now.minute
            )
            val newImages = collectible.imagePaths.mapNotNull { path ->
                val src = File(path)
                if (!src.exists()) return@mapNotNull null
                val parentDir = src.parentFile ?: return@mapNotNull null
                val dest = File(parentDir, "${System.currentTimeMillis()}_${src.name}")
                try {
                    src.copyTo(dest, overwrite = false)
                    dest.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            val dup = collectible.copy(
                id = 0L,
                name = CollectibleNameUtils.buildDuplicateName(collectible.name, ts),
                imagePaths = newImages,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertCollectible(dup)
        }
    }

    fun deleteCollectible(collectible: Collectible) {
        viewModelScope.launch {
            collectible.imagePaths.forEach { ImageUtils.deleteImage(context, it) }
            repository.deleteCollectible(collectible.id)
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`，`BUILD SUCCESSFUL`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryViewModel.kt
git commit -m "feat: 图鉴模式 ViewModel"
```

---

### Task 3: LongPressMenu 支持隐藏批量按钮

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/LongPressMenu.kt`

- [ ] **Step 1: 新增 `showBatchSelect` 参数**

将签名改为：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenu(
    state: LongPressMenuState,
    onDismiss: () -> Unit,
    onQuickStatus: (OrderStatus) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onBatchSelect: () -> Unit,
    showBatchSelect: Boolean = true
) {
```

把底部的「批量选择」区块（`Spacer`/`HorizontalDivider`/`MenuRow(...批量选择...)` 那组，即第 64-71 行）包进 `if (showBatchSelect)`：

```kotlin
            if (showBatchSelect) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                MenuRow(
                    icon = Icons.Default.SelectAll,
                    label = "批量选择",
                    onClick = { onBatchSelect() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
```

其余内容不动。

- [ ] **Step 2: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/LongPressMenu.kt
git commit -m "feat: LongPressMenu 支持隐藏批量选择按钮"
```

---

### Task 4: GalleryScreen 界面

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt`

- [ ] **Step 1: 创建 `GalleryScreen.kt`**

```kotlin
package com.goodsbuy.app.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long) -> Unit,
    preferencesRepository: PreferencesRepository,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }
    var menuState by remember { mutableStateOf<LongPressMenuState?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Collectible?>(null) }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    if (menuState != null) {
        LongPressMenu(
            state = menuState!!,
            onDismiss = { menuState = null },
            onQuickStatus = { status -> viewModel.quickUpdateStatus(menuState!!.collectible, status) },
            onEdit = { onNavigateToForm(menuState!!.collectible.id) },
            onDuplicate = { viewModel.duplicateCollectible(menuState!!.collectible) },
            onDelete = {
                pendingDelete = menuState?.collectible
                showDeleteConfirm = true
            },
            onBatchSelect = {},
            showBatchSelect = false
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; pendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${pendingDelete?.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { viewModel.deleteCollectible(it) }
                    pendingDelete = null
                    showDeleteConfirm = false
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; pendingDelete = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图鉴模式") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.groupBy == GroupBy.IP,
                    onClick = { viewModel.setGroupBy(GroupBy.IP) },
                    label = { Text("按 IP") }
                )
                FilterChip(
                    selected = uiState.groupBy == GroupBy.SERIES,
                    onClick = { viewModel.setGroupBy(GroupBy.SERIES) },
                    label = { Text("按系列") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.groups.isEmpty() && !uiState.isLoading) {
                EmptyState(message = "还没有藏品")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(prefs.columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.groups.forEach { group ->
                        item(key = "header_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                            GalleryGroupHeader(group)
                        }
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
}

@Composable
private fun GalleryGroupHeader(group: GalleryGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${group.count} 件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/gallery/GalleryScreen.kt
git commit -m "feat: 图鉴模式页面"
```

---

### Task 5: 路由注册

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/navigation/NavGraph.kt`

- [ ] **Step 1: `Screen.kt` 新增路由**

在 `data object Profile : Screen("profile")` 后加：

```kotlin
    data object Gallery : Screen("gallery")
```

- [ ] **Step 2: `NavGraph.kt` 注册**

在 `composable(Screen.Profile.route)` 块之后新增：

```kotlin
        composable(Screen.Gallery.route) {
            val context = LocalContext.current
            val preferencesRepository = remember { PreferencesRepository(context) }
            GalleryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) },
                preferencesRepository = preferencesRepository
            )
        }
```

同时给 `CollectibleListScreen` 的调用加 `onNavigateToGallery`：

```kotlin
            CollectibleListScreen(
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                preferencesRepository = preferencesRepository
            )
```

`NavGraph.kt` 顶部加 import：

```kotlin
import com.goodsbuy.app.ui.gallery.GalleryScreen
```

- [ ] **Step 3: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。
注：`CollectibleListScreen` 的 `onNavigateToGallery` 参数要等 Task 7 才加上，若此处编译失败，可先给 `CollectibleListScreen` 签名加 `onNavigateToGallery: () -> Unit = {}` 占位（Task 7 会补全 UI）。**推荐直接先加占位参数。**

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/navigation/Screen.kt app/src/main/java/com/goodsbuy/app/ui/navigation/NavGraph.kt app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt
git commit -m "feat: 注册图鉴模式路由"
```

---

### Task 6: 入口偏好 galleryEntryHome

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/preferences/PreferencesRepository.kt`

- [ ] **Step 1: 修改 `PreferencesRepository.kt`**

`GridPreferences` 末尾加字段：

```kotlin
data class GridPreferences(
    val columns: Int = 2,
    val cardSize: Int = 140,
    val showName: Boolean = true,
    val showPrice: Boolean = true,
    val showStatus: Boolean = true,
    val sortField: String = "CREATED_AT",
    val sortAscending: Boolean = false,
    val fontSize: Int = 1,           // 0=小 1=中 2=大
    val showSortControl: Boolean = true,
    val loggingEnabled: Boolean = false,
    val galleryEntryHome: Boolean = false
)
```

`_state` 初始化末尾加 `prefs.getBoolean(PREF_GALLERY_ENTRY_HOME, false)`：

```kotlin
    private val _state = mutableStateOf(
        GridPreferences(
            columns, cardSize, showName, showPrice, showStatus,
            prefs.getString(PREF_SORT_FIELD, "CREATED_AT") ?: "CREATED_AT",
            prefs.getBoolean(PREF_SORT_ASCENDING, false),
            prefs.getInt(PREF_FONT_SIZE, 1),
            prefs.getBoolean(PREF_SHOW_SORT, true),
            prefs.getBoolean(PREF_LOGGING, false),
            prefs.getBoolean(PREF_GALLERY_ENTRY_HOME, false)
        )
    )
```

新增 getter（在 `loggingEnabled` getter 后）：

```kotlin
    val galleryEntryHome: Boolean get() = prefs.getBoolean(PREF_GALLERY_ENTRY_HOME, false)
```

`save` 里加持久化（在 `putBoolean(PREF_LOGGING, ...)` 后）：

```kotlin
            putBoolean(PREF_GALLERY_ENTRY_HOME, prefs.galleryEntryHome)
```

companion 里加常量：

```kotlin
        private const val PREF_GALLERY_ENTRY_HOME = "gallery_entry_home"
```

- [ ] **Step 2: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/preferences/PreferencesRepository.kt
git commit -m "feat: 图鉴入口位置偏好"
```

---

### Task 7: 首页入口（藏品柜）

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt`

- [ ] **Step 1: 加参数**

签名改为：

```kotlin
fun CollectibleListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    onNavigateToGallery: () -> Unit = {},
    preferencesRepository: PreferencesRepository,
    viewModel: CollectibleListViewModel = hiltViewModel()
)
```

- [ ] **Step 2: 搜索栏上方加入口 chips**

在 `Column(...) {` 内、`OutlinedTextField`（搜索框）之前插入：

```kotlin
            if (prefs.galleryEntryHome) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("网格") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onNavigateToGallery,
                        label = { Text("图鉴") }
                    )
                }
            }
```

（`prefs` 变量已存在于该 Composable 中，无需新状态。）

- [ ] **Step 3: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt
git commit -m "feat: 首页图鉴入口（偏好开启时显示）"
```

---

### Task 8: 我的入口 + 设置切换

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: 加参数**

签名改为：

```kotlin
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
)
```

- [ ] **Step 2: import 图标**

```kotlin
import androidx.compose.material.icons.filled.PhotoLibrary
```

- [ ] **Step 3: 主卡片加「图鉴模式」行**

在「导入备份」行的 `HorizontalDivider()` 之后、「关于谷的拜」行之前插入：

```kotlin
                        if (!prefs.galleryEntryHome) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToGallery() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("图鉴模式", style = MaterialTheme.typography.bodyLarge)
                                    Text("按 IP/系列分类查看", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                        }
```

- [ ] **Step 4: 设置卡片加「图鉴入口位置」**

在「启用日志记录」的 `HorizontalDivider()` 之后（日志区块之前）插入：

```kotlin
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("图鉴入口位置", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.weight(1f))
                            FilterChip(
                                selected = !prefs.galleryEntryHome,
                                onClick = { prefs = prefs.copy(galleryEntryHome = false); preferencesRepository.save(prefs) },
                                label = { Text("我的") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = prefs.galleryEntryHome,
                                onClick = { prefs = prefs.copy(galleryEntryHome = true); preferencesRepository.save(prefs) },
                                label = { Text("首页") }
                            )
                        }
```

- [ ] **Step 5: 编译验证**

Run: `& .\gradlew.bat compileDebugKotlin *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`。
注：`ProfileScreen` 的调用在 `NavGraph.kt`（`NavGraph` 中目前只传 `preferencesRepository` 和 `onNavigateBack`），`onNavigateToGallery` 有默认值，无需改 NavGraph。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt
git commit -m "feat: 我的页图鉴入口与入口位置设置"
```

---

### Task 9: 全量构建 + 单测验证

- [ ] **Step 1: 运行全部单元测试**

Run: `& .\gradlew.bat testDebugUnitTest *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`，`BUILD SUCCESSFUL`（含已有 `CollectibleNameUtilsTest` 与新增 `GalleryGroupingTest`）。

- [ ] **Step 2: 构建 Debug APK**

Run: `& .\gradlew.bat assembleDebug *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"`（先设 JAVA_HOME）
Expected: `EXITCODE=0`，产物 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 3: 自查功能清单**

- [ ] 「我的 → 图鉴模式」进入独立页面
- [ ] 顶部「按 IP / 按系列」可切换
- [ ] 组头显示名称 + 件数，未分类在最后
- [ ] 点卡片进详情；长按弹菜单（无「批量选择」）
- [ ] 「我的 → 设置 → 图鉴入口位置」可切首页/我的；切首页后藏品柜顶部出现「网格/图鉴」chips，且我的页隐藏图鉴行

- [ ] **Step 4: 提交收尾（如需）**

```bash
git status
git add -A
git commit -m "chore: 图鉴模式功能收尾"
```

---

## 自检记录（写作时执行）

- **Spec 覆盖**：入口偏好（T6-8）、独立路由（T5）、分组+统计（T1）、维度切换（T1/T2）、未分类（T1）、组排序（T1）、卡片点按/长按（T4）、LongPressMenu 隐藏批量（T3）、单测（T1）、偏好持久化（T6）。Spec 全部要求均有对应任务。
- **占位扫描**：无 TBD/TODO；所有代码步骤含完整代码。
- **类型一致性**：`GroupBy`/`GalleryGroup`/`GalleryUiState`/`groupCollectibles`/`UNCATEGORIZED_NAME`/`GalleryViewModel`/`GalleryScreen`/`onNavigateToGallery` 在全文命名一致；`showBatchSelect` 仅在 T3 引入并被 T4 使用。
