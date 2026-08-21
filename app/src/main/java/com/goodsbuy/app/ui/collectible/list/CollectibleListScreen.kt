package com.goodsbuy.app.ui.collectible.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.collectible.list.SortField
import com.goodsbuy.app.ui.components.AppFAB
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.components.SearchBar
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
            searchText = TextFieldValue(uiState.searchQuery, selection = TextRange(uiState.searchQuery.length))
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
                    TopAppBar(
                        title = { Text("谷的拜") },
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
        },
        floatingActionButton = {
            if (!uiState.isBatchMode) {
                AppFAB(onClick = { onNavigateToForm(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "添加藏品", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchBar(
                value = searchText,
                onValueChange = { searchText = it; viewModel.onSearchQueryChange(it.text) },
                placeholder = "搜索藏品名称、IP、角色…",
                onClear = { searchText = TextFieldValue(""); viewModel.onSearchQueryChange("") },
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            )

            // Compact toolbar: sort + status filter in one row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prefs.showSortControl) {
                    item {
                        Box {
                            CompactPill(
                                label = uiState.sortField.label,
                                selected = false,
                                onClick = { sortMenuExpanded = true },
                                leadingIcon = {
                                    Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            )
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
                    }
                    item {
                        val directionDesc = !uiState.sortAscending
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.onSortDirectionToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (directionDesc) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = "切换升序/降序",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                item {
                    CompactPill(
                        label = "全部",
                        selected = uiState.selectedStatusFilter == null,
                        onClick = { viewModel.onStatusFilterChange(null) }
                    )
                }
                items(OrderStatus.entries) { status ->
                    CompactPill(
                        label = status.displayName,
                        selected = uiState.selectedStatusFilter == status.name,
                        onClick = { viewModel.onStatusFilterChange(status.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = uiState.collectibles.isEmpty() && !uiState.isLoading,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "list_or_empty",
                modifier = Modifier.weight(1f)
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
                                modifier = Modifier.fillMaxWidth().animateItemPlacement(tween(250))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 紧凑筛选胶囊：比 FilterChip（32dp）更矮（约 24dp），为藏品网格省出空间。 */
@Composable
private fun CompactPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
