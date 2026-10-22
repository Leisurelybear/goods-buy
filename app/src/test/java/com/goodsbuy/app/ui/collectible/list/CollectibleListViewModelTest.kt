package com.goodsbuy.app.ui.collectible.list

import android.content.Context
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.domain.usecase.GetDashboardSummaryUseCase
import com.goodsbuy.app.domain.usecase.UpdateCollectibleStatusUseCase
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.util.AppLogger
import com.goodsbuy.app.util.UndoDeleteManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectibleListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeCollectibleRepository : CollectibleRepository {
        val all = MutableStateFlow<List<Collectible>>(emptyList())
        var lastQuery: String? = null
        var lastStatus: String? = null
        var updated: Collectible? = null

        override fun getAllCollectibles(): Flow<List<Collectible>> = all
        override suspend fun getAllCollectiblesOnce(): List<Collectible> = all.value
        override suspend fun getCollectibleById(id: Long): Collectible? =
            all.value.firstOrNull { it.id == id }

        override fun observeCollectibles(query: String, status: String?): Flow<List<Collectible>> {
            lastQuery = query
            lastStatus = status
            return MutableStateFlow(all.value.filter { collectible ->
                (status == null || collectible.status.name == status) &&
                    (query.isBlank() || collectible.name.contains(query, ignoreCase = true))
            })
        }

        override suspend fun insertCollectible(collectible: Collectible): Long = collectible.id
        override suspend fun updateCollectible(collectible: Collectible) { updated = collectible }
        override suspend fun deleteCollectible(id: Long) {}
        override fun getSoldCollectibles(): Flow<List<Collectible>> = MutableStateFlow(emptyList())
    }

    private val repository = FakeCollectibleRepository()
    private lateinit var viewModel: CollectibleListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // AppLogger writes through android.util.Log, which is not available in JVM tests.
        mockkObject(AppLogger)
        every { AppLogger.i(any(), any()) } returns Unit
        val preferences = mockk<PreferencesRepository>(relaxed = true)
        every { preferences.sortField } returns "CREATED_AT"
        every { preferences.sortAscending } returns false
        viewModel = CollectibleListViewModel(
            repository = repository,
            preferencesRepository = preferences,
            undoDeleteManager = mockk<UndoDeleteManager>(relaxed = true),
            getDashboardSummary = GetDashboardSummaryUseCase(),
            updateStatusUseCase = UpdateCollectibleStatusUseCase(repository),
            context = mockk<Context>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `search and status filter are applied together`() = runTest(testDispatcher) {
        repository.all.value = listOf(
            collectible(1, name = "初音挂件", status = OrderStatus.SOLD),
            collectible(2, name = "初音立牌", status = OrderStatus.OWNED),
            collectible(3, name = "其他藏品", status = OrderStatus.SOLD)
        )

        viewModel.onSearchQueryChange("初音")
        viewModel.onStatusFilterChange(OrderStatus.SOLD.name)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf(1L), state.collectibles.map { it.id })
        assertEquals("初音", repository.lastQuery)
        assertEquals(OrderStatus.SOLD.name, repository.lastStatus)
    }

    @Test
    fun `clearing the status filter keeps the search term active`() = runTest(testDispatcher) {
        repository.all.value = listOf(
            collectible(1, name = "初音挂件", status = OrderStatus.SOLD),
            collectible(2, name = "初音立牌", status = OrderStatus.OWNED)
        )

        viewModel.onSearchQueryChange("初音")
        viewModel.onStatusFilterChange(OrderStatus.SOLD.name)
        viewModel.uiState.first { !it.isLoading }

        viewModel.onStatusFilterChange(null)
        val state = viewModel.uiState.first { !it.isLoading && it.selectedStatusFilter == null }

        assertEquals(listOf(1L, 2L), state.collectibles.map { it.id }.sorted())
        assertNull(repository.lastStatus)
    }

    @Test
    fun `quick status update clears the sale date when leaving sold`() = runTest(testDispatcher) {
        val sold = collectible(1, name = "初音挂件", status = OrderStatus.SOLD, sellDate = 123456L)

        viewModel.quickUpdateStatus(sold, OrderStatus.LISTED)
        advanceUntilIdle()

        assertEquals(OrderStatus.LISTED, repository.updated?.status)
        assertNull(repository.updated?.sellDate)
    }

    private fun collectible(
        id: Long,
        name: String,
        status: OrderStatus,
        sellDate: Long? = null
    ) = Collectible(
        id = id, name = name, category = "", type = "",
        ipName = "", seriesName = "", characterTag = "", remark = "",
        purchaseChannel = "", purchaseShop = "", purchaseDate = 0,
        purchasePrice = 0.0, purchaseQuantity = 1, purchaseShipping = 0.0,
        expectedPrice = 0.0, sellPrice = null, sellQuantity = null, sellShipping = null,
        isFreeShipping = false, sellDate = sellDate, buyerInfo = null, sellRemark = null,
        status = status, storageStatus = StorageStatus.IN_STOCK,
        imagePaths = emptyList(), createdAt = id, updatedAt = id
    )
}
