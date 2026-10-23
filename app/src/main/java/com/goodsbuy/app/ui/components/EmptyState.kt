package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient

/**
 * 空状态：入场淡入上浮，之后图标持续"悬浮 + 呼吸"，外圈光晕缓慢明灭。
 * 动效幅度刻意压得很小，只负责让空页面不显得死板。
 */
@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String = "还没有藏品，点击 + 添加") {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "empty_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "empty_offset"
    )

    val floatTransition = rememberInfiniteTransition(label = "empty_float")
    val bob by floatTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2_600), RepeatMode.Reverse),
        label = "empty_bob"
    )
    val glow by floatTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(2_600), RepeatMode.Reverse),
        label = "empty_glow"
    )
    val iconScale = rememberBreathingScale(minScale = 1f, maxScale = 1.06f, durationMillis = 2_600)

    val gradient = LocalAppGradient.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .alpha(alpha)
            .offset(y = offsetY.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = bob.dp)) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(gradient.start.copy(alpha = glow), gradient.end.copy(alpha = 0f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(gradient.start, gradient.end))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}