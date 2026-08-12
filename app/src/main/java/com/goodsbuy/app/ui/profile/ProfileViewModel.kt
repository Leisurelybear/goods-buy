package com.goodsbuy.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.backup.ImportPreviewResult
import com.goodsbuy.app.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val context: Context,
    private val repository: CollectibleRepository,
    private val dao: CollectibleDao
) : ViewModel() {
    private val _importPreview = MutableStateFlow<ImportPreviewResult?>(null)
    val importPreview: StateFlow<ImportPreviewResult?> = _importPreview

    private val _forceImportDuplicates = MutableStateFlow(false)
    val forceImportDuplicates: StateFlow<Boolean> = _forceImportDuplicates

    private val _importedUri = MutableStateFlow<Uri?>(null)
    val importedUri: StateFlow<Uri?> = _importedUri

    fun setForceImportDuplicates(force: Boolean) {
        _forceImportDuplicates.value = force
    }

    fun setImportedUri(uri: Uri?) {
        _importedUri.value = uri
    }

    fun clearPreview() {
        _importPreview.value = null
        _importedUri.value = null
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _importPreview.value = BackupManager.previewImport(context, uri, dao, _forceImportDuplicates.value)
        }
    }

    fun confirmImport(onSuccess: (Int) -> Unit, onFailure: (String) -> Unit) {
        val uri = _importedUri.value ?: run {
            onFailure("No file selected")
            return
        }
        viewModelScope.launch {
            val result = BackupManager.import(context, uri, dao, _forceImportDuplicates.value)
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
                val collectibles = repository.getAllCollectibles().first()
                val success = BackupManager.export(context, collectibles, outputUri)
                if (success) onSuccess() else onFailure("Export failed")
            } catch (e: Exception) {
                onFailure(e.message ?: "Export failed")
            }
        }
    }
}
