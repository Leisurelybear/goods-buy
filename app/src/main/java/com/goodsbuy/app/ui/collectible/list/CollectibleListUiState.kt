package com.goodsbuy.app.ui.collectible.list

import com.goodsbuy.app.domain.model.Collectible

data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null
)
