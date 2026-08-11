package com.graincabinet.app.ui.collectible.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graincabinet.app.domain.calculator.ProfitLossCalculator
import com.graincabinet.app.domain.model.OrderStatus
import com.graincabinet.app.domain.repository.CollectibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectibleDetailViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    private val calculator: ProfitLossCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectibleDetailUiState())
    val uiState: StateFlow<CollectibleDetailUiState> = _uiState.asStateFlow()

    fun loadCollectible(id: Long) {
        viewModelScope.launch {
            val collectible = repository.getCollectibleById(id)
            val pl = if (collectible != null && collectible.status == OrderStatus.SOLD) calculator.calculate(collectible) else null
            _uiState.update { it.copy(collectible = collectible, profitLoss = pl, isLoading = false) }
        }
    }

    fun updateStatus(newStatus: OrderStatus) {
        val collectible = _uiState.value.collectible ?: return
        viewModelScope.launch {
            val updated = collectible.copy(
                status = newStatus,
                sellDate = if (newStatus == OrderStatus.SOLD) System.currentTimeMillis() else collectible.sellDate
            )
            repository.updateCollectible(updated)
            val pl = if (updated.status == OrderStatus.SOLD) calculator.calculate(updated) else null
            _uiState.update { it.copy(collectible = updated, profitLoss = pl) }
        }
    }

    fun markAsSold() {
        updateStatus(OrderStatus.SOLD)
    }

    fun deleteCollectible() {
        val id = _uiState.value.collectible?.id ?: return
        viewModelScope.launch {
            repository.deleteCollectible(id)
            _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }
}
