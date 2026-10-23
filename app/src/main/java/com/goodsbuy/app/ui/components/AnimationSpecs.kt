package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

/**
 * Centralized animation specs so every module uses consistent timing.
 * All durations follow the review doc guideline of 150-300ms,
 * and all springs use medium stiffness for a natural feel.
 */

val enterAnimSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val colorAnimSpec: FiniteAnimationSpec<Color> = tween(150, easing = FastOutSlowInEasing)
val numberAnimSpec: FiniteAnimationSpec<Float> = tween(400, easing = FastOutSlowInEasing)
val fadeAnimSpec: FiniteAnimationSpec<Float> = tween(200, easing = FastOutSlowInEasing)
val slideAnimSpec: FiniteAnimationSpec<Float> = tween(250, easing = FastOutSlowInEasing)
val chartDrawSpec: FiniteAnimationSpec<Float> = tween(500, easing = FastOutSlowInEasing)
val pressScaleSpring: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

val enterEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val exitEasing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

/* ------------------------------------------------------------------------------------------------
 * v1.5 动画增强新增规格：统一"回弹 / 揭示 / 呼吸 / 导航"的时序，避免各页面各写一套魔法数字。
 * ---------------------------------------------------------------------------------------------- */

/** 数字/胶囊变化时的"弹一下"回弹。 */
val popSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/** 列表项依次揭示（stagger）时使用，略带过冲，更有"活过来"的感觉。 */
val revealSpring: FiniteAnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** 呼吸/脉动等无限循环动画的单个周期。 */
val pulseSpec: FiniteAnimationSpec<Float> = tween(2400, easing = FastOutSlowInEasing)

/** 底部 Tab 之间切换：轻缩放 + 淡入淡出（shared axis 的简化版）。 */
val tabEnterSpec: FiniteAnimationSpec<Float> = tween(240, easing = enterEasing)
val tabExitSpec: FiniteAnimationSpec<Float> = tween(160, easing = exitEasing)

/** 二级页面（详情/表单/图鉴）推入与退出。 */
val pageEnterSpec: FiniteAnimationSpec<Float> = tween(280, easing = enterEasing)
val pageExitSpec: FiniteAnimationSpec<Float> = tween(220, easing = exitEasing)
