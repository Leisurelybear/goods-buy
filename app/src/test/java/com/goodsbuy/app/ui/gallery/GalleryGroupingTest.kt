package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryGroupingTest {

    private fun collectible(
        id: Long,
        ipName: String = "",
        seriesName: String = "",
        createdAt: Long = id
    ) = Collectible(
        id = id, name = "藏品$id", category = "", type = "",
        ipName = ipName, seriesName = seriesName, characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = 0.0, purchaseQuantity = 1, purchaseShipping = 0.0,
        expectedPrice = 0.0, sellPrice = null, sellQuantity = null, sellShipping = null,
        isFreeShipping = false, sellDate = null, buyerInfo = null, sellRemark = null,
        status = OrderStatus.OWNED, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = createdAt, updatedAt = createdAt
    )

    @Test
    fun `groups by IP with correct counts`() {
        val list = listOf(
            collectible(1, ipName = "原神"),
            collectible(2, ipName = "原神"),
            collectible(3, ipName = "蔚蓝档案"),
            collectible(4, ipName = "")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("原神", "蔚蓝档案", "未分类"), groups.map { it.name })
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
    }

    @Test
    fun `groups by series with correct counts`() {
        val list = listOf(
            collectible(1, seriesName = "阿里乌斯"),
            collectible(2, seriesName = "阿里乌斯"),
            collectible(3, seriesName = "千年"),
            collectible(4, seriesName = "")
        )
        val groups = groupCollectibles(list, GroupBy.SERIES)
        assertEquals(listOf("阿里乌斯", "千年", "未分类"), groups.map { it.name })
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
    }

    @Test
    fun `orders groups by count descending and uncategorized last`() {
        val list = listOf(
            collectible(1, ipName = "A"), collectible(2, ipName = "A"), collectible(3, ipName = "A"),
            collectible(4, ipName = "B"),
            collectible(5, ipName = ""), collectible(6, ipName = "  ")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("A", "B", "未分类"), groups.map { it.name })
        assertEquals(listOf(3, 1, 2), groups.map { it.count })
    }

    @Test
    fun `returns empty groups for empty list`() {
        assertEquals(emptyList<GalleryGroup>(), groupCollectibles(emptyList(), GroupBy.IP))
        assertEquals(emptyList<GalleryGroup>(), groupCollectibles(emptyList(), GroupBy.SERIES))
    }

    @Test
    fun `blank keys are treated as uncategorized`() {
        val list = listOf(
            collectible(1, ipName = " "),
            collectible(2, ipName = "原神")
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf("原神", "未分类"), groups.map { it.name })
        assertEquals(1, groups[1].count)
    }

    @Test
    fun `items within group sorted by series then createdAt desc`() {
        val list = listOf(
            collectible(1, ipName = "IP1", seriesName = "S2", createdAt = 100),
            collectible(2, ipName = "IP1", seriesName = "S1", createdAt = 200),
            collectible(3, ipName = "IP1", seriesName = "S1", createdAt = 300)
        )
        val groups = groupCollectibles(list, GroupBy.IP)
        assertEquals(listOf(3L, 2L, 1L), groups.single().collectibles.map { it.id })
    }
}
