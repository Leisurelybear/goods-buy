package com.goodsbuy.app.ui.collectible.form

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodsbuy.app.util.EdgeFadeMask
import com.goodsbuy.app.util.FadeShape
import com.goodsbuy.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EdgeFadeEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EdgeFadeEditUiState())
    val uiState: StateFlow<EdgeFadeEditUiState> = _uiState.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _done = MutableSharedFlow<String>()
    val done: SharedFlow<String> = _done.asSharedFlow()

    private var previewJob: Job? = null

    fun initialize(sourcePath: String) {
        if (_uiState.value.sourcePath == sourcePath) return
        _uiState.value = EdgeFadeEditUiState(sourcePath = sourcePath)
        schedulePreview()
    }

    fun onShapeChange(shape: FadeShape) {
        _uiState.update { it.copy(shape = shape) }
        schedulePreview()
    }

    fun onIntensityChange(intensity: Float) {
        _uiState.update { it.copy(intensity = intensity.coerceIn(0f, 1f)) }
        schedulePreview()
    }

    fun confirm() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val s = _uiState.value
                if (s.intensity <= 0f) {
                    ImageUtils.resetEdgeFade(context, s.sourcePath)
                } else {
                    ImageUtils.applyEdgeFade(context, s.sourcePath, s.shape, s.intensity)
                }
            }
            _uiState.update { it.copy(isProcessing = false) }
            result?.let {
                _uiState.update { state -> state.copy(resultPath = it) }
                _done.emit(it)
            }
        }
    }

    fun reset() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ImageUtils.resetEdgeFade(context, _uiState.value.sourcePath)
            }
            _uiState.update { it.copy(isProcessing = false, intensity = 0f) }
            result?.let {
                _uiState.update { state -> state.copy(resultPath = it) }
                _done.emit(it)
            }
            schedulePreview()
        }
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(80)
            val s = _uiState.value
            if (s.sourcePath.isEmpty()) return@launch
            _uiState.update { it.copy(isPreviewLoading = true) }
            val bitmap = withContext(Dispatchers.IO) {
                val base = ImageUtils.baseOfImage(s.sourcePath)
                val source = if (File(ImageUtils.origBackupPath(base)).exists()) {
                    ImageUtils.origBackupPath(base)
                } else {
                    s.sourcePath
                }
                val src = ImageUtils.decodeDownscaled(source, PREVIEW_DIMENSION) ?: return@withContext null
                if (s.intensity <= 0f) {
                    src
                } else {
                    val w = src.width
                    val h = src.height
                    val pixels = IntArray(w * h)
                    src.getPixels(pixels, 0, w, 0, 0, w, h)
                    val alpha = EdgeFadeMask.alphaMask(w, h, s.shape, s.intensity)
                    val out = EdgeFadeMask.applyAlpha(pixels, alpha)
                    val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    outBmp.setPixels(out, 0, w, 0, 0, w, h)
                    src.recycle()
                    outBmp
                }
            }
            if (bitmap != null) {
                _previewBitmap.value?.recycle()
                _previewBitmap.value = bitmap
            }
            _uiState.update { it.copy(isPreviewLoading = false) }
        }
    }

    private companion object { const val PREVIEW_DIMENSION = 640 }
}