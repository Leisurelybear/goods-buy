package com.goodsbuy.app.util

import android.content.Context
import androidx.room.withTransaction
import com.goodsbuy.app.data.db.AppDatabase
import com.goodsbuy.app.data.mapper.toEntity
import com.goodsbuy.app.domain.model.Collectible
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PendingDeletion(val token: Long, val collectibles: List<Collectible>)

interface UndoDeleteController {
    val pending: StateFlow<PendingDeletion?>
    suspend fun delete(collectibles: List<Collectible>)
    suspend fun undo(): Boolean
}

@Singleton
class UndoDeleteManager @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : UndoDeleteController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _pending = MutableStateFlow<PendingDeletion?>(null)
    override val pending: StateFlow<PendingDeletion?> = _pending.asStateFlow()
    private var expirationJob: Job? = null

    override suspend fun delete(collectibles: List<Collectible>) {
        if (collectibles.isEmpty()) return
        finalizePending()
        database.withTransaction {
            collectibles.map { it.id }.chunked(500).forEach { ids ->
                database.collectibleDao().deleteCollectiblesByIds(ids)
            }
        }
        val pending = PendingDeletion(System.nanoTime(), collectibles)
        _pending.value = pending
        expirationJob = scope.launch {
            delay(UNDO_WINDOW_MS)
            if (_pending.value?.token == pending.token) finalizePending()
        }
    }

    override suspend fun undo(): Boolean {
        val pending = _pending.value ?: return false
        expirationJob?.cancel()
        return try {
            database.withTransaction {
                database.collectibleDao().insertCollectibles(pending.collectibles.map { it.toEntity() })
            }
            _pending.value = null
            true
        } catch (e: Exception) {
            AppLogger.e("Delete", "Undo failed: ${e.message}", e)
            expirationJob = scope.launch {
                delay(UNDO_WINDOW_MS)
                if (_pending.value?.token == pending.token) finalizePending()
            }
            false
        }
    }

    private suspend fun finalizePending() {
        expirationJob?.cancel()
        val pending = _pending.value ?: return
        pending.collectibles.flatMap { it.imagePaths }.forEach { ImageUtils.deleteImage(context, it) }
        _pending.value = null
    }

    private companion object { const val UNDO_WINDOW_MS = 10_000L }
}
