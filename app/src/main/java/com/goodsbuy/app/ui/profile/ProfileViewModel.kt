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
import com.goodsbuy.app.util.BackupStage
import com.goodsbuy.app.util.ExportStage
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
            val persisted = draft?.id?.let { repository.getCollectibleById(it)?.imagePaths }.orEmpty()
            if (draft != null) ImageUtils.deleteUnreferencedImages(draft.imagePaths, persisted)
            draftStore.delete(key)
            refreshDrafts()
        }
    }
    private val _importPreview = MutableStateFlow<ImportPreviewResult?>(null)
    val importPreview: StateFlow<ImportPreviewResult?> = _importPreview

    private val _importMode = MutableStateFlow(ImportMode.SKIP)
    val importMode: StateFlow<ImportMode> = _importMode
    fun setImportMode(mode: ImportMode) {
        _importMode.value = mode
        _importError.value = null
    }

    private val _importedUri = MutableStateFlow<Uri?>(null)
    val importedUri: StateFlow<Uri?> = _importedUri

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress

    private val _importStage = MutableStateFlow(BackupStage.READING)
    val importStage: StateFlow<BackupStage> = _importStage

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    fun setImportedUri(uri: Uri?) {
        _importedUri.value = uri
    }

    fun clearPreview() {
        _importPreview.value = null
        _importedUri.value = null
        _importError.value = null
        _importProgress.value = 0f
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _importPreview.value = withContext(Dispatchers.IO) {
                BackupManager.previewImport(context, uri, dao, _importMode.value)
            }
        }
    }

    /**
     * Runs the import. On failure the preview is kept (with [importError] set) so
     * the user can retry without having to pick the file again.
     */
    fun confirmImport(onSuccess: suspend (Int) -> Unit) {
        viewModelScope.launch {
            val uri = _importedUri.value ?: run {
                _importError.value = "未选择备份文件"
                return@launch
            }
            _isImporting.value = true
            _importProgress.value = 0f
            _importError.value = null
            _importStage.value = BackupStage.READING
            val result = withContext(Dispatchers.IO) {
                BackupManager.import(context, uri, database, _importMode.value) { stage, current, total ->
                    _importStage.value = stage
                    _importProgress.value = if (total > 0) current.toFloat() / total else 0f
                }
            }
            _isImporting.value = false
            result.fold(
                onSuccess = { count ->
                    clearPreview()
                    onSuccess(count)
                },
                onFailure = { e -> _importError.value = e.message ?: "导入失败" }
            )
        }
    }

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress

    private val _exportStage = MutableStateFlow(ExportStage.PREPARING)
    val exportStage: StateFlow<ExportStage> = _exportStage

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError

    /** Target of the last export attempt, kept so a failure can be retried. */
    private var lastExportUri: Uri? = null

    /**
     * Runs the export. On failure the target URI is kept and [exportError] is set
     * so the user can retry without going through the file picker again.
     */
    fun exportBackup(outputUri: Uri, onSuccess: (Int) -> Unit) {
        lastExportUri = outputUri
        runExport(outputUri, onSuccess)
    }

    fun retryExport(onSuccess: (Int) -> Unit) {
        val uri = lastExportUri
        if (uri == null) {
            _exportError.value = "未选择导出位置"
            return
        }
        runExport(uri, onSuccess)
    }

    fun dismissExport() {
        if (_isExporting.value) return
        _exportError.value = null
        _exportProgress.value = 0f
        lastExportUri = null
    }

    private fun runExport(outputUri: Uri, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportStage.value = ExportStage.PREPARING
            _exportError.value = null
            val result = withContext(Dispatchers.IO) {
                val collectibles = repository.getAllCollectiblesOnce()
                BackupManager.export(context, collectibles, outputUri) { stage, current, total ->
                    _exportStage.value = stage
                    _exportProgress.value = if (total > 0) current.toFloat() / total else 0f
                }.map { collectibles.size }
            }
            _isExporting.value = false
            result.fold(
                onSuccess = { count ->
                    _exportProgress.value = 1f
                    lastExportUri = null
                    AppLogger.i("Export", "Done: count=$count")
                    onSuccess(count)
                },
                onFailure = { e ->
                    AppLogger.e("Export", "Failed: ${e.message}", e)
                    _exportError.value = e.message ?: "导出失败"
                }
            )
        }
    }
}
