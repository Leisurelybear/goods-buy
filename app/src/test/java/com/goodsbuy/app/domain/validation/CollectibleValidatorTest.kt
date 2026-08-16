package com.goodsbuy.app.domain.validation

import com.goodsbuy.app.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectibleValidatorTest {
    private fun input(
        name: String = "挂件",
        price: String = "10",
        quantity: String = "2",
        shipping: String = "0",
        expected: String = "",
        sellPrice: String = "",
        sellQuantity: String = "",
        sellShipping: String = "",
        status: OrderStatus = OrderStatus.OWNED
    ) = CollectibleInput(name, price, quantity, shipping, expected, sellPrice, sellQuantity, sellShipping, status)

    @Test
    fun `owned item accepts optional sale fields`() {
        assertTrue(CollectibleValidator.validate(input()).isValid)
    }

    @Test
    fun `sold item requires sale price and quantity`() {
        val errors = CollectibleValidator.validate(input(status = OrderStatus.SOLD)).errors
        assertTrue(errors.containsKey("sellPrice"))
        assertTrue(errors.containsKey("sellQuantity"))
    }

    @Test
    fun `sale quantity cannot exceed purchase quantity`() {
        val errors = CollectibleValidator.validate(input(sellPrice = "20", sellQuantity = "3", status = OrderStatus.SOLD)).errors
        assertEquals("售出数量不能超过购入数量", errors["sellQuantity"])
    }

    @Test
    fun `negative and zero quantities are rejected`() {
        val errors = CollectibleValidator.validate(input(price = "-1", quantity = "0")).errors
        assertTrue(errors.containsKey("purchasePrice"))
        assertTrue(errors.containsKey("purchaseQuantity"))
    }
}
