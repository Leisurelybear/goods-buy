package com.goodsbuy.app.ui.components

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * 全站"是否播放装饰性动效"开关。
 *
 * 默认允许；[com.goodsbuy.app.MainActivity] 会按系统动画设置（开发者选项里的
 * "动画时长缩放"设为 0、或系统开启"移除动画"）自动降级为 false。
 * 降级后所有装饰性动效直接落到终态：功能与布局不变，只是不再"动"。
 */
val LocalMotionEnabled = compositionLocalOf { true }

/**
 * 读取系统动画开关。[ValueAnimator.areAnimatorsEnabled] 为 false 表示系统层面
 * 关闭了动画，此时应当降级。读取异常时按"允许动效"处理，避免误伤。
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean = remember { systemAnimationsEnabled() }

internal fun systemAnimationsEnabled(): Boolean = runCatching {
    ValueAnimator.areAnimatorsEnabled()
}.getOrDefault(true)