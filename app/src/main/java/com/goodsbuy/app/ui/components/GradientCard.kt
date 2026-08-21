package com.goodsbuy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.theme.LocalAppGradient
import com.goodsbuy.app.ui.theme.LocalAppTheme

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    useDarkGradient: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val gradient = if (useDarkGradient) theme.darkBrandGradient else LocalAppGradient.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(gradient.start, gradient.end)))
            .padding(20.dp),
        content = content
    )
}
