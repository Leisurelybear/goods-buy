package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible

enum class GroupBy { IP, SERIES }

data class GalleryGroup(
    val name: String,
    val count: Int,
    val collectibles: List<Collectible>
)

data class GalleryUiState(
    val groupBy: GroupBy = GroupBy.IP,
    val groups: List<GalleryGroup> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)
