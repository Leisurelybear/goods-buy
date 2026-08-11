package com.goodsbuy.app.ui.collectible.detail

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.ProfitLoss

data class CollectibleDetailUiState(
    val collectible: Collectible? = null,
    val profitLoss: ProfitLoss? = null,
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false
)
