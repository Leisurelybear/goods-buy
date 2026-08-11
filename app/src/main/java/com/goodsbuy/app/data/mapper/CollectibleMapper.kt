package com.goodsbuy.app.data.mapper

import com.goodsbuy.app.data.entity.CollectibleEntity
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus

fun CollectibleEntity.toDomain(): Collectible = Collectible(
    id = id, name = name, category = category, type = type,
    ipName = ipName, seriesName = seriesName, characterTag = characterTag, remark = remark,
    purchaseChannel = purchaseChannel, purchaseShop = purchaseShop, purchaseDate = purchaseDate,
    purchasePrice = purchasePrice, purchaseQuantity = purchaseQuantity, purchaseShipping = purchaseShipping,
    expectedPrice = expectedPrice, sellPrice = sellPrice, sellQuantity = sellQuantity,
    sellShipping = sellShipping, isFreeShipping = isFreeShipping, sellDate = sellDate,
    buyerInfo = buyerInfo, sellRemark = sellRemark,
    status = OrderStatus.fromKey(status), storageStatus = StorageStatus.fromKey(storageStatus),
    imagePaths = if (imagePaths.isEmpty()) emptyList() else imagePaths.split(","),
    createdAt = createdAt, updatedAt = updatedAt
)

fun Collectible.toEntity(): CollectibleEntity = CollectibleEntity(
    id = id, name = name, category = category, type = type,
    ipName = ipName, seriesName = seriesName, characterTag = characterTag, remark = remark,
    purchaseChannel = purchaseChannel, purchaseShop = purchaseShop, purchaseDate = purchaseDate,
    purchasePrice = purchasePrice, purchaseQuantity = purchaseQuantity, purchaseShipping = purchaseShipping,
    expectedPrice = expectedPrice, sellPrice = sellPrice, sellQuantity = sellQuantity,
    sellShipping = sellShipping, isFreeShipping = isFreeShipping, sellDate = sellDate,
    buyerInfo = buyerInfo, sellRemark = sellRemark,
    status = status.name, storageStatus = storageStatus.name,
    imagePaths = imagePaths.joinToString(","),
    createdAt = createdAt, updatedAt = updatedAt
)
