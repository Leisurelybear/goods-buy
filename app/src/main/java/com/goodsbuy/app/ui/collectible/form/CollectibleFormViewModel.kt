package com.goodsbuy.app.ui.collectible.form

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.domain.validation.CollectibleInput
import com.goodsbuy.app.domain.validation.CollectibleValidator
import com.goodsbuy.app.util.ImageUtils
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CollectibleFormViewModel @Inject constructor(
    private val repository: CollectibleRepository,
    @ApplicationContext private val context: Context,
    private val draftStore: CollectibleDraftStore,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectibleFormUiState())
    val uiState: StateFlow<CollectibleFormUiState> = _uiState.asStateFlow()

    private var initializedKey: String? = null
    private var pendingDraft: CollectibleFormUiState? = null
    private var originalImagePaths: List<String> = emptyList()
    private var draftSaveJob: Job? = null
    private var editingTargetExists = true
    private var hasUnsavedDraftChanges = false

    fun initialize(id: Long?) {
        val key = draftKey(id)
        if (initializedKey == key) return
        initializedKey = key
        if (id == null) {
            prepareDraft(key)
            return
        }
        viewModelScope.launch {
            val collectible = repository.getCollectibleById(id)
            if (collectible == null) {
                editingTargetExists = false
                prepareDraft(key)
                return@launch
            }
            originalImagePaths = collectible.imagePaths
            _uiState.update {
                it.copy(
                    id = collectible.id, name = collectible.name, category = collectible.category,
                    type = collectible.type, ipName = collectible.ipName, seriesName = collectible.seriesName,
                    characterTag = collectible.characterTag, remark = collectible.remark,
                    purchaseChannel = collectible.purchaseChannel, purchaseShop = collectible.purchaseShop,
                    purchaseDate = collectible.purchaseDate, purchasePrice = collectible.purchasePrice.toString(),
                    purchaseQuantity = collectible.purchaseQuantity.toString(), purchaseShipping = collectible.purchaseShipping.toString(),
                    expectedPrice = collectible.expectedPrice.toString(), sellPrice = collectible.sellPrice?.toString() ?: "",
                    sellQuantity = collectible.sellQuantity?.toString() ?: "", sellShipping = collectible.sellShipping?.toString() ?: "",
                    isFreeShipping = collectible.isFreeShipping, sellDate = collectible.sellDate,
                    buyerInfo = collectible.buyerInfo ?: "", sellRemark = collectible.sellRemark ?: "",
                    status = collectible.status, storageStatus = collectible.storageStatus,
                    imagePaths = collectible.imagePaths, createdAt = collectible.createdAt
                )
            }
            prepareDraft(key)
        }
    }

    fun loadCollectible(id: Long) = initialize(id)

    private fun prepareDraft(key: String) {
        pendingDraft = draftStore.load(key)
        if (pendingDraft != null) _uiState.update { it.copy(hasDraft = true) }
    }

    fun restoreDraft() {
        val draft = pendingDraft ?: return
        _uiState.value = draft.copy(
            id = draft.id.takeIf { editingTargetExists },
            hasDraft = false, fieldErrors = emptyMap(), isSaving = false, isSaved = false, saveError = null
        )
        pendingDraft = null
    }

    fun discardDraft() {
        val draft = pendingDraft ?: return
        val keepPaths = originalImagePaths.toSet()
        draft.imagePaths.filterNot(keepPaths::contains).forEach { ImageUtils.deleteImageWithCompanions(context, it) }
        initializedKey?.let(draftStore::delete)
        draftSaveJob?.cancel()
        hasUnsavedDraftChanges = false
        pendingDraft = null
        _uiState.update { it.copy(hasDraft = false) }
    }

    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            val updated = when (field) {
                "name" -> state.copy(name = value)
                "category" -> state.copy(category = value)
                "type" -> state.copy(type = value)
                "ipName" -> state.copy(ipName = value)
                "seriesName" -> state.copy(seriesName = value)
                "characterTag" -> state.copy(characterTag = value)
                "remark" -> state.copy(remark = value)
                "purchaseChannel" -> state.copy(purchaseChannel = value)
                "purchaseShop" -> state.copy(purchaseShop = value)
                "purchasePrice" -> state.copy(purchasePrice = value)
                "purchaseQuantity" -> state.copy(purchaseQuantity = value)
                "purchaseShipping" -> state.copy(purchaseShipping = value)
                "expectedPrice" -> state.copy(expectedPrice = value)
                "sellPrice" -> state.copy(sellPrice = value)
                "sellQuantity" -> state.copy(sellQuantity = value)
                "sellShipping" -> state.copy(sellShipping = value)
                "buyerInfo" -> state.copy(buyerInfo = value)
                "sellRemark" -> state.copy(sellRemark = value)
                else -> state
            }
            updated.copy(fieldErrors = updated.fieldErrors - field)
        }
        scheduleDraftSave()
    }

    fun updateStatus(status: OrderStatus) {
        _uiState.update { it.copy(status = status, fieldErrors = it.fieldErrors - "sellPrice" - "sellQuantity") }
        scheduleDraftSave()
    }

    fun updateStorageStatus(status: StorageStatus) { _uiState.update { it.copy(storageStatus = status) }; scheduleDraftSave() }
    fun updateFreeShipping(free: Boolean) { _uiState.update { it.copy(isFreeShipping = free) }; scheduleDraftSave() }

    fun addImages(uris: List<Uri>) {
        val remainingSlots = (MAX_IMAGE_COUNT - _uiState.value.imagePaths.size).coerceAtLeast(0)
        if (remainingSlots == 0 || uris.isEmpty()) return
        viewModelScope.launch {
            val newPaths = withContext(Dispatchers.IO) {
                uris.distinct().take(remainingSlots).mapNotNull { ImageUtils.copyImageToInternalStorage(context, it) }
            }
            if (newPaths.isNotEmpty()) {
                _uiState.update { state -> state.copy(imagePaths = (state.imagePaths + newPaths).take(MAX_IMAGE_COUNT)) }
                scheduleDraftSave()
            }
        }
    }

    fun replaceImagePath(index: Int, newPath: String) {
        if (index !in _uiState.value.imagePaths.indices) return
        _uiState.update { state ->
            state.copy(imagePaths = state.imagePaths.mapIndexed { i, path -> if (i == index) newPath else path })
        }
        scheduleDraftSave()
    }

    fun removeImagePath(index: Int) {
        val path = _uiState.value.imagePaths.getOrNull(index) ?: return
        if (path !in originalImagePaths) ImageUtils.deleteImageWithCompanions(context, path)
        _uiState.update { state -> state.copy(imagePaths = state.imagePaths.filterIndexed { i, _ -> i != index }) }
        scheduleDraftSave()
    }

    fun save() {
        if (_uiState.value.isSaving || _uiState.value.isSaved) return
        val state = _uiState.value
        val validation = CollectibleValidator.validate(
            CollectibleInput(
                name = state.name, purchasePrice = state.purchasePrice, purchaseQuantity = state.purchaseQuantity,
                purchaseShipping = state.purchaseShipping, expectedPrice = state.expectedPrice, sellPrice = state.sellPrice,
                sellQuantity = state.sellQuantity, sellShipping = state.sellShipping, status = state.status
            )
        )
        if (!validation.isValid) {
            _uiState.update { it.copy(fieldErrors = validation.errors, saveError = "请修正表单中的错误") }
            return
        }
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val sellDate = when {
                    state.status == OrderStatus.SOLD && state.sellDate == null -> System.currentTimeMillis()
                    state.status != OrderStatus.SOLD -> null
                    else -> state.sellDate
                }
                val collectible = Collectible(
                    id = state.id ?: 0, name = state.name.trim(), category = state.category.trim(), type = state.type.trim(),
                    ipName = state.ipName.trim(), seriesName = state.seriesName.trim(), characterTag = state.characterTag.trim(),
                    remark = state.remark, purchaseChannel = state.purchaseChannel, purchaseShop = state.purchaseShop,
                    purchaseDate = state.purchaseDate, purchasePrice = state.purchasePrice.toDouble(), purchaseQuantity = state.purchaseQuantity.toInt(),
                    purchaseShipping = state.purchaseShipping.toDouble(), expectedPrice = state.expectedPrice.toDoubleOrNull() ?: 0.0,
                    sellPrice = state.sellPrice.toDoubleOrNull(), sellQuantity = state.sellQuantity.toIntOrNull(),
                    sellShipping = state.sellShipping.toDoubleOrNull(), isFreeShipping = state.isFreeShipping,
                    sellDate = sellDate, buyerInfo = state.buyerInfo.ifBlank { null }, sellRemark = state.sellRemark.ifBlank { null },
                    status = state.status, storageStatus = state.storageStatus, imagePaths = state.imagePaths,
                    createdAt = state.createdAt, updatedAt = System.currentTimeMillis()
                )
                if (state.id != null) repository.updateCollectible(collectible) else repository.insertCollectible(collectible)
                val currentBases = state.imagePaths.map { ImageUtils.baseOfImage(it) }.toSet()
                originalImagePaths.forEach { origPath ->
                    if (state.imagePaths.contains(origPath)) return@forEach
                    if (ImageUtils.baseOfImage(origPath) in currentBases) {
                        ImageUtils.deleteImage(context, origPath)
                    } else {
                        ImageUtils.deleteImageWithCompanions(context, origPath)
                    }
                }
                draftSaveJob?.cancel()
                hasUnsavedDraftChanges = false
                initializedKey?.let(draftStore::delete)
                pendingDraft = null
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "保存失败，请重试") }
            }
        }
    }

    fun clearSaveError() { _uiState.update { it.copy(saveError = null) } }

    private fun scheduleDraftSave() {
        val key = initializedKey ?: return
        if (_uiState.value.isSaved) return
        draftSaveJob?.cancel()
        hasUnsavedDraftChanges = true
        draftSaveJob = viewModelScope.launch {
            delay(preferencesRepository.draftAutoSaveDelayMillis)
            if (!_uiState.value.isSaved) {
                draftStore.save(key, _uiState.value)
                hasUnsavedDraftChanges = false
                _uiState.update { it.copy(draftSavedAt = System.currentTimeMillis()) }
            }
        }
    }

    override fun onCleared() {
        draftSaveJob?.cancel()
        val key = initializedKey
        if (key != null && hasUnsavedDraftChanges && !_uiState.value.isSaved) {
            draftStore.save(key, _uiState.value)
        }
        super.onCleared()
    }

    private fun draftKey(id: Long?): String = if (id == null) "new" else "edit_$id"
    private companion object { const val MAX_IMAGE_COUNT = 9 }
}
