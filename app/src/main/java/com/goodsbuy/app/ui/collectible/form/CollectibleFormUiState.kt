package com.goodsbuy.app.ui.collectible.form

import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus

data class CollectibleFormUiState(
    val id: Long? = null,
    val name: String = "",
    val category: String = "",
    val type: String = "官方",
    val ipName: String = "",
    val seriesName: String = "",
    val characterTag: String = "",
    val remark: String = "",
    val purchaseChannel: String = "",
    val purchaseShop: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val purchasePrice: String = "",
    val purchaseQuantity: String = "1",
    val purchaseShipping: String = "0",
    val expectedPrice: String = "",
    val sellPrice: String = "",
    val sellQuantity: String = "",
    val sellShipping: String = "",
    val isFreeShipping: Boolean = false,
    val sellDate: Long? = null,
    val buyerInfo: String = "",
    val sellRemark: String = "",
    val status: OrderStatus = OrderStatus.OWNED,
    val storageStatus: StorageStatus = StorageStatus.IN_STOCK,
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)
