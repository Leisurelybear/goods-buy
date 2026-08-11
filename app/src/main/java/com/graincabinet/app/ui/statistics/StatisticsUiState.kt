package com.graincabinet.app.ui.statistics

import com.graincabinet.app.domain.model.CategoryStat
import com.graincabinet.app.domain.model.DashboardSummary
import com.graincabinet.app.domain.model.MonthlyStat

data class StatisticsUiState(
    val summary: DashboardSummary = DashboardSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0),
    val categoryStats: List<CategoryStat> = emptyList(),
    val monthlyStats: List<MonthlyStat> = emptyList(),
    val categoryType: String = "ip",
    val isLoading: Boolean = true
)
