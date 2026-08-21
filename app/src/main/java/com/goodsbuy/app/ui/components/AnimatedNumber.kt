package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedNumber(
    targetValue: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    decimals: Int = 2,
    prefix: String = "",
    suffix: String = ""
) {
    var displayValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetValue) { displayValue = targetValue.toFloat() }

    val animatedValue by animateFloatAsState(
        targetValue = displayValue,
        animationSpec = numberAnimSpec,
        label = "animated_number"
    )

    val formatStr = "%,.${decimals}f"
    val text = if (animatedValue < 0) {
        "-$prefix${String.format(formatStr, -animatedValue)}$suffix"
    } else {
        "$prefix${String.format(formatStr, animatedValue)}$suffix"
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
fun AnimatedInt(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    prefix: String = "",
    suffix: String = ""
) {
    var displayValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetValue) { displayValue = targetValue.toFloat() }

    val animatedValue by animateFloatAsState(
        targetValue = displayValue,
        animationSpec = numberAnimSpec,
        label = "animated_int"
    )

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
        modifier = modifier
    )
}
