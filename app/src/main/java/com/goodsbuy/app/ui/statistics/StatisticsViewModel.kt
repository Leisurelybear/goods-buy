package com.goodsbuy.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.domain.usecase.GetCategoryStatsUseCase
import com.goodsbuy.app.domain.usecase.GetDashboardSummaryUseCase
import com.goodsbuy.app.domain.usecase.GetMonthlyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getCategoryStats: GetCategoryStatsUseCase,
    private val getMonthlyStats: GetMonthlyStatsUseCase
) : ViewModel() {

    private val _categoryType = MutableStateFlow("ip")
    private val _statusFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getAllCollectibles(), _categoryType, _statusFilter
    ) { collectibles, categoryType, status ->
        val filtered = if (status != null) collectibles.filter { it.status.name == status } else collectibles
        StatisticsUiState(
            summary = getDashboardSummary(filtered),
            categoryStats = getCategoryStats(filtered, categoryType),
            monthlyStats = getMonthlyStats(filtered),
            categoryType = categoryType,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    fun changeCategoryType(type: String) { _categoryType.value = type }
    fun changeStatusFilter(status: String?) { _statusFilter.value = status }
}
