package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.CategoryStat
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.calculator.CollectibleAccounting
import com.goodsbuy.app.domain.model.OrderStatus
import javax.inject.Inject

class GetCategoryStatsUseCase @Inject constructor() {
    operator fun invoke(collectibles: List<Collectible>, categoryType: String): List<CategoryStat> {
        return collectibles.groupBy {
            when (categoryType) {
                "ip" -> it.ipName
                "series" -> it.seriesName
                "category" -> it.category
                else -> it.ipName
            }
        }.map { (name, items) ->
            // All four figures come from the same shared accounting rules so
            // 投入 / 成本 / 收入 / 利润率 stay consistent within a category.
            val grossInvestment = items.sumOf { CollectibleAccounting.purchaseTotal(it) }
            val settled = items.filter { CollectibleAccounting.isSettled(it) }
            val realizedCost = settled.sumOf { CollectibleAccounting.realizedCost(it) }
            val revenue = items.filter { it.status == OrderStatus.SOLD }
                .sumOf { CollectibleAccounting.saleRevenue(it) }
            val holdingValue = items.filterNot { CollectibleAccounting.isSettled(it) }
                .sumOf { it.expectedPrice * CollectibleAccounting.remainingQuantity(it) }
            CategoryStat(
                categoryName = name,
                count = items.size,
                grossInvestment = grossInvestment,
                realizedCost = realizedCost,
                holdingValue = holdingValue,
                revenue = revenue,
                profit = revenue - realizedCost
            )
        }.sortedByDescending { it.profit }
    }
}
