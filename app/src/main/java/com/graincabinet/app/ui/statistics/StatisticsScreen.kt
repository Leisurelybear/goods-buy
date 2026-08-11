package com.graincabinet.app.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.graincabinet.app.ui.components.ProfitLossText

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("总览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow("总投入", "¥${String.format("%.2f", uiState.summary.totalInvestment)}")
                    StatRow("总营收", "¥${String.format("%.2f", uiState.summary.totalRevenue)}")
                    StatRow("累计盈亏", "", profitLoss = uiState.summary.totalProfit)
                    StatRow("盈亏比例", "${String.format("%.1f", uiState.summary.totalProfitRate)}%", profitLoss = uiState.summary.totalProfit)
                    StatRow("持仓市值", "¥${String.format("%.2f", uiState.summary.holdingValue)}")
                    StatRow("藏品总数", "${uiState.summary.totalCount} (持有${uiState.summary.ownedCount}/已售${uiState.summary.soldCount})")
                }
            }
        }

        item {
            Text("分类统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    ProfitLossText(profitLoss = com.graincabinet.app.domain.model.ProfitLoss(stat.investment, stat.revenue, stat.profit, if (stat.investment > 0) (stat.profit / stat.investment) * 100 else 0.0))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String, profitLoss: Double? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profitLoss != null) {
            val color = if (profitLoss >= 0) com.graincabinet.app.ui.theme.ProfitGreen else com.graincabinet.app.ui.theme.LossRed
            val sign = if (profitLoss >= 0) "+" else ""
            Text(text = "$sign¥${String.format("%.2f", profitLoss)}", color = color, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
