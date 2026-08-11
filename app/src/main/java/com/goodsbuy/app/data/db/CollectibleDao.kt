package com.goodsbuy.app.data.db

import androidx.room.*
import com.goodsbuy.app.data.entity.CollectibleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectibleDao {
    @Query("SELECT * FROM collectibles ORDER BY createdAt DESC")
    fun getAllCollectibles(): Flow<List<CollectibleEntity>>

    @Query("SELECT * FROM collectibles WHERE id = :id")
    suspend fun getCollectibleById(id: Long): CollectibleEntity?

    @Query("SELECT * FROM collectibles WHERE status = :status ORDER BY createdAt DESC")
    fun getCollectiblesByStatus(status: String): Flow<List<CollectibleEntity>>

    @Query("SELECT * FROM collectibles WHERE name LIKE \'%\' || :query || \'%\' OR seriesName LIKE \'%\' || :query || \'%\' OR ipName LIKE \'%\' || :query || \'%\' OR characterTag LIKE \'%\' || :query || \'%\' OR purchaseShop LIKE \'%\' || :query || \'%\' ORDER BY createdAt DESC")
    fun searchCollectibles(query: String): Flow<List<CollectibleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectible(collectible: CollectibleEntity): Long

    @Update
    suspend fun updateCollectible(collectible: CollectibleEntity)

    @Delete
    suspend fun deleteCollectible(collectible: CollectibleEntity)

    @Query("DELETE FROM collectibles WHERE id = :id")
    suspend fun deleteCollectibleById(id: Long)

    @Query("SELECT * FROM collectibles WHERE status = \'SOLD\' ORDER BY sellDate DESC")
    fun getSoldCollectibles(): Flow<List<CollectibleEntity>>
}
