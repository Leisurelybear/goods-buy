package com.goodsbuy.app.ui.gallery

import android.content.Context
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeCollectibleRepository : CollectibleRepository {
        val allCollectibles = MutableStateFlow<List<Collectible>>(emptyList())
        var updated: Collectible? = null
        var inserted: Collectible? = null
        var deletedId: Long? = null

        override fun getAllCollectibles(): Flow<List<Collectible>> = allCollectibles
        override suspend fun getAllCollectiblesOnce(): List<Collectible> = allCollectibles.value
        override suspend fun getCollectibleById(id: Long): Collectible? =
            allCollectibles.value.firstOrNull { it.id == id }
        override fun getCollectiblesByStatus(status: String): Flow<List<Collectible>> =
            MutableStateFlow(allCollectibles.value.filter { it.status.name == status })
        override fun searchCollectibles(query: String): Flow<List<Collectible>> =
            MutableStateFlow(allCollectibles.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.ipName.contains(query, ignoreCase = true)
            })
        override suspend fun insertCollectible(collectible: Collectible): Long {
            inserted = collectible
            return collectible.id
        }
        override suspend fun updateCollectible(collectible: Collectible) {
            updated = collectible
        }
        override suspend fun deleteCollectible(id: Long) {
            deletedId = id
        }
        override fun getSoldCollectibles(): Flow<List<Collectible>> =
            MutableStateFlow(allCollectibles.value.filter { it.status == OrderStatus.SOLD })
    }

    private fun collectible(
        id: Long,
        name: String = "藏品$id",
        ipName: String = "",
        seriesName: String = "",
        status: OrderStatus = OrderStatus.OWNED,
        sellDate: Long? = null,
        imagePaths: List<String> = emptyList()
    ) = Collectible(
        id = id, name = name, category = "", type = "",
        ipName = ipName, seriesName = seriesName, characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = 0.0, purchaseQuantity = 1, purchaseShipping = 0.0,
        expectedPrice = 0.0, sellPrice = null, sellQuantity = null, sellShipping = null,
        isFreeShipping = false, sellDate = sellDate, buyerInfo = null, sellRemark = null,
        status = status, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = imagePaths, createdAt = id, updatedAt = id
    )

    private val repository = FakeCollectibleRepository()
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GalleryViewModel(repository, mockk<Context>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState groups by IP with counts and empty loading state`() = runTest(testDispatcher) {
        repository.allCollectibles.value = listOf(
            collectible(1, ipName = "原神"),
            collectible(2, ipName = "原神"),
            collectible(3, ipName = "蔚蓝档案"),
            collectible(4, ipName = "")
        )

        val state = viewModel.uiState.first()

        assertEquals(GroupBy.IP, state.groupBy)
        assertEquals(listOf("原神", "蔚蓝档案", "未分类"), state.groups.map { it.name })
        assertEquals(listOf(2, 1, 1), state.groups.map { it.count })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `setGroupBy switches dimension to SERIES`() = runTest(testDispatcher) {
        repository.allCollectibles.value = listOf(
            collectible(1, seriesName = "阿里乌斯"),
            collectible(2, seriesName = "阿里乌斯"),
            collectible(3, seriesName = "千年")
        )

        viewModel.setGroupBy(GroupBy.SERIES)
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals(GroupBy.SERIES, state.groupBy)
        assertEquals(listOf("阿里乌斯", "千年"), state.groups.map { it.name })
    }

    @Test
    fun `search filters by name ip series and character`() = runTest(testDispatcher) {
        repository.allCollectibles.value = listOf(
            collectible(1, name = "星野立牌", ipName = "蔚蓝档案"),
            collectible(2, name = "皮卡丘挂件", ipName = "宝可梦", seriesName = "春日系列"),
            collectible(3, name = "夏日徽章", ipName = "原神", seriesName = "海岛")
        )

        viewModel.setGroupBy(GroupBy.SERIES)
        viewModel.setSearchQuery("春日")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals(listOf("春日系列"), state.groups.map { it.name })
        assertEquals("春日", state.searchQuery)
    }

    @Test
    fun `quickUpdateStatus sets sellDate when sold`() = runTest(testDispatcher) {
        val item = collectible(1, status = OrderStatus.OWNED, sellDate = null)

        viewModel.quickUpdateStatus(item, OrderStatus.SOLD)
        advanceUntilIdle()

        val updated = repository.updated
        assertNotNull(updated)
        assertEquals(OrderStatus.SOLD, updated!!.status)
        assertNotNull(updated.sellDate)
    }

    @Test
    fun `quickUpdateStatus keeps existing sellDate for non-sold status`() = runTest(testDispatcher) {
        val item = collectible(1, status = OrderStatus.LISTED, sellDate = 123456L)

        viewModel.quickUpdateStatus(item, OrderStatus.LISTED)
        advanceUntilIdle()

        val updated = repository.updated
        assertNotNull(updated)
        assertEquals(123456L, updated!!.sellDate)
    }

    @Test
    fun `duplicateCollectible inserts a copy with fresh name and no image copy`() = runTest(testDispatcher) {
        val item = collectible(1, name = "星野", imagePaths = emptyList())

        viewModel.duplicateCollectible(item)
        advanceUntilIdle()

        val dup = repository.inserted
        assertNotNull(dup)
        assertEquals(0L, dup!!.id)
        assertTrue(dup.name.startsWith("星野"))
        assertTrue(dup.imagePaths.isEmpty())
        assertEquals(item.ipName, dup.ipName)
    }

    @Test
    fun `deleteCollectible deletes by id`() = runTest(testDispatcher) {
        val item = collectible(1, imagePaths = emptyList())

        viewModel.deleteCollectible(item)
        advanceUntilIdle()

        assertEquals(1L, repository.deletedId)
    }
}
