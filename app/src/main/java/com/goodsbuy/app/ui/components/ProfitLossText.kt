package com.goodsbuy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.theme.LossRed
import com.goodsbuy.app.ui.theme.ProfitGreen

@Composable
fun ProfitLossText(profitLoss: ProfitLoss, modifier: Modifier = Modifier) {
    val targetColor = if (profitLoss.profitAmount >= 0) ProfitGreen else LossRed
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = colorAnimSpec,
        label = "profit_loss_color"
    )
    val sign = if (profitLoss.profitAmount >= 0) "+" else ""
    Text(
        text = "$sign\u00a5${String.format("%.2f", profitLoss.profitAmount)}",
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}
