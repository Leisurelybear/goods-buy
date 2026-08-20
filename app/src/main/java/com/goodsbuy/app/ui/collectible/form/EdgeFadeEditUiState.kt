package com.goodsbuy.app.ui.collectible.form

import com.goodsbuy.app.util.FadeShape

data class EdgeFadeEditUiState(
    val sourcePath: String = "",
    val resultPath: String = "",
    val shape: FadeShape = FadeShape.ELLIPSE,
    val intensity: Float = 0f,
    val isProcessing: Boolean = false,
    val isPreviewLoading: Boolean = false
)