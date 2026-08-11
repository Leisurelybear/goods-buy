package com.graincabinet.app.ui.collectible.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graincabinet.app.domain.repository.CollectibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CollectibleListViewModel @Inject constructor(
    private val repository: CollectibleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CollectibleListUiState> = combine(
        _searchQuery, _statusFilter
    ) { query, status -> Pair(query, status) }
        .flatMapLatest { (query, status) ->
            val flow = if (query.isNotBlank()) repository.searchCollectibles(query)
            else if (status != null) repository.getCollectiblesByStatus(status)
            else repository.getAllCollectibles()
            flow.map { CollectibleListUiState(collectibles = it, isLoading = false, searchQuery = query, selectedStatusFilter = status) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectibleListUiState())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String?) { _statusFilter.value = status }
}
