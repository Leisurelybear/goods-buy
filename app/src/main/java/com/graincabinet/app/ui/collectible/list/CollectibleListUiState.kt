package com.graincabinet.app.ui.collectible.list

import com.graincabinet.app.domain.model.Collectible

data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null
)
