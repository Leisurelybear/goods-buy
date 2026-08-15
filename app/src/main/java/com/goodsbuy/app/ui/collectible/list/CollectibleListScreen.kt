package com.goodsbuy.app.ui.collectible.list

import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.collectible.list.SortField
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long?) -> Unit,
    onNavigateToGallery: () -> Unit = {},
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
    var pendingDelete by remember { mutableStateOf<Collectible?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

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
                    Text("确定要删除选中的 ${uiState.selectedIds.size} 件藏品吗？此操作不可撤销。")
                } else {
                    Text("确定要删除「${pendingDelete?.name}」吗？此操作不可撤销。")
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
                        val allSelected = uiState.selectedIds.size == uiState.collectibles.size && uiState.collectibles.isNotEmpty()
                        TextButton(onClick = {
                            if (allSelected) viewModel.clearSelection()
                            else viewModel.selectAll(uiState.collectibles.map { it.id })
                        }) {
                            Text(if (allSelected) "取消全选" else "全选")
                        }
                        if (uiState.selectedIds.isNotEmpty()) {
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
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
                        Text(
                            if (uiState.sortAscending) "升序" else "降序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                            fontSize = prefs.fontSize,
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
