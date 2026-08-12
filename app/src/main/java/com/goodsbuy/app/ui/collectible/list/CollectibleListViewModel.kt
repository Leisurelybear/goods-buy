package com.goodsbuy.app.ui.collectible.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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

    fun hideLongPressMenu() {
        _longPressMenuState.value = null
    }

    fun enterBatchMode(id: Long) {
        _longPressMenuState.value = null
        _isBatchMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelect(id: Long) {
        _selectedIds.update { currentSet ->
            if (currentSet.contains(id)) currentSet - id else currentSet + id
        }
    }

    fun exitBatchMode() {
        _isBatchMode.value = false
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deleteCollectible(id)
            }
        }
    }

    fun batchDeleteSingle(id: Long) {
        viewModelScope.launch {
            repository.deleteCollectible(id)
        }
    }

    fun duplicateCollectible(collectible: Collectible) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val ts = "${"%04d-%02d-%02d %02d:%02d".format(
                now.date.year,
                now.date.monthNumber,
                now.date.dayOfMonth,
                now.hour,
                now.minute
            )}"
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
