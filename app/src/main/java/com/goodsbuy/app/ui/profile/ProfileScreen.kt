package com.goodsbuy.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.ui.backup.ImportPreviewScreen
import kotlinx.coroutines.launch
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var showSettings by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImportedUri(it)
            viewModel.previewImport(it)
        }
    }

    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(
            outputUri = uri,
            onSuccess = { scope.launch { snackbarHostState.showSnackbar("导出成功") } },
            onFailure = { message -> scope.launch { snackbarHostState.showSnackbar("导出失败: $message") } }
        )
    }

    // Collect state from ViewModel
    val currentImportPreview by viewModel.importPreview.collectAsState()
    val currentImportMode by viewModel.importMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    // Import preview is a full-screen surface with its own TopAppBar — render it
    // directly to avoid a double title bar (ProfileScreen's + ImportPreviewScreen's).
    if (currentImportPreview != null) {
        ImportPreviewScreen(
            preview = currentImportPreview!!,
            importMode = currentImportMode,
            onModeChange = { viewModel.setImportMode(it) },
            onConfirm = {
                scope.launch {
                    viewModel.confirmImport(
                        onSuccess = { count -> snackbarHostState.showSnackbar("成功导入 $count 条藏品") },
                        onFailure = { message -> snackbarHostState.showSnackbar("导入失败: $message") }
                    )
                }
            },
            onDismiss = { viewModel.clearPreview() },
            isImporting = isImporting,
            importProgress = importProgress
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showSettings) "显示设置" else "我的") },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(16.dp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("字体大小", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val labels = listOf("小", "中", "大")
                                labels.forEachIndexed { idx, label ->
                                    FilterChip(
                                        selected = prefs.fontSize == idx,
                                        onClick = { prefs = prefs.copy(fontSize = idx); preferencesRepository.save(prefs) },
                                        label = { Text(label) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        SettingToggleRow("显示名称", prefs.showName) { prefs = prefs.copy(showName = it); preferencesRepository.save(prefs) }
                        SettingToggleRow("显示价格", prefs.showPrice) { prefs = prefs.copy(showPrice = it); preferencesRepository.save(prefs) }
                        SettingToggleRow("显示状态", prefs.showStatus) { prefs = prefs.copy(showStatus = it); preferencesRepository.save(prefs) }
                        SettingToggleRow("显示排序栏", prefs.showSortControl) { prefs = prefs.copy(showSortControl = it); preferencesRepository.save(prefs) }

                        HorizontalDivider()

                        SettingToggleRow("启用日志记录", prefs.loggingEnabled) {
                            prefs = prefs.copy(loggingEnabled = it); preferencesRepository.save(prefs)
                            com.goodsbuy.app.util.AppLogger.setEnabled(it)
                        }
                        if (prefs.loggingEnabled) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            val logFile = com.goodsbuy.app.util.AppLogger.getLogFile()
                            if (logFile != null && logFile.exists()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            ctx, "${ctx.packageName}.fileprovider", logFile
                                        )
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        ctx.startActivity(android.content.Intent.createChooser(shareIntent, "分享日志文件"))
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("导出日志", style = MaterialTheme.typography.bodyLarge)
                                        Text("分享或查看 app.log", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
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
                                Text("v1.0.2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
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
