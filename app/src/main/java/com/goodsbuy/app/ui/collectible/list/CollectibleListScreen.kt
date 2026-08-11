package com.goodsbuy.app.ui.collectible.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.components.CollectibleCard
import com.goodsbuy.app.ui.components.EmptyState
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.preferences.GridPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: () -> Unit,
    preferencesRepository: PreferencesRepository,
    viewModel: CollectibleListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs by preferencesRepository.collectGridPreferences()

    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Default.Add, contentDescription = "添加藏品")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("搜索藏品、IP、角色...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.Search, contentDescription = "筛选")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Filter chips (collapsible)
            if (showFilters) {
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

            // Grid gallery
            if (uiState.collectibles.isEmpty() && !uiState.isLoading) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(prefs.columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.collectibles, key = { it.id }) { collectible ->
                        CollectibleCard(
                            collectible = collectible,
                            onClick = { onNavigateToDetail(collectible.id) },
                            cardSize = prefs.cardSize.dp
                        )
                    }
                }
            }
        }
    }
}
