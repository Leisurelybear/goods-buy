package com.graincabinet.app.data.repository

import com.graincabinet.app.data.db.CollectibleDao
import com.graincabinet.app.data.mapper.toDomain
import com.graincabinet.app.data.mapper.toEntity
import com.graincabinet.app.domain.model.Collectible
import com.graincabinet.app.domain.repository.CollectibleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectibleRepositoryImpl @Inject constructor(
    private val collectibleDao: CollectibleDao
) : CollectibleRepository {

    override fun getAllCollectibles(): Flow<List<Collectible>> =
        collectibleDao.getAllCollectibles().map { list -> list.map { it.toDomain() } }

    override suspend fun getCollectibleById(id: Long): Collectible? =
        collectibleDao.getCollectibleById(id)?.toDomain()

    override fun getCollectiblesByStatus(status: String): Flow<List<Collectible>> =
        collectibleDao.getCollectiblesByStatus(status).map { list -> list.map { it.toDomain() } }

    override fun searchCollectibles(query: String): Flow<List<Collectible>> =
        collectibleDao.searchCollectibles(query).map { list -> list.map { it.toDomain() } }

    override suspend fun insertCollectible(collectible: Collectible): Long =
        collectibleDao.insertCollectible(collectible.toEntity())

    override suspend fun updateCollectible(collectible: Collectible) =
        collectibleDao.updateCollectible(collectible.toEntity())

    override suspend fun deleteCollectible(id: Long) =
        collectibleDao.deleteCollectibleById(id)

    override fun getSoldCollectibles(): Flow<List<Collectible>> =
        collectibleDao.getSoldCollectibles().map { list -> list.map { it.toDomain() } }
}
