package com.goodsbuy.app.domain.validation

import com.goodsbuy.app.domain.model.OrderStatus

data class CollectibleInput(
    val name: String,
    val purchasePrice: String,
    val purchaseQuantity: String,
    val purchaseShipping: String,
    val expectedPrice: String,
    val sellPrice: String,
    val sellQuantity: String,
    val sellShipping: String,
    val status: OrderStatus
)

data class CollectibleValidationResult(
    val errors: Map<String, String>
) {
    val isValid: Boolean get() = errors.isEmpty()
}

object CollectibleValidator {
    fun validate(input: CollectibleInput): CollectibleValidationResult {
        val errors = linkedMapOf<String, String>()

        if (input.name.isBlank()) errors["name"] = "请输入制品名称"

        validateNonNegative(input.purchasePrice, "purchasePrice", "购买单价", errors)
        val quantity = input.purchaseQuantity.toIntOrNull()
        if (quantity == null || quantity <= 0) errors["purchaseQuantity"] = "购入数量必须是大于 0 的整数"
        validateNonNegative(input.purchaseShipping, "purchaseShipping", "购入运费", errors)
        validateNonNegative(input.expectedPrice, "expectedPrice", "心理预期价", errors, allowBlank = true)

        val sellPrice = validateNonNegative(input.sellPrice, "sellPrice", "售出单价", errors, allowBlank = true)
        val sellQuantity = validatePositiveInt(input.sellQuantity, "sellQuantity", "售出数量", errors, allowBlank = true)
        validateNonNegative(input.sellShipping, "sellShipping", "售出运费", errors, allowBlank = true)

        if (input.status == OrderStatus.SOLD) {
            if (sellPrice == null) errors["sellPrice"] = "已售出藏品必须填写售出单价"
            if (sellQuantity == null) errors["sellQuantity"] = "已售出藏品必须填写售出数量"
        }
        if (sellQuantity != null && quantity != null && sellQuantity > quantity) {
            errors["sellQuantity"] = "售出数量不能超过购入数量"
        }

        return CollectibleValidationResult(errors)
    }

    private fun validateNonNegative(
        value: String,
        key: String,
        label: String,
        errors: MutableMap<String, String>,
        allowBlank: Boolean = false
    ): Double? {
        if (value.isBlank() && allowBlank) return null
        val parsed = value.toDoubleOrNull()
        if (parsed == null || parsed < 0 || parsed.isNaN() || parsed.isInfinite()) {
            errors[key] = "${label}必须是大于等于 0 的数字"
            return null
        }
        return parsed
    }

    private fun validatePositiveInt(
        value: String,
        key: String,
        label: String,
        errors: MutableMap<String, String>,
        allowBlank: Boolean
    ): Int? {
        if (value.isBlank() && allowBlank) return null
        val parsed = value.toIntOrNull()
        if (parsed == null || parsed <= 0) errors[key] = "${label}必须是大于 0 的整数"
        return parsed?.takeIf { it > 0 }
    }
}
