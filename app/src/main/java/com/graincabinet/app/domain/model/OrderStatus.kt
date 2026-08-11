package com.graincabinet.app.domain.model

enum class OrderStatus(val displayName: String, val colorHex: Long) {
    PENDING_TAIL("待补尾款", 0xFFFF9800),
    PENDING_SHIPPING_FEE("待补邮", 0xFF2196F3),
    PENDING_SEND("待发货", 0xFF9C27B0),
    IN_TRANSIT_ORDER("运输中", 0xFF00BCD4),
    OWNED("已拥有", 0xFF4CAF50),
    HESITATING_SELL("犹豫出售", 0xFFFFB74D),
    LISTED("已挂出", 0xFF42A5F5),
    SOLD("已售出", 0xFF9E9E9E),
    GIFT("赠品/付邮送", 0xFFE91E63),
    LOST("遗失/损坏", 0xFFF44336);
    companion object {
        fun fromKey(key: String): OrderStatus = entries.firstOrNull { it.name == key } ?: OWNED
    }
}
