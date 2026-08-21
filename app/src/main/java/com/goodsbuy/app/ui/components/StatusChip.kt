package com.goodsbuy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.OrderStatus

/**
 * 状态胶囊。[onImage] 为 true 时用于叠加在图片上：深色遮罩底 + 白字，
 * 仅保留状态色圆点作标识，保证任意照片背景下的可读性。
 */
@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier, onImage: Boolean = false) {
    val targetColor = Color(status.colorHex)
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = colorAnimSpec,
        label = "status_chip_color"
    )
    Surface(
        color = if (onImage) Color.Black.copy(alpha = 0.55f) else color.copy(alpha = 0.15f),
        contentColor = if (onImage) Color.White else color,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                status.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
