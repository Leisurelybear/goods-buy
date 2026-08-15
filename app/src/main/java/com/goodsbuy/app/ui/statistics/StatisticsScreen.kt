package com.goodsbuy.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
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
                    StatRow("总投入", "¥${String.format("%.2f", uiState.summary.totalInvestment)}")
                    StatRow("总营收", "¥${String.format("%.2f", uiState.summary.totalRevenue)}")
                    StatRow("累计盈亏", "", profitAmount = uiState.summary.totalProfit)
                    StatRow(
                        "盈亏比例",
                        "${String.format("%.1f", uiState.summary.totalProfitRate)}%",
                        valueColor = if (uiState.summary.totalProfit >= 0) ProfitGreen else LossRed
                    )
                    StatRow("持仓市值", "¥${String.format("%.2f", uiState.summary.holdingValue)}")
                    StatRow("藏品总数", "${uiState.summary.totalCount} (持有${uiState.summary.ownedCount}/已售${uiState.summary.soldCount})")
                }
            }
        }

        item {
            MonthlyTrendChart(stats = uiState.monthlyStats)
        }

        item {
            // Status filter row
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

            if (showFilter) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

        items(uiState.categoryStats) { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stat.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text("${stat.count}件 · 投入¥${String.format("%.0f", stat.investment)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun StatRow(label: String, value: String, profitAmount: Double? = null, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profitAmount != null) {
            val color = if (profitAmount >= 0) ProfitGreen else LossRed
            val sign = if (profitAmount >= 0) "+" else ""
            Text(text = "$sign¥${String.format("%.2f", profitAmount)}", color = color, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}
