package com.goodsbuy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.OrderStatus

@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val targetColor = Color(status.colorHex)
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = colorAnimSpec,
        label = "status_chip_color"
    )
    Text(
        text = status.displayName,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
