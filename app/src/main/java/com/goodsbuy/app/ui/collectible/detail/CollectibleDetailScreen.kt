package com.goodsbuy.app.ui.collectible.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.components.StatusChip
import com.goodsbuy.app.ui.components.ProfitLossText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleDetailScreen(
    collectibleId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: CollectibleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(collectibleId) { viewModel.loadCollectible(collectibleId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.collectible?.name ?: "藏品详情") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = {
                    IconButton(onClick = onNavigateToEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                    IconButton(onClick = { viewModel.requestDelete() }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
                }
            )
        }
    ) { padding ->
        val collectible = uiState.collectible

        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                title = { Text("确认删除") },
                text = { Text("确定要删除「${collectible?.name}」吗？此操作不可撤销，其图片也会被删除。") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteCollectible(onDeleted = onNavigateBack) }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("取消") }
                }
            )
        }

        if (collectible == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
             Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                 // 快捷状态修改
                 Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                     Column(modifier = Modifier.padding(12.dp)) {
                         Text("点击修改状态", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(
                             modifier = Modifier.horizontalScroll(rememberScrollState()),
                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                         ) {
                             OrderStatus.entries.forEach { status ->
                                 FilterChip(
                                     selected = collectible.status == status,
                                     onClick = { viewModel.updateStatus(status) },
                                     label = { Text(status.displayName, style = MaterialTheme.typography.labelSmall) }
                                 )
                             }
                         }
                     }
                 }

                 Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("基础信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DetailRow("制品名称", collectible.name)
                        DetailRow("品类", collectible.category)
                        DetailRow("种类", collectible.type)
                        DetailRow("所属IP", collectible.ipName)
                        DetailRow("系列名称", collectible.seriesName)
                        DetailRow("角色/CP", collectible.characterTag)
                        if (collectible.remark.isNotBlank()) DetailRow("备注", collectible.remark)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("购入信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DetailRow("购买渠道", collectible.purchaseChannel)
                        DetailRow("店铺/卖家", collectible.purchaseShop)
                        DetailRow("入手单价", "¥${collectible.purchasePrice}")
                        DetailRow("购入数量", "${collectible.purchaseQuantity}")
                        DetailRow("购入运费", "¥${collectible.purchaseShipping}")
                        DetailRow("心理预期价", "¥${collectible.expectedPrice}")
                        DetailRow("总成本", "¥${collectible.purchasePrice * collectible.purchaseQuantity + collectible.purchaseShipping}")
                    }
                }

                if (collectible.sellPrice != null || collectible.sellDate != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("卖出信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            collectible.sellPrice?.let { DetailRow("售出单价", "¥$it") }
                            collectible.sellQuantity?.let { DetailRow("售出数量", "$it") }
                            if (collectible.isFreeShipping) {
                                DetailRow("运费", "包邮（卖家承担）")
                            } else {
                                collectible.sellShipping?.let { DetailRow("售出运费", "¥$it") }
                            }
                            collectible.sellDate?.let {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                DetailRow("售出日期", sdf.format(java.util.Date(it)))
                            }
                            collectible.buyerInfo?.let { if (it.isNotBlank()) DetailRow("买家信息", it) }
                            collectible.sellRemark?.let { if (it.isNotBlank()) DetailRow("售出备注", it) }
                        }
                    }
                }

                if (uiState.profitLoss != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("盈亏情况", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            DetailRow("总营收", "¥${uiState.profitLoss!!.totalRevenue}")
                            DetailRow("盈亏金额", "", profitLoss = uiState.profitLoss)
                            DetailRow("盈亏比例", "${String.format("%.1f", uiState.profitLoss!!.profitRate)}%", profitLoss = uiState.profitLoss)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, profitLoss: com.goodsbuy.app.domain.model.ProfitLoss? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (profitLoss != null) {
            ProfitLossText(profitLoss = profitLoss)
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
