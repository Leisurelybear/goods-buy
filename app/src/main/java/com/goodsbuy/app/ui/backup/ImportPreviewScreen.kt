package com.goodsbuy.app.ui.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.backup.ImportMode
import com.goodsbuy.app.util.CollectibleRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    preview: ImportPreviewResult,
    importMode: ImportMode,
    onModeChange: (ImportMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isImporting: Boolean = false,
    importProgress: Float = 0f
) {
    val modes = listOf(
        ImportMode.SKIP to "跳过重复",
        ImportMode.ADD to "新增重复",
        ImportMode.OVERWRITE to "覆盖重复"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览") },
                navigationIcon = {
                    IconButton(onClick = onDismiss, enabled = !isImporting) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("总计", style = MaterialTheme.typography.bodyLarge)
                        Text("${preview.total} 条", style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("将导入/覆盖", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${preview.willImport} 条", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("将跳过", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${preview.willSkip} 条", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("重复项处理", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { (mode, label) ->
                            FilterChip(
                                selected = importMode == mode,
                                onClick = { onModeChange(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // List of items
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(preview.items, key = { index, _ -> index }) { _, item ->
                    ImportPreviewRow(
                        record = item.record,
                        action = item.action,
                        reason = item.reason
                    )
                }
            }

            // Confirm button / progress
            if (isImporting) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val percent = (importProgress * 100).toInt()
                    Text(
                        "正在导入… $percent%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { importProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = preview.willImport > 0
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("确认导入 ${preview.willImport} 条藏品")
                }
            }
        }
    }
}

@Composable
fun ImportPreviewRow(record: CollectibleRecord, action: ImportAction, reason: String) {
    val (bgColor, icon, tint) = when (action) {
        ImportAction.IMPORT -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.primary
        )
        ImportAction.OVERWRITE -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            Icons.Default.Sync,
            MaterialTheme.colorScheme.tertiary
        )
        ImportAction.SKIP -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            Icons.Default.Close,
            MaterialTheme.colorScheme.error
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        record.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.ipName.isNotEmpty()) {
                        Text(
                            record.ipName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (record.seriesName.isNotEmpty()) {
                        Text(
                            record.seriesName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint
                )
            }
            if (reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (action == ImportAction.SKIP) MaterialTheme.colorScheme.error else tint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (action == ImportAction.SKIP) MaterialTheme.colorScheme.error else tint
                    )
                }
            }
        }
    }
}
