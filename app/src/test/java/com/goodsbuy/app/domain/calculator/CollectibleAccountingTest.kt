package com.goodsbuy.app.domain.calculator

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectibleAccountingTest {

    @Test
    fun `partial sale allocates purchase shipping by sold quantity`() {
        val collectible = collectible(
            purchaseQuantity = 4,
            purchasePrice = 10.0,
            purchaseShipping = 8.0,
            sellQuantity = 2,
            sellPrice = 20.0,
            sellShipping = 4.0,
            status = OrderStatus.SOLD
        )

        assertEquals(48.0, CollectibleAccounting.purchaseTotal(collectible), 0.001)
        assertEquals(24.0, CollectibleAccounting.realizedCost(collectible), 0.001)
        assertEquals(44.0, CollectibleAccounting.saleRevenue(collectible), 0.001)
        assertEquals(2, CollectibleAccounting.remainingQuantity(collectible))
    }

    @Test
    fun `sold record without quantity defaults to all purchased units`() {
        val collectible = collectible(
            purchaseQuantity = 3,
            purchasePrice = 12.0,
            sellPrice = 20.0,
            sellQuantity = null,
            status = OrderStatus.SOLD
        )

        assertEquals(3, CollectibleAccounting.realizedQuantity(collectible))
        assertEquals(36.0, CollectibleAccounting.realizedCost(collectible), 0.001)
        assertEquals(60.0, CollectibleAccounting.saleRevenue(collectible), 0.001)
    }

    private fun collectible(
        purchaseQuantity: Int,
        purchasePrice: Double,
        purchaseShipping: Double = 0.0,
        sellQuantity: Int?,
        sellPrice: Double?,
        sellShipping: Double? = null,
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
        purchaseDate = 0,
        purchasePrice = purchasePrice,
        purchaseQuantity = purchaseQuantity,
        purchaseShipping = purchaseShipping,
        expectedPrice = 0.0,
        sellPrice = sellPrice,
        sellQuantity = sellQuantity,
        sellShipping = sellShipping,
        isFreeShipping = false,
        sellDate = null,
        buyerInfo = null,
        sellRemark = null,
        status = status,
        storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(),
        createdAt = 0,
        updatedAt = 0
    )
}
