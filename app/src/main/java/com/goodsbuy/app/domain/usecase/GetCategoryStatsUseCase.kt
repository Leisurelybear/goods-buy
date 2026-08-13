package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.CategoryStat
import com.goodsbuy.app.domain.model.Collectible
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
            val investment = items.sumOf { it.purchasePrice * it.purchaseQuantity + it.purchaseShipping }
            val soldItems = items.filter { it.status.name == "SOLD" }
            val revenue = soldItems.sumOf { (it.sellPrice ?: 0.0) * (it.sellQuantity ?: 0) }
            val costOfSold = soldItems.sumOf { it.purchasePrice * it.purchaseQuantity + it.purchaseShipping }
            CategoryStat(categoryName = name, count = items.size, investment = investment, revenue = revenue, profit = revenue - costOfSold)
        }.sortedByDescending { it.profit }
    }
}
