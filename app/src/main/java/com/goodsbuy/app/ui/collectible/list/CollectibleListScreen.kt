package com.goodsbuy.app.ui.collectible.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.goodsbuy.app.ui.components.GradientCard
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
                        title = { },
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
            HeroHeader(
                title = "谷的拜",
                subtitle = "共 ${uiState.summary.totalCount} 件 · 持有 ${uiState.summary.ownedCount} · 已出 ${uiState.summary.soldCount}"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("藏品总数", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        StatNumber(value = uiState.summary.totalCount.toDouble(), color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                GradientCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("总投入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        StatNumber(value = uiState.summary.totalInvestment, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("总回收", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        StatNumber(value = uiState.summary.totalRevenue, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                GradientCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Text("总收益率", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        StatNumber(value = uiState.summary.totalProfitRate, decimals = 1, suffix = "%", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SearchBar(
                value = searchText,
                onValueChange = { searchText = it; viewModel.onSearchQueryChange(it.text) },
                placeholder = "搜索藏品名称、IP、角色…",
                onClear = { searchText = TextFieldValue(""); viewModel.onSearchQueryChange("") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Status filter chips
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

            // Sort control (can be hidden via settings to save space)
            if (prefs.showSortControl) {
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
}
