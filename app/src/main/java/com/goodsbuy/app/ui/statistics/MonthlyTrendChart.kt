package com.goodsbuy.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.MonthlyStat
import java.util.Locale
import kotlin.math.max

@Composable
fun MonthlyTrendChart(
    stats: List<MonthlyStat>,
    modifier: Modifier = Modifier
) {
    val visibleStats = stats.takeLast(12)
    val expenseColor = MaterialTheme.colorScheme.primary
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("月度收支趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (visibleStats.isEmpty()) {
                Text(
                    "录入购买或售出日期后，这里会显示趋势",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TrendLegend("支出", expenseColor)
                    TrendLegend("收入", incomeColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val chartWidth = maxOf(320.dp, (visibleStats.size * 72).dp)
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.width(chartWidth)) {
                        val description = visibleStats.joinToString("；") {
                            "${it.yearMonth}支出${formatAmount(it.expense)}元，收入${formatAmount(it.income)}元"
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp)
                                .semantics { contentDescription = description }
                        ) {
                            val maxValue = max(
                                1.0,
                                visibleStats.maxOf { max(it.expense, it.income) }
                            ).toFloat()
                            val chartHeight = size.height - 16.dp.toPx()
                            val xStep = if (visibleStats.size == 1) 0f else size.width / (visibleStats.size - 1)

                            for (index in 1..3) {
                                val y = chartHeight * index / 4f
                                drawLine(
                                    color = gridColor.copy(alpha = 0.45f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            drawSeries(visibleStats.map { it.expense.toFloat() }, expenseColor, maxValue, chartHeight, xStep)
                            drawSeries(visibleStats.map { it.income.toFloat() }, incomeColor, maxValue, chartHeight, xStep)
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            visibleStats.forEach { stat ->
                                Text(
                                    text = stat.yearMonth.substringAfter("-") + "月",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "最近 ${visibleStats.size} 个月 · 支出 ¥${formatAmount(visibleStats.sumOf { it.expense })} · 收入 ¥${formatAmount(visibleStats.sumOf { it.income })}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendLegend(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Spacer(modifier = Modifier.width(10.dp).height(10.dp).background(color, RectangleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun DrawScope.drawSeries(
    values: List<Float>,
    color: Color,
    maxValue: Float,
    chartHeight: Float,
    xStep: Float
) {
    if (values.isEmpty()) return
    val path = Path()
    values.forEachIndexed { index, value ->
        val x = if (values.size == 1) size.width / 2f else index * xStep
        val y = chartHeight - (value / maxValue).coerceIn(0f, 1f) * chartHeight
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        drawCircle(color = color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

private fun formatAmount(amount: Double): String = String.format(Locale.getDefault(), "%.0f", amount)
