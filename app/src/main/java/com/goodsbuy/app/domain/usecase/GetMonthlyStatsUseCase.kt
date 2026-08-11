package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.MonthlyStat
import javax.inject.Inject

class GetMonthlyStatsUseCase @Inject constructor() {
    operator fun invoke(collectibles: List<Collectible>): List<MonthlyStat> {
        val grouped = collectibles.groupBy {
            val date = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            date.format(java.util.Date(it.purchaseDate))
        }
        return grouped.map { (month, items) ->
            val expense = items.sumOf { it.purchasePrice * it.purchaseQuantity + it.purchaseShipping }
            val income = items.filter { it.status.name == "SOLD" }.sumOf { (it.sellPrice ?: 0.0) * (it.sellQuantity ?: 0) }
            MonthlyStat(yearMonth = month, expense = expense, income = income)
        }.sortedBy { it.yearMonth }
    }
}
