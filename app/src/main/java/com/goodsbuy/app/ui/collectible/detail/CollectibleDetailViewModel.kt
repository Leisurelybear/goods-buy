package com.goodsbuy.app.ui.collectible.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.calculator.ProfitLossCalculator
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.util.ImageUtils
import com.goodsbuy.app.util.AppLogger
import com.goodsbuy.app.util.UndoDeleteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectibleDetailViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    private val calculator: ProfitLossCalculator,
    private val undoDeleteManager: UndoDeleteManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectibleDetailUiState())
    val uiState: StateFlow<CollectibleDetailUiState> = _uiState.asStateFlow()

    fun loadCollectible(id: Long) {
        viewModelScope.launch {
            val collectible = repository.getCollectibleById(id)
            val pl = if (collectible != null && (collectible.status == OrderStatus.SOLD || collectible.sellPrice != null)) calculator.calculate(collectible) else null
            _uiState.update { it.copy(collectible = collectible, profitLoss = pl, isLoading = false) }
        }
    }

    fun updateStatus(newStatus: OrderStatus) {
        val collectible = _uiState.value.collectible ?: return
        viewModelScope.launch {
            val updated = collectible.copy(
                status = newStatus,
                sellDate = if (newStatus == OrderStatus.SOLD && collectible.sellDate == null) System.currentTimeMillis() else collectible.sellDate
            )
            repository.updateCollectible(updated)
            val pl = if (updated.status == OrderStatus.SOLD || updated.sellPrice != null) calculator.calculate(updated) else null
            _uiState.update { it.copy(collectible = updated, profitLoss = pl) }
            AppLogger.i("Status", "Detail update: id=${collectible.id}, name=${collectible.name}, ${collectible.status} -> $newStatus")
        }
    }

    fun markAsSold() {
        updateStatus(OrderStatus.SOLD)
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteCollectible(onDeleted: (() -> Unit)? = null) {
        val collectible = _uiState.value.collectible ?: return
        viewModelScope.launch {
            undoDeleteManager.delete(listOf(collectible))
            _uiState.update { it.copy(showDeleteDialog = false, collectible = null) }
            AppLogger.i("Delete", "Detail delete: id=${collectible.id}, name=${collectible.name}")
            onDeleted?.invoke()
        }
    }
}
