package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 全站通用动效工具集。
 *
 * 设计原则：所有动效都读 [AnimationSpecs] 里的统一时序，单个页面不写魔法数字；
 * 动效只做"锦上添花"，不参与布局测量（缩放走 graphicsLayer），因此不会引起重排或跳动。
 */

/**
 * 按压回弹系数。把返回值应用到 `Modifier.graphicsLayer` 上即可获得
 * "按下缩小、松手弹回"的手感。
 */
@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
    enabled: Boolean = true
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = pressScaleSpring,
        label = "press_scale"
    ).value
}

/** 给任意区域加上"按下回弹 + 涟漪"效果。 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    indication: Indication? = LocalIndication.current,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, pressedScale, enabled)
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 骨架屏流光。把扫光画在内容背后，图片/文字加载完成后自然被覆盖。
 * 占位块、图片卡片底衬都可以直接挂这个修饰符。
 */
@Composable
fun Modifier.shimmerBackground(
    shape: Shape = RectangleShape,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    durationMillis: Int = 1_200
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    return this
        .clip(shape)
        .drawWithCache {
            val sweepWidth = size.width.coerceAtLeast(1f) * 0.65f
            val startX = -sweepWidth + progress * (size.width + sweepWidth * 2f)
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(startX, 0f),
                end = Offset(startX + sweepWidth, size.height.coerceAtLeast(1f))
            )
            onDrawBehind { drawRect(brush) }
        }
}

/**
 * 缓慢流动的品牌渐变（"极光"底纹）。用于 Hero 横幅、FAB 等品牌表面，
 * 让静态渐变"活"起来；周期很长（十几秒），不会喧宾夺主。
 */
@Composable
fun Modifier.auroraGradient(
    colors: List<Color>,
    durationMillis: Int = 14_000
): Modifier {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_phase"
    )
    return this.drawWithCache {
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val shift = (phase - 0.5f) * width * 0.9f
        val first = colors.first()
        val second = colors[minOf(1, colors.lastIndex)]
        val brush = Brush.linearGradient(
            colors = listOf(first, second, first),
            start = Offset(-width * 0.4f + shift, 0f),
            end = Offset(width * 1.4f + shift, height)
        )
        onDrawBehind { drawRect(brush) }
    }
}

/** 选中态描边：宽度从 0 弹性增长，避免"啪"地出现一圈硬边。 */
@Composable
fun Modifier.animatedBorder(
    visible: Boolean,
    shape: Shape,
    brush: Brush,
    width: Dp = 2.dp
): Modifier {
    val borderWidth by animateDpAsState(
        targetValue = if (visible) width else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animated_border_width"
    )
    return this.border(BorderStroke(borderWidth, brush), shape)
}

/**
 * 列表项依次揭示（stagger）。同一屏内 index 越大出现越晚，形成瀑布式入场。
 * [key] 变化时会重新播放一次（例如切换筛选、切换分组）。
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    key: Any? = Unit,
    stepMillis: Int = 45,
    maxDelayMillis: Int = 270,
    slideFrom: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    var revealed by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        delay(index.coerceAtLeast(0).times(stepMillis).coerceAtMost(maxDelayMillis).toLong())
        revealed = true
    }
    val slidePx = with(LocalDensity.current) { slideFrom.toPx() }
    val revealAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = fadeAnimSpec,
        label = "reveal_alpha"
    )
    val revealSlide by animateFloatAsState(
        targetValue = if (revealed) 0f else slidePx,
        animationSpec = revealSpring,
        label = "reveal_slide"
    )
    Box(
        modifier.graphicsLayer {
            alpha = revealAlpha
            translationY = revealSlide
        }
    ) {
        content()
    }
}

/**
 * 循环"呼吸"缩放，用于空状态图标、提示性元素等需要持续吸引注意力的地方。
 * 幅度很小（默认 +4%），避免干扰阅读。
 */
@Composable
fun rememberBreathingScale(
    minScale: Float = 1f,
    maxScale: Float = 1.04f,
    durationMillis: Int = 2_400
): Float {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )
    return scale
}