package com.goodsbuy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * 看板/统计数字：加粗、等宽数字（tnum）、带滚动计数动画。
 * 默认 ¥ 前缀；渐变卡上文字统一用 [MaterialTheme.colorScheme.onPrimary] 以保证对比度。
 */
@Composable
fun StatNumber(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    decimals: Int = 0,
    prefix: String = "\u00a5",
    suffix: String = ""
) {
    val tabular = style.copy(fontFeatureSettings = "tnum")
    AnimatedNumber(
        targetValue = value,
        modifier = modifier,
        style = tabular,
        color = color,
        decimals = decimals,
        prefix = prefix,
        suffix = suffix
    )
}
