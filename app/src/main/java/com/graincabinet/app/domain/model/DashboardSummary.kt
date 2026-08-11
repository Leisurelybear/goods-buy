package com.graincabinet.app.domain.model

data class DashboardSummary(
    val totalInvestment: Double, val totalRevenue: Double, val holdingValue: Double,
    val totalProfit: Double, val totalProfitRate: Double, val totalCount: Int,
    val ownedCount: Int, val soldCount: Int
)
