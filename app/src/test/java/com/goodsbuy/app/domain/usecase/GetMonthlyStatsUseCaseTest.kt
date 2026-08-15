package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMonthlyStatsUseCaseTest {

    @Test
    fun `purchase expense and sale income use their own months`() {
        val collectible = collectible(
            purchaseDate = date("2026-01-10"),
            sellDate = date("2026-02-03"),
            purchasePrice = 100.0,
            purchaseShipping = 10.0,
            sellPrice = 180.0,
            sellShipping = 5.0,
            status = OrderStatus.SOLD
        )

        val stats = GetMonthlyStatsUseCase()(listOf(collectible))

        assertEquals(listOf("2026-01", "2026-02"), stats.map { it.yearMonth })
        assertEquals(110.0, stats[0].expense, 0.001)
        assertEquals(0.0, stats[0].income, 0.001)
        assertEquals(0.0, stats[1].expense, 0.001)
        assertEquals(185.0, stats[1].income, 0.001)
    }

    private fun date(value: String): Long = LocalDate.parse(value)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun collectible(
        purchaseDate: Long,
        sellDate: Long,
        purchasePrice: Double,
        purchaseShipping: Double,
        sellPrice: Double,
        sellShipping: Double,
        status: OrderStatus
    ) = Collectible(
        id = 1,
        name = "测试藏品",
        category = "",
        type = "",
        ipName = "",
        seriesName = "",
        characterTag = "",
        remark = "",
        purchaseChannel = "",
        purchaseShop = "",
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        purchaseQuantity = 1,
        purchaseShipping = purchaseShipping,
        expectedPrice = 0.0,
        sellPrice = sellPrice,
        sellQuantity = 1,
        sellShipping = sellShipping,
        isFreeShipping = false,
        sellDate = sellDate,
        buyerInfo = null,
        sellRemark = null,
        status = status,
        storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(),
        createdAt = 0,
        updatedAt = 0
    )
}
