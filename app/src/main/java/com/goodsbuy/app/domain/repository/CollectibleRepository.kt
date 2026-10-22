package com.goodsbuy.app.domain.repository

import com.goodsbuy.app.domain.model.Collectible
import kotlinx.coroutines.flow.Flow

interface CollectibleRepository {
    fun getAllCollectibles(): Flow<List<Collectible>>
    suspend fun getAllCollectiblesOnce(): List<Collectible>
    suspend fun getCollectibleById(id: Long): Collectible?

    /**
     * Observes collectibles matching [query] (blank = any) and [status]
     * (null = any). Both filters are applied together.
     */
    fun observeCollectibles(query: String, status: String?): Flow<List<Collectible>>

    suspend fun insertCollectible(collectible: Collectible): Long
    suspend fun updateCollectible(collectible: Collectible)
    suspend fun deleteCollectible(id: Long)
    fun getSoldCollectibles(): Flow<List<Collectible>>
}
