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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.BuildConfig
import com.goodsbuy.app.ui.backup.ImportPreviewScreen
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ListRowItem
import kotlinx.coroutines.launch
import com.goodsbuy.app.ui.preferences.PreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToForm: (Long?) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
var showSettings by remember { mutableStateOf(false) }
    var showDrafts by remember { mutableStateOf(false) }
    var showDeleteLogsDialog by remember { mutableStateOf(false) }
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
    val drafts by viewModel.drafts.collectAsState()

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
                title = { Text(if (showSettings) "显示设置" else if (showDrafts) "草稿箱" else "我的") },
                navigationIcon = {
                    if (showSettings || showDrafts) {
                        IconButton(onClick = { showSettings = false; showDrafts = false }) {
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

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("草稿自动保存间隔", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "停止编辑后保存，推荐 0.5 秒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val delayOptions = PreferencesRepository.DRAFT_AUTO_SAVE_DELAY_OPTIONS.map { delayMillis ->
                                delayMillis to if (delayMillis == 500L) "0.5 秒" else "${delayMillis / 1_000} 秒"
                            }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                delayOptions.forEachIndexed { index, (delayMillis, label) ->
                                    SegmentedButton(
                                        selected = prefs.draftAutoSaveDelayMillis == delayMillis,
                                        onClick = {
                                            prefs = prefs.copy(draftAutoSaveDelayMillis = delayMillis)
                                            preferencesRepository.save(prefs)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index, delayOptions.size),
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        SettingToggleRow("首页多图自动轮询", prefs.homeImageAutoRotate) {
                            prefs = prefs.copy(homeImageAutoRotate = it)
                            preferencesRepository.save(prefs)
                        }
                        Text(
                            "开启后，首页当前屏幕中的多图片藏品会自动切换封面",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("轮询间隔", style = MaterialTheme.typography.bodyLarge)
                                Text("每张图片停留的时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (prefs.homeImageRotationIntervalSeconds > 1) {
                                            prefs = prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds - 1)
                                            preferencesRepository.save(prefs)
                                        }
                                    },
                                    enabled = prefs.homeImageRotationIntervalSeconds > 1
                                ) { Text("−", style = MaterialTheme.typography.headlineMedium) }
                                Text("${prefs.homeImageRotationIntervalSeconds} 秒", style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = {
                                        if (prefs.homeImageRotationIntervalSeconds < 60) {
                                            prefs = prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds + 1)
                                            preferencesRepository.save(prefs)
                                        }
                                    },
                                    enabled = prefs.homeImageRotationIntervalSeconds < 60
                                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                            }
                        }

                        HorizontalDivider()

                        Text("图鉴入口", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "选择图鉴入口显示的位置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("藏品柜", "我的").forEachIndexed { index, label ->
                                val selected = if (index == 0) prefs.galleryEntryHome else !prefs.galleryEntryHome
                                SegmentedButton(
                                    selected = selected,
                                    onClick = {
                                        prefs = prefs.copy(galleryEntryHome = index == 0)
                                        preferencesRepository.save(prefs)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                    label = { Text(label) }
                                )
                            }
                        }

                        SettingToggleRow("启用日志记录", prefs.loggingEnabled) {
                            prefs = prefs.copy(loggingEnabled = it); preferencesRepository.save(prefs)
                            com.goodsbuy.app.util.AppLogger.setEnabled(it)
                        }
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                            val logFile = com.goodsbuy.app.util.AppLogger.getLogFile()
                            val crashFile = com.goodsbuy.app.util.AppLogger.getCrashLogFile()
                            if (logFile != null || crashFile != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val files = listOfNotNull(logFile, crashFile)
                                        val uris = ArrayList(files.map {
                                            androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
                                        })
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "*/*"
                                            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
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
Text("分享 app.log / crash.log", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showDeleteLogsDialog = true },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("删除日志", style = MaterialTheme.typography.bodyLarge)
Text("删除后仍会继续记录新日志", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else if (showDrafts) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (drafts.isEmpty()) {
                            Text("暂无草稿", style = MaterialTheme.typography.bodyLarge)
                            Text("在添加或编辑藏品时离开页面，内容会自动保存在这里。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            drafts.forEach { draft ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(draft.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(if (draft.id == null) "新藏品草稿" else "编辑藏品草稿", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { onNavigateToForm(draft.id) }) { Text("继续编辑") }
                                    IconButton(onClick = { viewModel.deleteDraft(draft.key) }) {
                                        Icon(Icons.Default.Close, contentDescription = "删除草稿")
                                    }
                                }
                                if (draft != drafts.last()) HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                // Main profile screen
                HeroHeader(
                    title = "我的",
                    subtitle = "谷的拜 · v${BuildConfig.VERSION_NAME}"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        ListRowItem(
                            title = "设置",
                            subtitle = "外观、数据、日志等",
                            trailing = {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.graphicsLayer(rotationZ = 180f)
                                )
                            },
                            onClick = { showSettings = true }
                        )
                        HorizontalDivider()
                        ListRowItem(
                            title = "草稿箱",
                            subtitle = "${drafts.size} 条未完成草稿",
                            onClick = { showDrafts = true; viewModel.refreshDrafts() }
                        )
                        HorizontalDivider()
                        ListRowItem(
                            title = "导出备份",
                            subtitle = "导出所有藏品及图片",
                            onClick = {
                                val fileName = "谷的拜备份_${System.currentTimeMillis()}.zip"
                                exportLauncher.launch(fileName)
                            }
                        )
                        HorizontalDivider()
                        ListRowItem(
                            title = "导入备份",
                            subtitle = "从ZIP文件恢复数据",
                            onClick = { importLauncher.launch("application/zip") }
                        )
                        if (!prefs.galleryEntryHome) {
                            HorizontalDivider()
                            ListRowItem(
                                title = "图鉴模式",
                                subtitle = "按 IP/系列分类查看",
                                onClick = { onNavigateToGallery() }
                            )
                        }
                        HorizontalDivider()
                        ListRowItem(
                            title = "关于谷的拜",
                            subtitle = "v${BuildConfig.VERSION_NAME}",
                            onClick = {}
                        )
                    }
                }
            }
        }
    }

    if (showDeleteLogsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLogsDialog = false },
            title = { Text("删除日志") },
            text = { Text("将删除 app.log 和 crash.log 以释放空间，删除后仍会继续记录新日志。") },
            confirmButton = {
                TextButton(onClick = {
                    com.goodsbuy.app.util.AppLogger.deleteLogs()
                    showDeleteLogsDialog = false
                    scope.launch { snackbarHostState.showSnackbar("日志已删除") }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLogsDialog = false }) { Text("取消") }
            }
        )
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
