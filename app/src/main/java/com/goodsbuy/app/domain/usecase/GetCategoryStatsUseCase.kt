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
            val investment = items.sumOf { CollectibleAccounting.purchaseTotal(it) }
            val soldItems = items.filter { it.status == OrderStatus.SOLD }
            val revenue = soldItems.sumOf { CollectibleAccounting.saleRevenue(it) }
            val costOfSold = items.filter { CollectibleAccounting.isSettled(it) }
                .sumOf { CollectibleAccounting.realizedCost(it) }
            CategoryStat(categoryName = name, count = items.size, investment = investment, revenue = revenue, profit = revenue - costOfSold)
        }.sortedByDescending { it.profit }
    }
}
