package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.DashboardSummary
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.calculator.CollectibleAccounting
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor() {
    private val settledStatuses = setOf(OrderStatus.SOLD, OrderStatus.GIFT, OrderStatus.LOST)

    operator fun invoke(collectibles: List<Collectible>): DashboardSummary {
        val totalInvestment = collectibles.sumOf { CollectibleAccounting.purchaseTotal(it) }
        val soldOnes = collectibles.filter { it.status == OrderStatus.SOLD }
        val totalRevenue = soldOnes.sumOf { CollectibleAccounting.saleRevenue(it) }
        val costOfSettled = collectibles.filter { it.status in settledStatuses }
            .sumOf { CollectibleAccounting.realizedCost(it) }
        val holdingValue = collectibles.filter { it.status !in settledStatuses }.sumOf { it.expectedPrice }
        val totalProfit = totalRevenue - costOfSettled
        val totalProfitRate = if (costOfSettled > 0) (totalProfit / costOfSettled) * 100 else 0.0
        return DashboardSummary(
            totalInvestment = totalInvestment, totalRevenue = totalRevenue, holdingValue = holdingValue,
            totalProfit = totalProfit, totalProfitRate = totalProfitRate, totalCount = collectibles.size,
            ownedCount = collectibles.count { it.status !in settledStatuses },
            soldCount = soldOnes.size
        )
    }
}
