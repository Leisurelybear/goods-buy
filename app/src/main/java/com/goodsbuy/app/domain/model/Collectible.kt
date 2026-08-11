package com.goodsbuy.app.domain.model

data class Collectible(
    val id: Long = 0, val name: String, val category: String, val type: String,
    val ipName: String, val seriesName: String, val characterTag: String, val remark: String,
    val purchaseChannel: String, val purchaseShop: String, val purchaseDate: Long,
    val purchasePrice: Double, val purchaseQuantity: Int, val purchaseShipping: Double,
    val expectedPrice: Double, val sellPrice: Double?, val sellQuantity: Int?,
    val sellShipping: Double?, val isFreeShipping: Boolean, val sellDate: Long?,
    val buyerInfo: String?, val sellRemark: String?, val status: OrderStatus,
    val storageStatus: StorageStatus, val imagePaths: List<String>,
    val createdAt: Long, val updatedAt: Long
)
