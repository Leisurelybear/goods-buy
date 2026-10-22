package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDashboardSummaryUseCaseTest {

    private val useCase = GetDashboardSummaryUseCase()

    @Test
    fun `holding value multiplies unit price by remaining quantity`() {
        // 4 bought, 1 already sold -> only 3 units are still held.
        val partiallySold = collectible(
            id = 1, purchaseQuantity = 4, purchasePrice = 10.0, purchaseShipping = 8.0,
            expectedPrice = 30.0, sellQuantity = 1, sellPrice = 20.0, status = OrderStatus.OWNED
        )

        val summary = useCase(listOf(partiallySold))

        assertEquals(48.0, summary.totalInvestment, 0.001)
        assertEquals(90.0, summary.holdingValue, 0.001)
        assertEquals(1, summary.ownedCount)
    }

    @Test
    fun `profit and rate use settled cost as the denominator`() {
        val held = collectible(
            id = 1, purchaseQuantity = 4, purchasePrice = 10.0, purchaseShipping = 8.0,
            expectedPrice = 30.0, sellQuantity = 1, sellPrice = 20.0, status = OrderStatus.OWNED
        )
        val sold = collectible(
            id = 2, purchaseQuantity = 1, purchasePrice = 100.0, purchaseShipping = 0.0,
            expectedPrice = 0.0, sellQuantity = 1, sellPrice = 180.0, sellShipping = 5.0,
            status = OrderStatus.SOLD
        )

        val summary = useCase(listOf(held, sold))

        assertEquals(148.0, summary.totalInvestment, 0.001)
        assertEquals(185.0, summary.totalRevenue, 0.001)
        assertEquals(90.0, summary.holdingValue, 0.001)
        assertEquals(85.0, summary.totalProfit, 0.001)
        assertEquals(85.0, summary.totalProfitRate, 0.001)
        assertEquals(1, summary.soldCount)
    }

    private fun collectible(
        id: Long,
        purchaseQuantity: Int,
        purchasePrice: Double,
        purchaseShipping: Double,
        expectedPrice: Double,
        sellQuantity: Int?,
        sellPrice: Double?,
        sellShipping: Double? = null,
        status: OrderStatus
    ) = Collectible(
        id = id, name = "藏品$id", category = "", type = "",
        ipName = "", seriesName = "", characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = purchasePrice, purchaseQuantity = purchaseQuantity, purchaseShipping = purchaseShipping,
        expectedPrice = expectedPrice, sellPrice = sellPrice, sellQuantity = sellQuantity,
        sellShipping = sellShipping, isFreeShipping = false, sellDate = null,
        buyerInfo = null, sellRemark = null, status = status, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = 0, updatedAt = 0
    )
}
