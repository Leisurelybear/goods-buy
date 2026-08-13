package com.goodsbuy.app.ui.collectible.list

import com.goodsbuy.app.domain.model.Collectible

enum class SortField(val label: String) {
    CREATED_AT("创建时间"),
    NAME("名称"),
    IP_NAME("IP"),
    CHARACTER("角色"),
    PURCHASE_PRICE("入手价"),
    SELL_PRICE("售价"),
    PURCHASE_DATE("入手日期"),
    STATUS("状态")
}

data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val isBatchMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val sortField: SortField = SortField.CREATED_AT,
    val sortAscending: Boolean = false
)
