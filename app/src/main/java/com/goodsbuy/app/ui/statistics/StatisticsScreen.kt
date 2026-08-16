package com.goodsbuy.app.ui.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.components.AnimatedInt
import com.goodsbuy.app.ui.components.AnimatedNumber
import com.goodsbuy.app.ui.components.ProfitLossText
import com.goodsbuy.app.ui.theme.LossRed
import com.goodsbuy.app.ui.theme.ProfitGreen

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilter by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedStatus) {
        viewModel.changeStatusFilter(selectedStatus)
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("总览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRowAnimated("总投入", uiState.summary.totalInvestment)
                    StatRowAnimated("总营收", uiState.summary.totalRevenue)
                    StatRow("累计盈亏", profitAmount = uiState.summary.totalProfit)
                    StatRow(
                        "盈亏比例",
                        "${String.format("%.1f", uiState.summary.totalProfitRate)}%",
                        valueColor = if (uiState.summary.totalProfit >= 0) ProfitGreen else LossRed
                    )
                    StatRowAnimated("持仓市值", uiState.summary.holdingValue)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("藏品总数", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            AnimatedInt(
                                targetValue = uiState.summary.totalCount,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                " (持有${uiState.summary.ownedCount}/已售${uiState.summary.soldCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            MonthlyTrendChart(stats = uiState.monthlyStats)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("分类统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showFilter = !showFilter }) {
                    Icon(Icons.Default.FilterList, contentDescription = "筛选状态")
                }
            }

            AnimatedVisibility(
                visible = showFilter,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("全部") }
                    )
                    OrderStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status.name,
                            onClick = { selectedStatus = status.name },
                            label = { Text(status.displayName) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.categoryType == "ip", onClick = { viewModel.changeCategoryType("ip") }, label = { Text("按IP") })
                FilterChip(selected = uiState.categoryType == "series", onClick = { viewModel.changeCategoryType("series") }, label = { Text("按系列") })
                FilterChip(selected = uiState.categoryType == "category", onClick = { viewModel.changeCategoryType("category") }, label = { Text("按品类") })
            }
        }

        items(uiState.categoryStats, key = { it.categoryName }) { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stat.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text("${stat.count}件 \u00b7 投入\u00a5${String.format("%.0f", stat.investment)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ProfitLossText(profitLoss = ProfitLoss(stat.investment, stat.revenue, stat.profit, if (stat.investment > 0) (stat.profit / stat.investment) * 100 else 0.0))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatRowAnimated(label: String, value: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedNumber(
            targetValue = value,
            prefix = "\u00a5",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatRow(label: String, value: String = "", profitAmount: Double? = null, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profitAmount != null) {
            val color = if (profitAmount >= 0) ProfitGreen else LossRed
            val sign = if (profitAmount >= 0) "+" else ""
            AnimatedNumber(
                targetValue = profitAmount,
                prefix = "$sign\u00a5",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}
