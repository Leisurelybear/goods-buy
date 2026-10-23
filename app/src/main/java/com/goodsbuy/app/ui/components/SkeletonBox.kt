package com.goodsbuy.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 骨架占位块：带流光，加载态不再是一块死板的灰。 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(16.dp)
            .shimmerBackground(shape = MaterialTheme.shapes.extraSmall)
    )
}