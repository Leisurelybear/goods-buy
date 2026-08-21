package com.goodsbuy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun GoodsBuyTheme(
    theme: ThemeConfig = DreamyPurpleTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) theme.darkColors else theme.lightColors
    val activeGradient = if (darkTheme) theme.darkBrandGradient else theme.brandGradient
    CompositionLocalProvider(
        LocalAppTheme provides theme,
        LocalAppGradient provides activeGradient
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
