package com.goodsbuy.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.backup.ImportAction
import com.goodsbuy.app.ui.backup.ImportPreviewResult
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.util.BackupManager
import com.goodsbuy.app.util.CollectibleRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    repository: CollectibleRepository = hiltViewModel(),
    dao: CollectibleDao = hiltViewModel()
) {
    var showSettings by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Import preview state
    var importPreview by remember { mutableStateOf<ImportPreviewResult?>(null) }
    var forceImportDuplicates by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importedUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importedUri = it }
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                importPreview = BackupManager.previewImport(context, uri, dao, forceImportDuplicates)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("解析备份文件失败: ${e.message}")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) {
            scope.launch { snackbarHostState.showSnackbar("取消导出") }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val collectibles = repository.getAllCollectibles().first()
                val success = BackupManager.export(context, collectibles, uri)
                if (success) {
                    snackbarHostState.showSnackbar("导出成功")
                } else {
                    snackbarHostState.showSnackbar("导出失败")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("导出失败: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showSettings) "显示设置" else if (importPreview != null) "导入预览" else "我的") },
                navigationIcon = {
                    if (showSettings || importPreview != null) {
                        IconButton(onClick = {
                            if (importPreview != null) importPreview = null
                            else showSettings = false
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSettings) {
                // Settings screen
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("显示设置", style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("每行展示数量", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (prefs.columns > 1) { prefs = prefs.copy(columns = prefs.columns - 1); preferencesRepository.save(prefs) }
                                }) { Text("-", style = MaterialTheme.typography.headlineMedium) }
                                Text("${prefs.columns}", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    if (prefs.columns < 4) { prefs = prefs.copy(columns = prefs.columns + 1); preferencesRepository.save(prefs) }
                                }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("卡片大小", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (prefs.cardSize > 100) { prefs = prefs.copy(cardSize = prefs.cardSize - 20); preferencesRepository.save(prefs) }
                                }) { Text("-", style = MaterialTheme.typography.headlineMedium) }
                                Text("${prefs.cardSize}dp", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    if (prefs.cardSize < 200) { prefs = prefs.copy(cardSize = prefs.cardSize + 20); preferencesRepository.save(prefs) }
                                }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                            }
                        }

                        HorizontalDivider()

                        SettingToggleRow("显示名称", prefs.showName) { prefs = prefs.copy(showName = it); preferencesRepository.save(prefs) }
                        SettingToggleRow("显示价格", prefs.showPrice) { prefs = prefs.copy(showPrice = it); preferencesRepository.save(prefs) }
                        SettingToggleRow("显示状态", prefs.showStatus) { prefs = prefs.copy(showStatus = it); preferencesRepository.save(prefs) }
                    }
                }
            } else if (importPreview != null && importedUri != null) {
                // Import preview screen
                ImportPreviewScreen(
                    preview = importPreview!!,
                    forceImportDuplicates = forceImportDuplicates,
                    onToggleForceImport = { forceImportDuplicates = it },
                    onConfirm = {
                        isImporting = true
                        scope.launch {
                            val uri = importedUri!!
                            val result = BackupManager.import(context, uri, dao, forceImportDuplicates)
                            isImporting = false
                            importPreview = null
                            importedUri = null
                            result.fold(
                                onSuccess = { count ->
                                    snackbarHostState.showSnackbar("成功导入 $count 条藏品")
                                },
                                onFailure = { e ->
                                    snackbarHostState.showSnackbar("导入失败: ${e.message}")
                                }
                            )
                        }
                    },
                    onDismiss = { importPreview = null; importedUri = null }
                )
            } else {
                // Main profile screen
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showSettings = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("设置", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer(rotationZ = 180f))
                        }
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val fileName = "谷的拜备份_${System.currentTimeMillis()}.zip"
                                exportLauncher.launch(fileName)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("导出备份", style = MaterialTheme.typography.bodyLarge)
                                Text("导出所有藏品及图片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                importLauncher.launch("application/zip")
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("导入备份", style = MaterialTheme.typography.bodyLarge)
                                Text("从ZIP文件恢复数据", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("关于谷的拜", style = MaterialTheme.typography.bodyLarge)
                                Text("v1.0.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportPreviewScreen(
    preview: ImportPreviewResult,
    forceImportDuplicates: Boolean,
    onToggleForceImport: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("导入预览", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("总计", style = MaterialTheme.typography.bodyLarge)
                    Text("${preview.total} 条", style = MaterialTheme.typography.bodyLarge)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("将导入", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${preview.willImport} 条", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("将跳过", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${preview.willSkip} 条", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Toggle for duplicates
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("强制导入重复项（自动添加后缀）", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = forceImportDuplicates, onCheckedChange = onToggleForceImport)
        }

        // List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(preview.items) { item ->
                ImportPreviewRow(item.record, item.action, item.reason)
            }
        }

        // Confirm button
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

@Composable
fun ImportPreviewRow(record: CollectibleRecord, action: ImportAction, reason: String) {
    val bgColor = if (action == ImportAction.IMPORT) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (record.ipName.isNotEmpty()) {
                        Text(record.ipName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (record.seriesName.isNotEmpty()) {
                        Text(record.seriesName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (action == ImportAction.IMPORT) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            if (reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Text(reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
