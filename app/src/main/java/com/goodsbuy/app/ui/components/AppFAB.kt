package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient

/**
 * 品牌 FAB：入场弹性放大 + 按下回弹，底色使用缓慢流动的品牌渐变。
 */
@Composable
fun AppFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val gradient = LocalAppGradient.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.90f)

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val entrance by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = revealSpring,
        label = "fab_entrance"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                val combined = pressScale * entrance
                scaleX = combined
                scaleY = combined
                alpha = entrance
            },
        interactionSource = interactionSource,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        content = {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .auroraGradient(listOf(gradient.start, gradient.end)),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    )
}