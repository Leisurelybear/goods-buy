package com.goodsbuy.app.ui.statistics

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
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.components.GradientCard
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ProfitLossText
import com.goodsbuy.app.ui.components.StatNumber

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedStatus) {
        viewModel.changeStatusFilter(selectedStatus)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroHeader(
                title = "数据看板",
                subtitle = "累计盈亏 ¥${String.format("%,.0f", uiState.summary.totalProfit)}"
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientCard(Modifier.weight(1f)) {
                    StatNumber(
                        value = "¥${String.format("%,.0f", uiState.summary.totalInvestment)}",
                        label = "总投入",
                        valueColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                GradientCard(Modifier.weight(1f)) {
                    StatNumber(
                        value = "¥${String.format("%,.0f", uiState.summary.totalRevenue)}",
                        label = "总营收",
                        valueColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientCard(Modifier.weight(1f)) {
                    StatNumber(
                        value = "${if (uiState.summary.totalProfit >= 0) "+" else ""}¥${String.format("%,.0f", uiState.summary.totalProfit)}",
                        label = "累计盈亏",
                        valueColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                GradientCard(Modifier.weight(1f)) {
                    StatNumber(
                        value = "¥${String.format("%,.0f", uiState.summary.holdingValue)}",
                        label = "持仓市值",
                        valueColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }
        item { MonthlyTrendChart(stats = uiState.monthlyStats) }
        item {
            Text("状态筛选", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = selectedStatus == null, onClick = { selectedStatus = null }, label = { Text("全部") })
                OrderStatus.entries.forEach { status ->
                    FilterChip(selected = selectedStatus == status.name, onClick = { selectedStatus = status.name }, label = { Text(status.displayName) })
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.categoryType == "ip", onClick = { viewModel.changeCategoryType("ip") }, label = { Text("按IP") })
                FilterChip(selected = uiState.categoryType == "series", onClick = { viewModel.changeCategoryType("series") }, label = { Text("按系列") })
                FilterChip(selected = uiState.categoryType == "category", onClick = { viewModel.changeCategoryType("category") }, label = { Text("按品类") })
            }
        }
        items(uiState.categoryStats, key = { it.categoryName }) { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stat.categoryName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${stat.count}件 · 投入¥${String.format("%.0f", stat.investment)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ProfitLossText(
                        profitLoss = ProfitLoss(
                            stat.investment,
                            stat.revenue,
                            stat.profit,
                            if (stat.investment > 0) (stat.profit / stat.investment) * 100 else 0.0
                        )
                    )
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
