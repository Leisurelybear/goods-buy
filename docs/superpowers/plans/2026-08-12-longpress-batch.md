# 长按藏品菜单与批量选择 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在藏品列表页为每张卡片增加长按交互：弹出 Bottom Sheet 提供快速修改状态、编辑、复制（名称追加时间戳）、删除；并支持从菜单进入批量选择模式，勾选多张卡片后批量删除。

**Architecture:** MVVM + Compose。长按菜单状态和批量选择状态都在 `CollectibleListViewModel` 中管理。图片通过 `java.io.File.copyTo()` 复制（文件已在内部存储，无需额外 Context）。

**Tech Stack:** Jetpack Compose, Material3, Hilt, Room, kotlinx-datetime

## Global Constraints
- Min SDK 29, target SDK 34
- 使用已存在的 `kotlinx-datetime` 依赖（`build.gradle.kts` 第60行）进行时间戳格式化
- 保留现有点击卡片 → 进入详情的行为（批量模式下禁用）
- 复制藏品时图片文件一并复制，原文件不变
- 时间戳后缀格式：` 2026-08-12 14:30`（yyyy-MM-dd HH:mm）
- 所有 UI 文案使用中文
- 不添加新依赖

---

### Task 1: UiState 新增批量模式字段

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListUiState.kt`

**Interfaces:**
- Consumes: 现有 `CollectibleListUiState`
- Produces: `isBatchMode: Boolean = false`, `selectedIds: Set<Long> = emptySet()`

- [ ] **Step 1: 更新 UiState**

将 `CollectibleListUiState.kt` 改为：

```kotlin
package com.goodsbuy.app.ui.collectible.list

import com.goodsbuy.app.domain.model.Collectible

data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val isBatchMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListUiState.kt
git commit -m "feat: add isBatchMode and selectedIds to CollectibleListUiState"
```

---

### Task 2: ViewModel 增加长按菜单和批量逻辑

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListViewModel.kt`

**Interfaces:**
- Consumes: `repository.deleteCollectible(id)`, `repository.updateCollectible(c)`, `repository.getCollectibleById(id)`, `repository.insertCollectible(c)`
- Produces: `data class LongPressMenuState(val collectible: Collectible)`, methods `showLongPressMenu`, `hideLongPressMenu`, `enterBatchMode`, `toggleSelect`, `exitBatchMode`, `batchDelete`, `duplicateCollectible`, `quickUpdateStatus`

- [ ] **Step 1: 重写 ViewModel**

完整替换 `CollectibleListViewModel.kt`：

```kotlin
package com.goodsbuy.app.ui.collectible.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CollectibleListViewModel @Inject constructor(
    private val repository: CollectibleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<String?>(null)
    private val _isBatchMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _longPressMenuState = MutableStateFlow<LongPressMenuState?>(null)

    val uiState: StateFlow<CollectibleListUiState> = combine(
        _searchQuery, _statusFilter
    ) { query, status -> Pair(query, status) }
        .flatMapLatest { (query, status) ->
            val flow = if (query.isNotBlank()) repository.searchCollectibles(query)
            else if (status != null) repository.getCollectiblesByStatus(status)
            else repository.getAllCollectibles()
            flow.map { collectibles ->
                CollectibleListUiState(
                    collectibles = collectibles,
                    isLoading = false,
                    searchQuery = query,
                    selectedStatusFilter = status,
                    isBatchMode = _isBatchMode.value,
                    selectedIds = _selectedIds.value
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectibleListUiState())

    val longPressMenuState: StateFlow<LongPressMenuState?> = _longPressMenuState

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String?) { _statusFilter.value = status }

    fun showLongPressMenu(collectible: Collectible) {
        _longPressMenuState.value = LongPressMenuState(collectible)
    }
    fun hideLongPressMenu() { _longPressMenuState.value = null }

    fun enterBatchMode(id: Long) {
        _longPressMenuState.value = null
        _isBatchMode.value = true
        _selectedIds.value = setOf(id)
    }
    fun toggleSelect(id: Long) {
        _selectedIds.update { if (it.contains(id)) it - id else it + id }
    }
    fun exitBatchMode() {
        _isBatchMode.value = false
        _selectedIds.value = emptySet()
    }
    fun batchDelete() {
        val ids = _selectedIds.value
        viewModelScope.launch { ids.forEach { repository.deleteCollectible(it) } }
    }
    fun duplicateCollectible(collectible: Collectible) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val ts = "${"%04d-%02d-%02d %02d:%02d".format(now.date.year, now.date.monthNumber, now.date.dayOfMonth, now.hour, now.minute)}"
            val newImages = collectible.imagePaths.mapNotNull { path ->
                val src = File(path)
                if (!src.exists()) return@mapNotNull null
                val dest = File(src.parentFile ?: return@mapNotNull null, "${System.currentTimeMillis()}_${src.name}")
                try { src.copyTo(dest, overwrite = false); dest.absolutePath } catch (e: Exception) { null }
            }
            val dup = collectible.copy(
                id = 0L,
                name = "${collectible.name} $ts",
                imagePaths = newImages,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertCollectible(dup)
        }
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
}

data class LongPressMenuState(val collectible: Collectible)
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListViewModel.kt
git commit -m "feat: add long-press menu and batch mode logic to ViewModel"
```

