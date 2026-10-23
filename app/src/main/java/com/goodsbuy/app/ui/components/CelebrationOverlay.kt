package com.goodsbuy.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val aspect: Float,
    val spin: Float,
    val alpha: Float,
    val color: Color
)

/**
 * 轻量礼花庆祝层：保存成功、导入完成等"值得开心一下"的时刻播放一次。
 *
 * 纯 Canvas 绘制，不参与布局测量、不拦截触摸（没有 pointerInput），
 * 动画结束由调用方把 [visible] 置回 false 即可。
 *
 * @param originX/originY 爆发原点，取值 0f..1f，相对于自身尺寸。
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 84,
    durationMillis: Int = 1_150,
    originX: Float = 0.5f,
    originY: Float = 0.45f
) {
    if (!visible) return

    val brand = LocalAppGradient.current
    val scheme = MaterialTheme.colorScheme
    val palette = remember(brand, scheme.tertiary, scheme.secondary) {
        listOf(
            brand.start,
            brand.end,
            scheme.tertiary,
            scheme.secondary,
            Color(0xFFFFD166),
            Color(0xFF06D6A0)
        )
    }
    val baseSize = with(LocalDensity.current) { 9.dp.toPx() }

    val particles = remember(particleCount, baseSize, palette) {
        val random = Random(particleCount * 7919L)
        List(particleCount) {
            val upward = -PI.toFloat() / 2f
            ConfettiParticle(
                angle = upward + (random.nextFloat() - 0.5f) * 2.2f,
                speed = 0.28f + random.nextFloat() * 0.62f,
                size = baseSize * (0.55f + random.nextFloat() * 0.7f),
                aspect = 0.45f + random.nextFloat() * 0.85f,
                spin = (random.nextFloat() - 0.5f) * 2f,
                alpha = 0.7f + random.nextFloat() * 0.3f,
                color = palette[random.nextInt(palette.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = LinearEasing))
    }

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        val origin = Offset(size.width * originX, size.height * originY)
        val travel = size.minDimension

        particles.forEach { particle ->
            val distance = particle.speed * travel * t
            val x = origin.x + cos(particle.angle) * distance
            val y = origin.y + sin(particle.angle) * distance + 0.6f * travel * t * t
            val fade = (particle.alpha * (1f - t * t)).coerceIn(0f, 1f)
            if (fade <= 0.02f) return@forEach
            rotate(degrees = particle.spin * t * 540f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = fade),
                    topLeft = Offset(x - particle.size / 2f, y - particle.size * particle.aspect / 2f),
                    size = Size(particle.size, particle.size * particle.aspect)
                )
            }
        }
    }
}