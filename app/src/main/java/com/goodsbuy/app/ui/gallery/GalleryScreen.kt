package com.goodsbuy.app.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.ui.collectible.list.LongPressMenu
import com.goodsbuy.app.ui.collectible.list.LongPressMenuState
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: (Long) -> Unit,
    preferencesRepository: PreferencesRepository,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingDeletion by viewModel.pendingDeletion.collectAsState()
    val prefs = preferencesRepository.preferencesState.value
    val snackbarHostState = remember { SnackbarHostState() }
    var menuState by remember { mutableStateOf<LongPressMenuState?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Collectible?>(null) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingDeletion?.token) {
        val pending = pendingDeletion ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "已删除 ${pending.collectibles.size} 件藏品",
            actionLabel = "撤销",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
    }

    var collapsedGroups by remember(uiState.groupBy) { mutableStateOf(setOf<String>()) }

    val selectedGroup = selectedGroupName?.let { name ->
        uiState.groups.firstOrNull { it.name == name }
    }

    LaunchedEffect(uiState.groups, selectedGroupName) {
        if (selectedGroupName != null && selectedGroup == null) {
            selectedGroupName = null
        }
    }

    BackHandler(enabled = selectedGroup != null) {
        selectedGroupName = null
    }

    val toggleGroupCollapse: (String) -> Unit = { name ->
        collapsedGroups = if (name in collapsedGroups) collapsedGroups - name
        else collapsedGroups + name
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
            text = { Text("确定要删除「${pendingDelete?.name}」吗？删除后可在提示出现时撤销。") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = selectedGroup,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "gallery_title"
                    ) { group ->
                        if (group != null) {
                            Column {
                                Text(group.name)
                                Text(
                                    "${group.count} 件",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text("图鉴模式")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedGroup != null) selectedGroupName = null else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedGroup != null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(prefs.columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedGroup.collectibles, key = { it.id }) { collectible ->
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
                            batchMode = false,
                            modifier = Modifier.animateItemPlacement(tween(250))
                        )
                    }
                }
            } else {
                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("搜索 IP/系列…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.searchQuery.isNotEmpty(),
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(150))
                        ) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                // Group by filter
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        collapsedGroups = if (uiState.groups.all { it.name in collapsedGroups }) {
                            emptySet()
                        } else {
                            uiState.groups.map { it.name }.toSet()
                        }
                    }, enabled = uiState.groups.isNotEmpty()) {
                        Text(if (uiState.groups.isNotEmpty() && uiState.groups.all { it.name in collapsedGroups }) "全部展开" else "全部折叠")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.groups.isEmpty() && !uiState.isLoading) {
                    EmptyState(message = if (uiState.searchQuery.isBlank()) "还没有藏品" else "没有匹配的藏品")
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
                                    onToggleCollapse = { toggleGroupCollapse(group.name) },
                                    onOpen = { selectedGroupName = group.name }
                                )
                            }
                            item(key = "items_${group.name}") {
                                AnimatedVisibility(
                                    visible = group.name !in collapsedGroups,
                                    enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                                    exit = shrinkVertically(tween(220)) + fadeOut(tween(220))
                                ) {
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
}

@Composable
private fun GalleryGroupHeader(
    group: GalleryGroup,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onOpen: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(220),
        label = "chevron_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleCollapse) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen, role = Role.Button)
                .padding(vertical = 8.dp),
        ) {
            Text(text = group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "${group.count} 件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.ChevronRight, contentDescription = "查看本组")
        }
    }
}
