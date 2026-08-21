package com.goodsbuy.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {

    @Test
    fun `byId returns DreamyPurple for known id`() {
        assertEquals(DreamyPurpleTheme, AppThemes.byId("dreamy_purple"))
    }

    @Test
    fun `byId falls back to DreamyPurple for unknown id`() {
        assertEquals(DreamyPurpleTheme, AppThemes.byId("not_exists"))
    }

    @Test
    fun `dreamy theme has distinct light and dark backgrounds`() {
        assertNotEquals(
            DreamyPurpleTheme.lightColors.background,
            DreamyPurpleTheme.darkColors.background
        )
    }

    @Test
    fun `dreamy theme defines brand gradients with distinct endpoints`() {
        assertNotEquals(
            DreamyPurpleTheme.brandGradient.start,
            DreamyPurpleTheme.brandGradient.end
        )
        assertTrue(AppThemes.all.size >= 1)
    }
}
