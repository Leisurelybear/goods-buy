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

    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val toggleGroupCollapse: (String) -> Unit = { name ->
        collapsedGroups = if (name in collapsedGroups) collapsedGroups - name
        else collapsedGroups + name
    }

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
            }
        }
    }
}

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
