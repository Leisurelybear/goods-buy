package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient

/**
 * 渐变底信息卡。默认使用当前主题品牌渐变（深色模式自动切换暗夜紫渐变）。
 * 可通过 [gradient] 传入自定义渐变（浅色模式下生效，用于区分不同指标卡）。
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: List<Color>? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val brand = LocalAppGradient.current
    val colors = if (gradient != null && !isSystemInDarkTheme()) {
        gradient
    } else {
        listOf(brand.start, brand.end)
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(colors))
            .padding(contentPadding),
        content = content
    )
}
