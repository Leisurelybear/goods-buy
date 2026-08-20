package com.goodsbuy.app.util

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class FadeShape { ELLIPSE, CIRCLE, ROUNDED_RECT, RECT }

object EdgeFadeMask {

    const val MAX_KEEP_FRACTION = 1f
    const val MIN_KEEP_FRACTION = 0.15f
    const val DEFAULT_FEATHER = 0.2f

    fun keepScale(intensity: Float): Float {
        val t = intensity.coerceIn(0f, 1f)
        return MAX_KEEP_FRACTION - (MAX_KEEP_FRACTION - MIN_KEEP_FRACTION) * t
    }

    fun alphaMask(
        width: Int,
        height: Int,
        shape: FadeShape,
        intensity: Float,
        feather: Float = DEFAULT_FEATHER
    ): IntArray {
        require(width > 0 && height > 0)
        val alphas = IntArray(width * height)
        val cx = width / 2f
        val cy = height / 2f
        val keep = keepScale(intensity)
        when (shape) {
            FadeShape.ELLIPSE -> {
                val inner = keep
                val outer = (keep + feather).coerceAtMost(1f)
                for (y in 0 until height) {
                    val ny = (y - cy) / cy
                    for (x in 0 until width) {
                        val nx = (x - cx) / cx
                        val d = sqrt(nx * nx + ny * ny) * 0.70710678f
                        alphas[y * width + x] = alphaFor(d, inner, outer)
                    }
                }
            }
            FadeShape.CIRCLE -> {
                val corner = sqrt(cx * cx + cy * cy)
                val inner = keep
                val outer = (keep + feather).coerceAtMost(1f)
                for (y in 0 until height) {
                    val dy = y - cy
                    for (x in 0 until width) {
                        val dx = x - cx
                        val d = sqrt(dx * dx + dy * dy) / corner
                        alphas[y * width + x] = alphaFor(d, inner, outer)
                    }
                }
            }
            FadeShape.RECT -> {
                val halfW = keep * cx
                val halfH = keep * cy
                val band = feather * min(cx, cy)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val dEdgeX = abs(x - cx) - halfW
                        val dEdgeY = abs(y - cy) - halfH
                        val d = max(dEdgeX, dEdgeY)
                        alphas[y * width + x] = alphaFor(d, 0f, band)
                    }
                }
            }
            FadeShape.ROUNDED_RECT -> {
                val insetX = (1f - keep) * cx
                val insetY = (1f - keep) * cy
                val halfW = cx - insetX
                val halfH = cy - insetY
                val radius = min(min(insetX, insetY) * 0.5f, min(halfW, halfH) * 0.5f)
                val band = feather * min(cx, cy)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val qx = abs(x - cx) - (halfW - radius)
                        val qy = abs(y - cy) - (halfH - radius)
                        val clamped = sqrt(max(qx, 0f) * max(qx, 0f) + max(qy, 0f) * max(qy, 0f)) +
                            min(max(qx, qy), 0f) - radius
                        alphas[y * width + x] = alphaFor(clamped, 0f, band)
                    }
                }
            }
        }
        return alphas
    }

    fun applyAlpha(pixels: IntArray, alpha: IntArray): IntArray {
        require(pixels.size == alpha.size)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val a = alpha[i]
            if (a == 255) {
                out[i] = pixels[i]
            } else {
                val src = pixels[i]
                val sr = (src shr 16) and 0xFF
                val sg = (src shr 8) and 0xFF
                val sb = src and 0xFF
                out[i] = (a shl 24) or (sr shl 16) or (sg shl 8) or sb
            }
        }
        return out
    }

    private fun alphaFor(d: Float, inner: Float, outer: Float): Int {
        if (d <= inner) return 255
        if (d >= outer) return 0
        val t = (d - inner) / (outer - inner).coerceAtLeast(1e-5f)
        val s = t * t * (3f - 2f * t)
        return ((1f - s) * 255f).toInt()
    }
}