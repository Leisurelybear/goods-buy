package com.goodsbuy.app.ui.collectible.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectibleFormViewModel @Inject constructor(
    private val repository: CollectibleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectibleFormUiState())
    val uiState: StateFlow<CollectibleFormUiState> = _uiState.asStateFlow()

    fun loadCollectible(id: Long) {
        viewModelScope.launch {
            val collectible = repository.getCollectibleById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    id = collectible.id, name = collectible.name, category = collectible.category,
                    type = collectible.type, ipName = collectible.ipName, seriesName = collectible.seriesName,
                    characterTag = collectible.characterTag, remark = collectible.remark,
                    purchaseChannel = collectible.purchaseChannel, purchaseShop = collectible.purchaseShop,
                    purchaseDate = collectible.purchaseDate, purchasePrice = collectible.purchasePrice.toString(),
                    purchaseQuantity = collectible.purchaseQuantity.toString(),
                    purchaseShipping = collectible.purchaseShipping.toString(),
                    expectedPrice = collectible.expectedPrice.toString(),
                    sellPrice = collectible.sellPrice?.toString() ?: "",
                    sellQuantity = collectible.sellQuantity?.toString() ?: "",
                    sellShipping = collectible.sellShipping?.toString() ?: "",
                    isFreeShipping = collectible.isFreeShipping, sellDate = collectible.sellDate,
                    buyerInfo = collectible.buyerInfo ?: "", sellRemark = collectible.sellRemark ?: "",
                    status = collectible.status, storageStatus = collectible.storageStatus,
                    imagePaths = collectible.imagePaths
                )
            }
        }
    }

    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            when (field) {
                "name" -> state.copy(name = value)
                "category" -> state.copy(category = value)
                "type" -> state.copy(type = value)
                "ipName" -> state.copy(ipName = value)
                "seriesName" -> state.copy(seriesName = value)
                "characterTag" -> state.copy(characterTag = value)
                "remark" -> state.copy(remark = value)
                "purchaseChannel" -> state.copy(purchaseChannel = value)
                "purchaseShop" -> state.copy(purchaseShop = value)
                "purchasePrice" -> state.copy(purchasePrice = value)
                "purchaseQuantity" -> state.copy(purchaseQuantity = value)
                "purchaseShipping" -> state.copy(purchaseShipping = value)
                "expectedPrice" -> state.copy(expectedPrice = value)
                "sellPrice" -> state.copy(sellPrice = value)
                "sellQuantity" -> state.copy(sellQuantity = value)
                "sellShipping" -> state.copy(sellShipping = value)
                "buyerInfo" -> state.copy(buyerInfo = value)
                "sellRemark" -> state.copy(sellRemark = value)
                else -> state
            }
        }
    }

    fun updateStatus(status: OrderStatus) { _uiState.update { it.copy(status = status) } }
    fun updateStorageStatus(storage: StorageStatus) { _uiState.update { it.copy(storageStatus = storage) } }
    fun updateFreeShipping(free: Boolean) { _uiState.update { it.copy(isFreeShipping = free) } }
    fun addImagePath(path: String) { _uiState.update { it.copy(imagePaths = it.imagePaths + path) } }
    fun removeImagePath(index: Int) { _uiState.update { state -> state.copy(imagePaths = state.imagePaths.filterIndexed { i, _ -> i != index }) } }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val collectible = Collectible(
                id = state.id ?: 0, name = state.name, category = state.category, type = state.type,
                ipName = state.ipName, seriesName = state.seriesName, characterTag = state.characterTag,
                remark = state.remark, purchaseChannel = state.purchaseChannel, purchaseShop = state.purchaseShop,
                purchaseDate = state.purchaseDate, purchasePrice = state.purchasePrice.toDoubleOrNull() ?: 0.0,
                purchaseQuantity = state.purchaseQuantity.toIntOrNull() ?: 1,
                purchaseShipping = state.purchaseShipping.toDoubleOrNull() ?: 0.0,
                expectedPrice = state.expectedPrice.toDoubleOrNull() ?: 0.0,
                sellPrice = state.sellPrice.toDoubleOrNull(), sellQuantity = state.sellQuantity.toIntOrNull(),
                sellShipping = state.sellShipping.toDoubleOrNull(), isFreeShipping = state.isFreeShipping,
                sellDate = state.sellDate, buyerInfo = state.buyerInfo, sellRemark = state.sellRemark,
                status = state.status, storageStatus = state.storageStatus, imagePaths = state.imagePaths,
                createdAt = if (state.id != null) 0 else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (state.id != null) repository.updateCollectible(collectible) else repository.insertCollectible(collectible)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
