package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.MonthlyStat
import com.goodsbuy.app.domain.calculator.CollectibleAccounting
import com.goodsbuy.app.domain.model.OrderStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class GetMonthlyStatsUseCase @Inject constructor() {
    operator fun invoke(collectibles: List<Collectible>): List<MonthlyStat> {
        val monthly = mutableMapOf<String, MonthlyTotals>()
        collectibles.forEach { collectible ->
            if (collectible.purchaseDate > 0) {
                val month = monthOf(collectible.purchaseDate)
                monthly.getOrPut(month) { MonthlyTotals() }.expense +=
                    CollectibleAccounting.purchaseTotal(collectible)
            }

            // Income belongs to the sale month, not the purchase month. If a
            // legacy record has no sale date, fall back to its purchase month
            // so the revenue is not silently omitted from the chart.
            if (collectible.status == OrderStatus.SOLD) {
                val saleDate = collectible.sellDate ?: collectible.purchaseDate
                if (saleDate > 0) {
                    val month = monthOf(saleDate)
                    monthly.getOrPut(month) { MonthlyTotals() }.income +=
                        CollectibleAccounting.saleRevenue(collectible)
                }
            }
        }
        return monthly.toSortedMap().map { (month, totals) ->
            MonthlyStat(yearMonth = month, expense = totals.expense, income = totals.income)
        }
    }

    private fun monthOf(epochMillis: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM", Locale.getDefault()).format(
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        )

    private data class MonthlyTotals(var expense: Double = 0.0, var income: Double = 0.0)
}
