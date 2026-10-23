package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 统一图片组件：加载中显示流光骨架，加载完成淡入，避免"白块 → 硬切"的廉价感。
 * 全站所有 AsyncImage 都应走这里，保证图片体验一致。
 */
@Composable
fun AppAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    shimmer: Boolean = true,
    placeholderColor: Color? = null
) {
    val context = LocalContext.current
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(220)
            .build()
    }
    var loading by remember(model) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (placeholderColor != null) Modifier.background(placeholderColor) else Modifier)
    ) {
        if (shimmer && loading) {
            Box(
                Modifier
                    .matchParentSize()
                    .shimmerBackground(shape = shape)
            )
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onLoading = { loading = true },
            onSuccess = { loading = false },
            onError = { loading = false }
        )
    }
}