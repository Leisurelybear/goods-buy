package com.graincabinet.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graincabinet.app.domain.model.Collectible
import com.graincabinet.app.domain.repository.CollectibleRepository
import com.graincabinet.app.domain.usecase.GetCategoryStatsUseCase
import com.graincabinet.app.domain.usecase.GetDashboardSummaryUseCase
import com.graincabinet.app.domain.usecase.GetMonthlyStatsUseCase
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

    val uiState: StateFlow<StatisticsUiState> = combine(
        repository.getAllCollectibles(), _categoryType
    ) { collectibles, categoryType ->
        StatisticsUiState(
            summary = getDashboardSummary(collectibles),
            categoryStats = getCategoryStats(collectibles, categoryType),
            monthlyStats = getMonthlyStats(collectibles),
            categoryType = categoryType,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    fun changeCategoryType(type: String) { _categoryType.value = type }
}
