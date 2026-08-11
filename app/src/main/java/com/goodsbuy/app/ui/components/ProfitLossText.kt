package com.goodsbuy.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.theme.ProfitGreen
import com.goodsbuy.app.ui.theme.LossRed

@Composable
fun ProfitLossText(profitLoss: ProfitLoss, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val color = if (profitLoss.profitAmount >= 0) ProfitGreen else LossRed
    val sign = if (profitLoss.profitAmount >= 0) "+" else ""
    Text(
        text = "$sign¥${String.format("%.2f", profitLoss.profitAmount)}",
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}
