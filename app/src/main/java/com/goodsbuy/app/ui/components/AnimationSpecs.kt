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
