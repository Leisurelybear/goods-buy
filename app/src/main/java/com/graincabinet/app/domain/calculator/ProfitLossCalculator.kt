package com.graincabinet.app.domain.calculator

import com.graincabinet.app.domain.model.Collectible
import com.graincabinet.app.domain.model.ProfitLoss
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfitLossCalculator @Inject constructor() {

    fun calculate(collectible: Collectible): ProfitLoss {
        val totalCost = collectible.purchasePrice * collectible.purchaseQuantity + collectible.purchaseShipping
        val totalRevenue = (collectible.sellPrice ?: 0.0) * (collectible.sellQuantity ?: 0) +
                if (collectible.isFreeShipping) 0.0 else (collectible.sellShipping ?: 0.0)
        val profitAmount = totalRevenue - totalCost
        val profitRate = if (totalCost > 0) (profitAmount / totalCost) * 100 else 0.0
        return ProfitLoss(totalCost = totalCost, totalRevenue = totalRevenue, profitAmount = profitAmount, profitRate = profitRate)
    }

    fun calculateBatch(collectibles: List<Collectible>): List<Pair<Collectible, ProfitLoss>> =
        collectibles.map { it to calculate(it) }
}
