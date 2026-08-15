package com.goodsbuy.app.ui.collectible.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.util.CollectibleNameUtils
import com.goodsbuy.app.util.ImageUtils
import com.goodsbuy.app.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectibleListViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<String?>(null)
    private val _isBatchMode = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _longPressMenuState = MutableStateFlow<LongPressMenuState?>(null)
    private val _sortField = MutableStateFlow(SortField.valueOf(preferencesRepository.sortField))
    private val _sortAscending = MutableStateFlow(preferencesRepository.sortAscending)

    private val collator = Collator.getInstance(Locale.CHINESE)

    val uiState: StateFlow<CollectibleListUiState> = combine(
        combine(_searchQuery, _statusFilter, _sortField, _sortAscending) { q, s, f, a -> Quad(q, s, f, a) }
            .flatMapLatest { (query, status, sortField, asc) ->
                val flow = if (query.isNotBlank()) repository.searchCollectibles(query)
                else if (status != null) repository.getCollectiblesByStatus(status)
                else repository.getAllCollectibles()
                flow.map { list -> sortList(list, sortField, asc) }
                    .map { list ->
                        CollectibleListUiState(
                            collectibles = list,
                            isLoading = false,
                            searchQuery = query,
                            selectedStatusFilter = status,
                            sortField = sortField,
                            sortAscending = asc
                        )
                    }
            },
        _isBatchMode,
        _selectedIds
    ) { base, batchMode, selectedIds ->
        base.copy(isBatchMode = batchMode, selectedIds = selectedIds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectibleListUiState())

    val longPressMenuState: StateFlow<LongPressMenuState?> = _longPressMenuState

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onStatusFilterChange(status: String?) { _statusFilter.value = status }

    fun onSortFieldChange(field: SortField) {
        _sortField.value = field
        preferencesRepository.save(preferencesRepository.preferencesState.value.copy(sortField = field.name))
        AppLogger.i("Sort", "Field changed to $field")
    }

    fun onSortDirectionToggle() {
        val next = !_sortAscending.value
        _sortAscending.value = next
        preferencesRepository.save(preferencesRepository.preferencesState.value.copy(sortAscending = next))
        AppLogger.i("Sort", "Direction toggled to ${if (next) "asc" else "desc"}")
    }

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

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun exitBatchMode() {
        _isBatchMode.value = false
        _selectedIds.value = emptySet()
    }

    fun batchDelete() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                repository.getCollectibleById(id)?.let { c ->
                    c.imagePaths.forEach { ImageUtils.deleteImage(context, it) }
                }
                repository.deleteCollectible(id)
            }
            AppLogger.i("Delete", "Batch delete: count=${ids.size}, ids=$ids")
        }
    }

    fun batchDeleteSingle(id: Long) {
        viewModelScope.launch {
            repository.getCollectibleById(id)?.let { c ->
                c.imagePaths.forEach { ImageUtils.deleteImage(context, it) }
            }
            repository.deleteCollectible(id)
            AppLogger.i("Delete", "Single delete: id=$id")
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
                name = CollectibleNameUtils.buildDuplicateName(collectible.name, ts),
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
            AppLogger.i("Status", "Quick update: id=${collectible.id}, name=${collectible.name}, ${collectible.status} -> $newStatus")
        }
    }

    private fun sortList(list: List<Collectible>, field: SortField, asc: Boolean): List<Collectible> {
        val comparator = when (field) {
            SortField.NAME -> compareBy(collator, Collectible::name)
            SortField.IP_NAME -> compareBy(collator, Collectible::ipName)
            SortField.CHARACTER -> compareBy(collator, Collectible::characterTag)
            SortField.STATUS -> compareBy(collator) { it.status.name }
            SortField.PURCHASE_PRICE -> compareBy<Collectible> { it.purchasePrice }
            SortField.SELL_PRICE -> compareBy(nullsLast<Double>(), Collectible::sellPrice)
            SortField.PURCHASE_DATE -> compareBy<Collectible> { it.purchaseDate }
            SortField.CREATED_AT -> compareBy<Collectible> { it.createdAt }
        }
        return list.sortedWith(if (asc) comparator else comparator.reversed())
    }
}

data class LongPressMenuState(val collectible: Collectible)

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
