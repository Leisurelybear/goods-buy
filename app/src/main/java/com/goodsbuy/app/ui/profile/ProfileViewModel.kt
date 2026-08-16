package com.goodsbuy.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.data.db.AppDatabase
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.backup.ImportMode
import com.goodsbuy.app.ui.backup.ImportPreviewResult
import com.goodsbuy.app.util.BackupManager
import com.goodsbuy.app.util.AppLogger
import com.goodsbuy.app.util.ImageUtils
import com.goodsbuy.app.ui.collectible.form.CollectibleDraftStore
import com.goodsbuy.app.ui.collectible.form.DraftSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CollectibleRepository,
    private val dao: CollectibleDao,
    private val database: AppDatabase,
    private val draftStore: CollectibleDraftStore
) : ViewModel() {
    private val _drafts = MutableStateFlow<List<DraftSummary>>(emptyList())
    val drafts: StateFlow<List<DraftSummary>> = _drafts

    init { refreshDrafts() }

    fun refreshDrafts() { _drafts.value = draftStore.list() }
    fun deleteDraft(key: String) {
        viewModelScope.launch {
            val draft = draftStore.load(key)
            val persistedPaths = draft?.id?.let { repository.getCollectibleById(it)?.imagePaths }.orEmpty().toSet()
            draft?.imagePaths?.filterNot(persistedPaths::contains)?.forEach { ImageUtils.deleteImage(context, it) }
            draftStore.delete(key)
            refreshDrafts()
        }
    }
    private val _importPreview = MutableStateFlow<ImportPreviewResult?>(null)
    val importPreview: StateFlow<ImportPreviewResult?> = _importPreview

    private val _importMode = MutableStateFlow(ImportMode.SKIP)
    val importMode: StateFlow<ImportMode> = _importMode
    fun setImportMode(mode: ImportMode) { _importMode.value = mode }

    private val _importedUri = MutableStateFlow<Uri?>(null)
    val importedUri: StateFlow<Uri?> = _importedUri

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress

    fun setImportedUri(uri: Uri?) {
        _importedUri.value = uri
    }

    fun clearPreview() {
        _importPreview.value = null
        _importedUri.value = null
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _importPreview.value = withContext(Dispatchers.IO) {
                BackupManager.previewImport(context, uri, dao, _importMode.value)
            }
        }
    }

    fun confirmImport(onSuccess: suspend (Int) -> Unit, onFailure: suspend (String) -> Unit) {
        viewModelScope.launch {
            val uri = _importedUri.value ?: run {
                onFailure("No file selected")
                return@launch
            }
            _isImporting.value = true
            _importProgress.value = 0f
            val result = withContext(Dispatchers.IO) {
                BackupManager.import(context, uri, database, _importMode.value) { current, total ->
                    _importProgress.value = if (total > 0) current.toFloat() / total else 0f
                }
            }
            _isImporting.value = false
            _importProgress.value = 0f
            clearPreview()
            result.fold(
                onSuccess = { count -> onSuccess(count) },
                onFailure = { e -> onFailure(e.message ?: "Import failed") }
            )
        }
    }

    fun exportBackup(outputUri: Uri, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val collectibles = repository.getAllCollectiblesOnce()
                val success = withContext(Dispatchers.IO) { BackupManager.export(context, collectibles, outputUri) }
                if (success) {
                    AppLogger.i("Export", "Done: count=${collectibles.size}")
                    onSuccess()
                } else {
                    AppLogger.e("Export", "Failed: BackupManager.export returned false")
                    onFailure("Export failed")
                }
            } catch (e: Exception) {
                AppLogger.e("Export", "Failed: ${e.message}", e)
                onFailure(e.message ?: "Export failed")
            }
        }
    }
}
