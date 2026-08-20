package com.goodsbuy.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeFadeMaskTest {

    private fun alphaAt(mask: IntArray, width: Int, x: Int, y: Int): Int = mask[y * width + x]

    @Test
    fun `keepScale maps intensity to keep fraction`() {
        assertEquals(1f, EdgeFadeMask.keepScale(0f), 1e-4f)
        assertEquals(0.15f, EdgeFadeMask.keepScale(1f), 1e-4f)
        assertEquals(1f, EdgeFadeMask.keepScale(-1f), 1e-4f)
        assertEquals(0.15f, EdgeFadeMask.keepScale(2f), 1e-4f)
    }

    @Test
    fun `zero intensity keeps every pixel opaque for all shapes`() {
        for (shape in FadeShape.entries) {
            val mask = EdgeFadeMask.alphaMask(20, 10, shape, 0f)
            assertTrue("shape=$shape", mask.all { it == 255 })
        }
    }

    @Test
    fun `full intensity ellipse leaves center opaque and corners transparent`() {
        val mask = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 1f)
        assertEquals(255, alphaAt(mask, 20, 10, 5))
        assertEquals(0, alphaAt(mask, 20, 0, 0))
        assertEquals(0, alphaAt(mask, 20, 19, 0))
    }

    @Test
    fun `circle keeps round region so left edge midpoint transparent at full intensity`() {
        val mask = EdgeFadeMask.alphaMask(20, 10, FadeShape.CIRCLE, 1f)
        assertEquals(0, alphaAt(mask, 20, 0, 5))
    }

    @Test
    fun `circle fades wide image left edge midpoint more than ellipse`() {
        val circle = EdgeFadeMask.alphaMask(20, 10, FadeShape.CIRCLE, 0.4f)
        val ellipse = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 0.4f)
        assertTrue(alphaAt(circle, 20, 0, 5) < alphaAt(ellipse, 20, 0, 5))
        assertEquals(255, alphaAt(ellipse, 20, 10, 5))
        assertEquals(255, alphaAt(circle, 20, 10, 5))
    }

    @Test
    fun `full intensity rect keeps center and fades edges`() {
        val mask = EdgeFadeMask.alphaMask(20, 10, FadeShape.RECT, 1f)
        assertEquals(255, alphaAt(mask, 20, 10, 5))
        assertEquals(0, alphaAt(mask, 20, 0, 0))
        assertEquals(0, alphaAt(mask, 20, 10, 0))
    }

    @Test
    fun `full intensity rounded rect keeps center and fades corners`() {
        val mask = EdgeFadeMask.alphaMask(20, 10, FadeShape.ROUNDED_RECT, 1f)
        assertEquals(255, alphaAt(mask, 20, 10, 5))
        assertEquals(0, alphaAt(mask, 20, 0, 0))
    }

    @Test
    fun `higher intensity reduces edge pixel alpha`() {
        val low = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 0.3f)
        val high = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 0.8f)
        assertTrue(alphaAt(low, 20, 2, 5) > alphaAt(high, 20, 2, 5))
    }

    @Test
    fun `transparency fraction grows with intensity`() {
        val zero = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 0f)
        val full = EdgeFadeMask.alphaMask(20, 10, FadeShape.ELLIPSE, 1f)
        assertEquals(0, zero.count { it < 255 })
        val transparent = full.count { it < 255 }
        assertTrue(transparent > (20 * 10) / 2)
    }

    @Test
    fun `applyAlpha sets alpha and keeps rgb`() {
        val pixels = intArrayOf(0xFF123456.toInt())
        val alpha = intArrayOf(0x80)
        assertEquals(0x80123456.toInt(), EdgeFadeMask.applyAlpha(pixels, alpha)[0])
    }

    @Test
    fun `applyAlpha keeps pixel unchanged when alpha is 255`() {
        val pixels = intArrayOf(0xFFAABBCC.toInt())
        val alpha = intArrayOf(255)
        assertEquals(0xFFAABBCC.toInt(), EdgeFadeMask.applyAlpha(pixels, alpha)[0])
    }
}