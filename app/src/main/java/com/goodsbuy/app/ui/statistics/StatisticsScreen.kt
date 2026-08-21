package com.goodsbuy.app.ui.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.ProfitLoss
import com.goodsbuy.app.ui.components.AnimatedInt
import com.goodsbuy.app.ui.components.GradientCard
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ProfitLossText
import com.goodsbuy.app.ui.components.SectionHeader
import com.goodsbuy.app.ui.components.StatNumber
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroHeader(
                title = "统计",
                subtitle = "${uiState.summary.totalCount} 件藏品",
                bottomContent = {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Text("累计盈亏", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        Text(
                            text = buildString {
                                if (uiState.summary.totalProfit >= 0) append("+")
                                append("\u00a5")
                                append(String.format("%.0f", uiState.summary.totalProfit))
                            },
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFFFF8FAB), Color(0xFFFFB3D9))
                ) {
                    Text("总投入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                    StatNumber(value = uiState.summary.totalInvestment, color = MaterialTheme.colorScheme.onPrimary, decimals = 0)
                }
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFFB388FF), Color(0xFF9B5CFF))
                ) {
                    Text("总营收", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                    StatNumber(value = uiState.summary.totalRevenue, color = MaterialTheme.colorScheme.onPrimary, decimals = 0)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(ProfitGreen, Color(0xFF66BB6A))
                ) {
                    Text("累计盈亏", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                    StatNumber(value = uiState.summary.totalProfit, color = MaterialTheme.colorScheme.onPrimary, decimals = 0)
                }
                GradientCard(
                    modifier = Modifier.weight(1f),
                    gradient = listOf(Color(0xFF42A5F5), Color(0xFF5C6BC0))
                ) {
                    Text("持仓市值", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                    StatNumber(value = uiState.summary.holdingValue, color = MaterialTheme.colorScheme.onPrimary, decimals = 0)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("藏品总数", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            AnimatedInt(
                                targetValue = uiState.summary.totalCount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                " (持有${uiState.summary.ownedCount}/已售${uiState.summary.soldCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("盈亏比例", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${String.format("%.1f", uiState.summary.totalProfitRate)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.summary.totalProfit >= 0) ProfitGreen else LossRed
                        )
                    }
                }
            }
        }

        item { MonthlyTrendChart(stats = uiState.monthlyStats) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "分类统计")
                IconButton(onClick = { showFilter = !showFilter }) {
                    Icon(Icons.Default.FilterList, contentDescription = "筛选状态")
                }
            }

            AnimatedVisibility(
                visible = showFilter,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = { selectedStatus = null },
                            label = { Text("全部") }
                        )
                    }
                    items(OrderStatus.entries) { status ->
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            "${stat.count}件 · 投入\u00a5${String.format("%.0f", stat.investment)}",
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
