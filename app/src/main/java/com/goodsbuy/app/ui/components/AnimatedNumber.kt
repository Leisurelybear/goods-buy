package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

/**
 * 数字变化时的轻微"弹一下"。首次渲染不播放，避免页面刚进来就抖。
 * 返回 State，调用方在 graphicsLayer 里读 value，只触发图层重绘而非重组。
 */
@Composable
private fun rememberPopScale(target: Any?, enabled: Boolean): State<Float> {
    val scale = remember { Animatable(1f) }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (!seeded) {
            seeded = true
        } else if (enabled) {
            scale.animateTo(1.10f, tween(110, easing = FastOutSlowInEasing))
            scale.animateTo(1f, popSpec)
        }
    }
    return scale.asState()
}

/**
 * 滚动计数数字。数值变化时除了滚动，还会轻微弹一下，让"变了"这件事更容易被看见。
 *
 * @param showSign 非负值时显示 "+" 前缀（盈亏类数字用）。
 * @param popOnChange 关闭可得到纯滚动效果。
 */
@Composable
fun AnimatedNumber(
    targetValue: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    decimals: Int = 2,
    prefix: String = "",
    suffix: String = "",
    showSign: Boolean = false,
    popOnChange: Boolean = true
) {
    var displayValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetValue) { displayValue = targetValue.toFloat() }

    val animatedValue by animateFloatAsState(
        targetValue = displayValue,
        animationSpec = numberAnimSpec,
        label = "animated_number"
    )

    val popScale = rememberPopScale(targetValue, popOnChange)

    val formatStr = "%,.${decimals}f"
    val text = if (animatedValue < 0) {
        "-$prefix${String.format(formatStr, -animatedValue)}$suffix"
    } else {
        "${if (showSign) "+" else ""}$prefix${String.format(formatStr, animatedValue)}$suffix"
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier.graphicsLayer {
            scaleX = popScale.value
            scaleY = popScale.value
        }
    )
}

/** 整数版滚动计数（[AnimatedNumber] 的 Int 包装）。 */
@Composable
fun AnimatedInt(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    prefix: String = "",
    suffix: String = "",
    popOnChange: Boolean = true
) {
    var displayValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetValue) { displayValue = targetValue.toFloat() }

    val animatedValue by animateFloatAsState(
        targetValue = displayValue,
        animationSpec = numberAnimSpec,
        label = "animated_int"
    )

    val popScale = rememberPopScale(targetValue, popOnChange)

    val v = animatedValue.toInt()
    val text = if (v < 0) {
        "-$prefix${String.format("%,d", -v)}$suffix"
    } else {
        "$prefix${String.format("%,d", v)}$suffix"
    }
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.graphicsLayer {
            scaleX = popScale.value
            scaleY = popScale.value
        }
    )
}