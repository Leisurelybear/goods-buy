package com.goodsbuy.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Indices cover the columns that actually appear in SQL predicates and sorts:
 * - `status` is an equality filter in the combined search query;
 * - `createdAt` backs `ORDER BY createdAt DESC` on every list query;
 * - `sellDate` backs the `ORDER BY sellDate DESC` used for sold records.
 *
 * Name/IP/series LIKE searches are intentionally not indexed: SQLite cannot use
 * a B-tree index for a leading-wildcard `LIKE '%x%'` pattern. That is what the
 * planned FTS5 table is for.
 */
@Entity(
    tableName = "collectibles",
    indices = [
        Index(value = ["status"], name = "index_collectibles_status"),
        Index(value = ["createdAt"], name = "index_collectibles_createdAt"),
        Index(value = ["sellDate"], name = "index_collectibles_sellDate")
    ]
)
data class CollectibleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val category: String, val type: String,
    val ipName: String, val seriesName: String, val characterTag: String, val remark: String,
    val purchaseChannel: String, val purchaseShop: String, val purchaseDate: Long,
    val purchasePrice: Double, val purchaseQuantity: Int, val purchaseShipping: Double,
    val expectedPrice: Double, val sellPrice: Double?, val sellQuantity: Int?,
    val sellShipping: Double?, val isFreeShipping: Boolean = false,
    val sellDate: Long?, val buyerInfo: String?, val sellRemark: String?,
    val status: String, val storageStatus: String, val imagePaths: String,
    val createdAt: Long, val updatedAt: Long
)
