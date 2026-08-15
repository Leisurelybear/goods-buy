package com.goodsbuy.app.domain.calculator

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus

/**
 * Shared accounting rules for a collectible.
 *
 * Shipping paid at purchase is allocated per unit so a partial sale only
 * realizes the cost of the units that were actually sold. Keeping these
 * rules in one place prevents the detail page, dashboard and charts from
 * drifting apart.
 */
object CollectibleAccounting {

    private val settledStatuses = setOf(OrderStatus.SOLD, OrderStatus.GIFT, OrderStatus.LOST)

    fun purchaseQuantity(collectible: Collectible): Int = collectible.purchaseQuantity.coerceAtLeast(0)

    fun purchaseTotal(collectible: Collectible): Double =
        collectible.purchasePrice * purchaseQuantity(collectible) + collectible.purchaseShipping

    fun realizedQuantity(collectible: Collectible): Int {
        val purchased = purchaseQuantity(collectible)
        if (purchased == 0) return 0
        val requested = collectible.sellQuantity
            ?: if (collectible.status in settledStatuses) purchased else 0
        return requested.coerceIn(0, purchased)
    }

    fun remainingQuantity(collectible: Collectible): Int =
        (purchaseQuantity(collectible) - realizedQuantity(collectible)).coerceAtLeast(0)

    fun unitCost(collectible: Collectible): Double {
        val quantity = purchaseQuantity(collectible)
        return if (quantity == 0) 0.0 else purchaseTotal(collectible) / quantity
    }

    fun realizedCost(collectible: Collectible): Double =
        unitCost(collectible) * realizedQuantity(collectible)

    fun saleRevenue(collectible: Collectible): Double {
        if (collectible.status != OrderStatus.SOLD && collectible.sellPrice == null) return 0.0
        val quantity = realizedQuantity(collectible)
        if (quantity == 0) return 0.0
        val itemRevenue = (collectible.sellPrice ?: 0.0) * quantity
        val shippingRevenue = if (collectible.isFreeShipping) 0.0 else (collectible.sellShipping ?: 0.0)
        return itemRevenue + shippingRevenue
    }

    fun isSettled(collectible: Collectible): Boolean = collectible.status in settledStatuses
}
