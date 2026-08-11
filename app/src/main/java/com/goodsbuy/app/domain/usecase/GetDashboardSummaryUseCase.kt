package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.DashboardSummary
import com.goodsbuy.app.domain.model.OrderStatus
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor() {
    operator fun invoke(collectibles: List<Collectible>): DashboardSummary {
        val totalInvestment = collectibles.sumOf { it.purchasePrice * it.purchaseQuantity + it.purchaseShipping }
        val soldOnes = collectibles.filter { it.status == OrderStatus.SOLD }
        val totalRevenue = soldOnes.sumOf { (it.sellPrice ?: 0.0) * (it.sellQuantity ?: 0) + if (it.isFreeShipping) 0.0 else (it.sellShipping ?: 0.0) }
        val holdingValue = collectibles.filter { it.status == OrderStatus.OWNED }.sumOf { it.expectedPrice }
        val totalProfit = totalRevenue - totalInvestment
        val totalProfitRate = if (totalInvestment > 0) (totalProfit / totalInvestment) * 100 else 0.0
        return DashboardSummary(
            totalInvestment = totalInvestment, totalRevenue = totalRevenue, holdingValue = holdingValue,
            totalProfit = totalProfit, totalProfitRate = totalProfitRate, totalCount = collectibles.size,
            ownedCount = collectibles.count { it.status == OrderStatus.OWNED },
            soldCount = soldOnes.size
        )
    }
}
