package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCollectibleStatusUseCaseTest {

    private class FakeRepository : CollectibleRepository {
        var updated: Collectible? = null
        override fun getAllCollectibles(): Flow<List<Collectible>> = MutableStateFlow(emptyList())
        override suspend fun getAllCollectiblesOnce(): List<Collectible> = emptyList()
        override suspend fun getCollectibleById(id: Long): Collectible? = null
        override fun observeCollectibles(query: String, status: String?): Flow<List<Collectible>> =
            MutableStateFlow(emptyList())
        override suspend fun insertCollectible(collectible: Collectible): Long = collectible.id
        override suspend fun updateCollectible(collectible: Collectible) { updated = collectible }
        override suspend fun deleteCollectible(id: Long) {}
        override fun getSoldCollectibles(): Flow<List<Collectible>> = MutableStateFlow(emptyList())
    }

    private val repository = FakeRepository()
    private val useCase = UpdateCollectibleStatusUseCase(repository)

    @Test
    fun `entering sold without a date stamps now and refreshes updatedAt`() {
        val collectible = collectible(status = OrderStatus.OWNED, sellDate = null, updatedAt = 100L)

        val migrated = useCase.migrate(collectible, OrderStatus.SOLD, now = 999L)

        assertEquals(OrderStatus.SOLD, migrated.status)
        assertEquals(999L, migrated.sellDate)
        assertEquals(999L, migrated.updatedAt)
    }

    @Test
    fun `staying sold preserves the original sale date`() {
        val collectible = collectible(status = OrderStatus.SOLD, sellDate = 500L, updatedAt = 100L)

        val migrated = useCase.migrate(collectible, OrderStatus.SOLD, now = 999L)

        assertEquals(500L, migrated.sellDate)
        assertEquals(999L, migrated.updatedAt)
    }

    @Test
    fun `leaving sold clears the sale date so a later sale is stamped fresh`() {
        val collectible = collectible(status = OrderStatus.SOLD, sellDate = 500L, updatedAt = 100L)

        val migrated = useCase.migrate(collectible, OrderStatus.LISTED, now = 999L)

        assertEquals(OrderStatus.LISTED, migrated.status)
        assertNull(migrated.sellDate)
        assertEquals(999L, migrated.updatedAt)
    }

    @Test
    fun `invoke persists the migrated collectible`() = runTest {
        val collectible = collectible(status = OrderStatus.OWNED, sellDate = null, updatedAt = 100L)

        val returned = useCase(collectible, OrderStatus.SOLD)

        assertEquals(OrderStatus.SOLD, repository.updated?.status)
        assertEquals(returned.sellDate, repository.updated?.sellDate)
    }

    private fun collectible(status: OrderStatus, sellDate: Long?, updatedAt: Long) = Collectible(
        id = 1, name = "测试藏品", category = "", type = "",
        ipName = "", seriesName = "", characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = 10.0, purchaseQuantity = 1, purchaseShipping = 0.0,
        expectedPrice = 0.0, sellPrice = null, sellQuantity = null, sellShipping = null,
        isFreeShipping = false, sellDate = sellDate, buyerInfo = null, sellRemark = null,
        status = status, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = 0, updatedAt = updatedAt
    )
}
