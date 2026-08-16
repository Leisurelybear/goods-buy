package com.goodsbuy.app.ui.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.di.DefaultDispatcher
import com.goodsbuy.app.util.CollectibleNameUtils
import com.goodsbuy.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val groupingDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _groupBy = MutableStateFlow(GroupBy.IP)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.getAllCollectibles(),
        _groupBy,
        _searchQuery
    ) { list, groupBy, searchQuery ->
        val query = searchQuery.trim()
        val filtered = if (query.isBlank()) list else list.filter { collectible ->
            listOf(
                collectible.name,
                collectible.ipName,
                collectible.seriesName,
                collectible.characterTag
            ).any { it.contains(query, ignoreCase = true) }
        }
        GalleryUiState(
            groupBy = groupBy,
            groups = groupCollectibles(filtered, groupBy),
            searchQuery = searchQuery,
            isLoading = false
        )
    }
        // Grouping and sorting can touch every record when the dimension changes.
        // Keep that work off the main thread so the gallery controls stay responsive.
        .flowOn(groupingDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GalleryUiState())

    fun setGroupBy(groupBy: GroupBy) {
        _groupBy.value = groupBy
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

    fun duplicateCollectible(collectible: Collectible) {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val ts = "%04d-%02d-%02d %02d:%02d".format(
                now.date.year, now.date.monthNumber, now.date.dayOfMonth, now.hour, now.minute
            )
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

    fun deleteCollectible(collectible: Collectible) {
        viewModelScope.launch {
            collectible.imagePaths.forEach { ImageUtils.deleteImage(context, it) }
            repository.deleteCollectible(collectible.id)
        }
    }
}
