package com.graincabinet.app.domain.repository

import com.graincabinet.app.domain.model.Collectible
import kotlinx.coroutines.flow.Flow

interface CollectibleRepository {
    fun getAllCollectibles(): Flow<List<Collectible>>
    suspend fun getCollectibleById(id: Long): Collectible?
    fun getCollectiblesByStatus(status: String): Flow<List<Collectible>>
    fun searchCollectibles(query: String): Flow<List<Collectible>>
    suspend fun insertCollectible(collectible: Collectible): Long
    suspend fun updateCollectible(collectible: Collectible)
    suspend fun deleteCollectible(id: Long)
    fun getSoldCollectibles(): Flow<List<Collectible>>
}