---

### Task 3: 创建 LongPressMenu 组件

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/LongPressMenu.kt`

**Interfaces:**
- Consumes: `LongPressMenuState`（Task 2 定义）, `OrderStatus`
- Produces: `@Composable fun LongPressMenu(state, onDismiss, onQuickStatus, onEdit, onDuplicate, onDelete, onBatchSelect)`

- [ ] **Step 1: 创建文件**

创建 `LongPressMenu.kt`：

```kotlin
package com.goodsbuy.app.ui.collectible.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenu(
    state: LongPressMenuState,
    onDismiss: () -> Unit,
    onQuickStatus: (OrderStatus) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onBatchSelect: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = state.collectible.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "快速修改状态",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                items(OrderStatus.entries) { status ->
                    FilterChip(
                        selected = state.collectible.status == status,
                        onClick = { onQuickStatus(status); onDismiss() },
                        label = { Text(status.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            // Action buttons
            MenuRow(icon = Icons.Default.Edit, label = "编辑", onClick = { onEdit(); onDismiss() })
            MenuRow(icon = Icons.Default.ContentCopy, label = "复制藏品", onClick = { onDuplicate(); onDismiss() })
            MenuRow(icon = Icons.Default.Delete, label = "删除", onClick = { onDelete(); onDismiss() }, isDanger = true)
            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            MenuRow(
                icon = Icons.Default.CheckboxMultipleMark,
                label = "批量选择",
                onClick = { onBatchSelect() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/LongPressMenu.kt
git commit -m "feat: add LongPressMenu composable with status chips and action buttons"
```

---

### Task 4: 更新 CollectibleCard 支持长按和批量选择

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/components/CollectibleCard.kt`

**Interfaces:**
- Consumes: 现有 `Collectible` 模型
- Produces: 新参数 `onLongPress: (() -> Unit)? = null`, `isSelected: Boolean = false`, `onSelect: (() -> Unit)? = null`, `batchMode: Boolean = false`

- [ ] **Step 1: 更新 CollectibleCard**

完整替换 `CollectibleCard.kt`：

```kotlin
package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectLongPressTouchSlop
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus

@Composable
fun CollectibleCard(
    collectible: Collectible,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardSize: Dp = 140.dp,
    showName: Boolean = true,
    showPrice: Boolean = true,
    showStatus: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    batchMode: Boolean = false
) {
    Card(
        modifier = modifier
            .width(cardSize)
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (onLongPress != null) Modifier.pointerInput(Unit) {
                    detectLongPressTouchSlop(onLongPress = { onLongPress() })
                } else Modifier
            )
            .clickable(enabled = !batchMode || onSelect != null) {
                if (batchMode) onSelect?.invoke() else onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            if (collectible.imagePaths.isNotEmpty()) {
                AsyncImage(
                    model = collectible.imagePaths[0],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
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

            // Status badge — top right
            if (showStatus) {
                val statusColor = Color(collectible.status.colorHex)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(statusColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = collectible.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Selection overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { onSelect?.invoke() },
                        modifier = Modifier.padding(6.dp).size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(52.dp)
                    .background(
                        Color.Black.copy(alpha = 0.55f),
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                style = MaterialTheme.typography.labelMedium,
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
                                text = "¥${collectible.purchasePrice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                    if (collectible.imagePaths.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "+${collectible.imagePaths.size - 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/components/CollectibleCard.kt
git commit -m "feat: add long-press gesture and batch selection overlay to CollectibleCard"
```

---

### Task 5: 整合到 CollectibleListScreen 并更新导航

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/navigation/NavGraph.kt`

**Interfaces:**
- Consumes: `viewModel.longPressMenuState`, `viewModel.isBatchMode`, `viewModel.selectedIds`
- Produces: 完整的长按菜单和批量选择 UI

- [ ] **Step 1: 更新 NavGraph**

将 `onNavigateToForm` 从 `() -> Unit` 改为 `(Long?) -> Unit`：

```kotlin
CollectibleListScreen(
    onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
    onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) },
    preferencesRepository = preferencesRepository
)
```

- [ ] **Step 2: 更新 CollectibleListScreen**

完整替换 `CollectibleListScreen.kt`：

```kotlin
package com.goodsbuy.app.ui.collectible.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    preferencesRepository: PreferencesRepository,
    viewModel: CollectibleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val menuState by viewModel.longPressMenuState.collectAsState()
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    var searchText by remember { mutableStateOf(TextFieldValue(uiState.searchQuery)) }
    LaunchedEffect(uiState.searchQuery) {
        if (searchText.text != uiState.searchQuery) {
            searchText = TextFieldValue(uiState.searchQuery, selection = androidx.compose.ui.text.TextRange(uiState.searchQuery.length))
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (menuState != null) {
        LongPressMenu(
            state = menuState!!,
            onDismiss = { viewModel.hideLongPressMenu() },
            onQuickStatus = { status -> viewModel.quickUpdateStatus(menuState!!.collectible, status) },
            onEdit = { onNavigateToForm(menuState!!.collectible.id) },
            onDuplicate = { viewModel.duplicateCollectible(menuState!!.collectible) },
            onDelete = { showDeleteConfirm = true },
            onBatchSelect = { viewModel.enterBatchMode(menuState!!.collectible.id) }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = {
                if (uiState.isBatchMode) {
                    Text("确定要删除选中的 ${uiState.selectedIds.size} 件藏品吗？此操作不可撤销。")
                } else {
                    Text("确定要删除「${menuState?.collectible?.name}」吗？此操作不可撤销。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (uiState.isBatchMode) viewModel.batchDelete()
                    else menuState?.collectible?.id?.let { viewModel.batchDeleteSingle(it) }
                    showDeleteConfirm = false
                    if (uiState.isBatchMode) viewModel.exitBatchMode()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isBatchMode) {
                TopAppBar(
                    title = { Text("批量操作  (${uiState.selectedIds.size} 项)") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitBatchMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    },
                    actions = {
                        if (uiState.selectedIds.isNotEmpty()) {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            } else {
                // No top bar — existing behavior
            }
        },
        floatingActionButton = {
            if (!uiState.isBatchMode) {
                FloatingActionButton(onClick = { onNavigateToForm(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "添加藏品")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!uiState.isBatchMode) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newValue ->
                        searchText = newValue
                        viewModel.onSearchQueryChange(newValue.text)
                    },
                    placeholder = { Text("搜索藏品、IP、角色...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            }

            if (uiState.collectibles.isEmpty() && !uiState.isLoading) {
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
                            onLongPress = { if (!uiState.isBatchMode) viewModel.showLongPressMenu(collectible) },
                            isSelected = uiState.selectedIds.contains(collectible.id),
                            onSelect = { viewModel.toggleSelect(collectible.id) },
                            batchMode = uiState.isBatchMode
                        )
                    }
                }
            }
        }
    }
}
```

**注意：** 上面代码引用了 `viewModel.batchDeleteSingle(id)` —— 需要在 Task 2 的 ViewModel 中补上这个方法。

- [ ] **Step 1b: 在 ViewModel 补充 batchDeleteSingle**

在 `CollectibleListViewModel.kt` 中追加方法：
```kotlin
fun batchDeleteSingle(id: Long) {
    viewModelScope.launch { repository.deleteCollectible(id) }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/list/CollectibleListScreen.kt
git add app/src/main/java/com/goodsbuy/app/ui/navigation/NavGraph.kt
git commit -m "feat: integrate long-press menu and batch selection into list screen"
```

---

### Task 6: 编译验证

- [ ] **Step 1: Gradle 构建**

运行：`.\gradlew.bat assembleDebug`
预期：BUILD SUCCESSFUL

- [ ] **Step 2: 手动验证清单**
  - [ ] 长按任意卡片 → Bottom Sheet 出现，含状态 chip 和编辑/复制/删除/批量选择按钮
  - [ ] 点击状态 chip → 状态立即变更，Sheet 关闭
  - [ ] 点击"编辑" → 跳转到表单页，字段预填充
  - [ ] 点击"复制藏品" → 新卡片出现在列表顶部，名称带时间戳后缀，图片一致
  - [ ] 点击"删除" → 确认弹窗出现，确认后卡片消失
  - [ ] 点击"批量选择" → 退出 Sheet，顶部变为批量操作栏（显示"批量操作 (1 项)"），卡片右上角出现勾选遮罩
  - [ ] 点击其他卡片 → 选中/取消选中，数量更新
  - [ ] 点击"删除" → 确认弹窗（显示 N 件）→ 确认后全部删除，退出批量模式
  - [ ] 点击"取消" → 退出批量模式，卡片恢复正常

- [ ] **Step 3: 修复任何问题**

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "fix: resolve build/runtime issues from long-press and batch feature"
```
