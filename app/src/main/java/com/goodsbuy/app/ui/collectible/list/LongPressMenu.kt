package com.goodsbuy.app.ui.collectible.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressMenu(
    state: LongPressMenuState,
    onDismiss: () -> Unit,
    onQuickStatus: (OrderStatus) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onBatchSelect: () -> Unit,
    showBatchSelect: Boolean = true
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = state.collectible.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "快速修改状态",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                items(OrderStatus.entries) { status ->
                    FilterChip(
                        selected = state.collectible.status == status,
                        onClick = { onQuickStatus(status); onDismiss() },
                        label = { Text(status.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            MenuRow(icon = Icons.Default.Edit, label = "编辑", onClick = { onEdit(); onDismiss() })
            MenuRow(icon = Icons.Default.ContentCopy, label = "复制藏品", onClick = { onDuplicate(); onDismiss() })
            MenuRow(icon = Icons.Default.Delete, label = "删除", onClick = { onDelete(); onDismiss() }, isDanger = true)
            if (showBatchSelect) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                MenuRow(
                    icon = Icons.Default.SelectAll,
                    label = "批量选择",
                    onClick = { onBatchSelect() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
