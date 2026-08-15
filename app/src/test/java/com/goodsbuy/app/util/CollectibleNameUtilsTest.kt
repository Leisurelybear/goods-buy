package com.goodsbuy.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectibleNameUtilsTest {

    @Test
    fun `no timestamp suffix - appends new timestamp`() {
        assertEquals(
            "Star Figure 2026-08-15 10:30",
            CollectibleNameUtils.buildDuplicateName("Star Figure", "2026-08-15 10:30")
        )
    }

    @Test
    fun `existing timestamp suffix - replaces it with new timestamp`() {
        assertEquals(
            "Star Figure 2026-08-15 10:30",
            CollectibleNameUtils.buildDuplicateName("Star Figure 2026-08-14 09:00", "2026-08-15 10:30")
        )
    }

    @Test
    fun `user modified suffix no longer matching - appends new timestamp`() {
        assertEquals(
            "Star Figure 2026-08-14 2026-08-15 10:30",
            CollectibleNameUtils.buildDuplicateName("Star Figure 2026-08-14", "2026-08-15 10:30")
        )
    }
}