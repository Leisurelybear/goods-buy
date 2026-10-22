package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCategoryStatsUseCaseTest {

    private val useCase = GetCategoryStatsUseCase()

    @Test
    fun `category breakdown separates gross investment from settled cost`() {
        val held = collectible(
            id = 1, ipName = "原神", purchaseQuantity = 4, purchasePrice = 10.0,
            purchaseShipping = 8.0, expectedPrice = 30.0, sellQuantity = 1,
            sellPrice = 20.0, status = OrderStatus.OWNED
        )
        val sold = collectible(
            id = 2, ipName = "原神", purchaseQuantity = 1, purchasePrice = 100.0,
            purchaseShipping = 0.0, expectedPrice = 0.0, sellQuantity = 1,
            sellPrice = 180.0, sellShipping = 5.0, status = OrderStatus.SOLD
        )

        val stats = useCase(listOf(held, sold), "ip")

        assertEquals(1, stats.size)
        val stat = stats.first()
        assertEquals("原神", stat.categoryName)
        assertEquals(2, stat.count)
        assertEquals(148.0, stat.grossInvestment, 0.001)
        assertEquals(100.0, stat.realizedCost, 0.001)
        assertEquals(90.0, stat.holdingValue, 0.001)
        assertEquals(185.0, stat.revenue, 0.001)
        assertEquals(85.0, stat.profit, 0.001)
    }

    @Test
    fun `profit rate derived from realized cost reconciles with profit`() {
        val sold = collectible(
            id = 1, ipName = "蔚蓝档案", purchaseQuantity = 1, purchasePrice = 100.0,
            purchaseShipping = 0.0, expectedPrice = 0.0, sellQuantity = 1,
            sellPrice = 150.0, status = OrderStatus.SOLD
        )

        val stat = useCase(listOf(sold), "ip").first()

        val rate = stat.profit / stat.realizedCost * 100
        assertEquals(50.0, rate, 0.001)
        assertEquals(0.0, stat.holdingValue, 0.001)
    }

    private fun collectible(
        id: Long,
        ipName: String,
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
        ipName = ipName, seriesName = "", characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = purchasePrice, purchaseQuantity = purchaseQuantity, purchaseShipping = purchaseShipping,
        expectedPrice = expectedPrice, sellPrice = sellPrice, sellQuantity = sellQuantity,
        sellShipping = sellShipping, isFreeShipping = false, sellDate = null,
        buyerInfo = null, sellRemark = null, status = status, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = 0, updatedAt = 0
    )
}
