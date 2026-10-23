package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient

/**
 * 品牌横幅：入场淡入上浮，底色使用缓慢流动的"极光"渐变。
 * 标题/副标题/底部内容接口保持不变，业务侧无需改动。
 */
@Composable
fun HeroHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    val gradient = LocalAppGradient.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val heroAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = fadeAnimSpec,
        label = "hero_alpha"
    )
    val heroOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = revealSpring,
        label = "hero_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = heroAlpha
                translationY = heroOffset
            }
            .clip(MaterialTheme.shapes.medium)
            .auroraGradient(listOf(gradient.start, gradient.end))
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
            if (bottomContent != null) {
                bottomContent()
            }
        }
        if (action != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) { action() }
        }
    }
}