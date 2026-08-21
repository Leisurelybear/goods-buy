package com.goodsbuy.app.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import com.goodsbuy.app.BuildConfig
import com.goodsbuy.app.ui.backup.ImportPreviewScreen
import com.goodsbuy.app.ui.components.HeroHeader
import com.goodsbuy.app.ui.components.ListRowItem
import com.goodsbuy.app.ui.components.SectionHeader
import com.goodsbuy.app.ui.preferences.GridPreferences
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.theme.AppThemes
import com.goodsbuy.app.util.AppLogger
import kotlinx.coroutines.launch

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
                title = { Text(if (showSettings) "设置" else if (showDrafts) "草稿箱" else "我的") },
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
                SettingsContent(
                    prefs = prefs,
                    onPrefsChange = { prefs = it; preferencesRepository.save(it) },
                    onDeleteLogsRequest = { showDeleteLogsDialog = true }
                )
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
                            subtitle = "外观 · 首页 · 图鉴 · 日志",
                            trailing = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                            subtitle = "v${BuildConfig.VERSION_NAME}"
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
                    AppLogger.deleteLogs()
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
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionHeader(title = title)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    ListRowItem(
        title = label,
        subtitle = subtitle,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun StepperRow(
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    ListRowItem(
        title = title,
        subtitle = subtitle,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, enabled = enabled && canDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "减少", tint = MaterialTheme.colorScheme.primary)
                }
                Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onIncrease, enabled = enabled && canIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "增加", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    prefs: GridPreferences,
    onPrefsChange: (GridPreferences) -> Unit,
    onDeleteLogsRequest: () -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    SettingsGroup(title = "外观") {
        ListRowItem(
            title = "主题",
            subtitle = "当前：${AppThemes.byId(prefs.themeId).label}",
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { showThemeDialog = true }
        )
        HorizontalDivider()
        StepperRow(
            title = "每行展示数量",
            value = "${prefs.columns}",
            canDecrease = prefs.columns > 1,
            canIncrease = prefs.columns < 4,
            onDecrease = { onPrefsChange(prefs.copy(columns = prefs.columns - 1)) },
            onIncrease = { onPrefsChange(prefs.copy(columns = prefs.columns + 1)) }
        )
        HorizontalDivider()
        StepperRow(
            title = "卡片大小",
            subtitle = "图鉴横向卡片宽度；藏品柜卡片随列数自适应",
            value = "${prefs.cardSize}dp",
            canDecrease = prefs.cardSize > 100,
            canIncrease = prefs.cardSize < 200,
            onDecrease = { onPrefsChange(prefs.copy(cardSize = (prefs.cardSize - 20).coerceAtLeast(100))) },
            onIncrease = { onPrefsChange(prefs.copy(cardSize = (prefs.cardSize + 20).coerceAtMost(200))) }
        )
        HorizontalDivider()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("字体大小", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("小", "中", "大").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = prefs.fontSize == idx,
                        onClick = { onPrefsChange(prefs.copy(fontSize = idx)) },
                        label = { Text(label) }
                    )
                }
            }
        }
        HorizontalDivider()
        SettingSwitchRow("显示名称", prefs.showName) { onPrefsChange(prefs.copy(showName = it)) }
        HorizontalDivider()
        SettingSwitchRow("显示价格", prefs.showPrice) { onPrefsChange(prefs.copy(showPrice = it)) }
        HorizontalDivider()
        SettingSwitchRow("显示状态", prefs.showStatus) { onPrefsChange(prefs.copy(showStatus = it)) }
    }

    SettingsGroup(title = "首页行为") {
        SettingSwitchRow("显示排序栏", prefs.showSortControl) { onPrefsChange(prefs.copy(showSortControl = it)) }
        HorizontalDivider()
        SettingSwitchRow(
            "多图自动轮询",
            prefs.homeImageAutoRotate,
            subtitle = "开启后，首页当前屏幕中的多图片藏品会自动切换封面"
        ) { onPrefsChange(prefs.copy(homeImageAutoRotate = it)) }
        HorizontalDivider()
        StepperRow(
            title = "轮询间隔",
            subtitle = "每张图片停留的时间",
            value = "${prefs.homeImageRotationIntervalSeconds} 秒",
            canDecrease = prefs.homeImageRotationIntervalSeconds > 1,
            canIncrease = prefs.homeImageRotationIntervalSeconds < 60,
            onDecrease = { onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds - 1)) },
            onIncrease = { onPrefsChange(prefs.copy(homeImageRotationIntervalSeconds = prefs.homeImageRotationIntervalSeconds + 1)) },
            enabled = prefs.homeImageAutoRotate
        )
    }

    SettingsGroup(title = "编辑与草稿") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("草稿自动保存间隔", style = MaterialTheme.typography.titleMedium)
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
                        onClick = { onPrefsChange(prefs.copy(draftAutoSaveDelayMillis = delayMillis)) },
                        shape = SegmentedButtonDefaults.itemShape(index, delayOptions.size),
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    SettingsGroup(title = "图鉴") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("入口位置", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("藏品柜", "我的").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = if (index == 0) prefs.galleryEntryHome else !prefs.galleryEntryHome,
                        onClick = { onPrefsChange(prefs.copy(galleryEntryHome = index == 0)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    val ctx = LocalContext.current
    val logFile = AppLogger.getLogFile()
    val crashFile = AppLogger.getCrashLogFile()
    SettingsGroup(title = "日志") {
        SettingSwitchRow("启用记录", prefs.loggingEnabled, subtitle = "记录运行日志用于排查问题") {
            onPrefsChange(prefs.copy(loggingEnabled = it))
            AppLogger.setEnabled(it)
        }
        if (logFile != null || crashFile != null) {
            HorizontalDivider()
            ListRowItem(
                title = "导出日志",
                subtitle = "分享 app.log / crash.log",
                onClick = {
                    val files = listOfNotNull(logFile, crashFile)
                    val uris = ArrayList(files.map {
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)
                    })
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
                }
            )
            HorizontalDivider()
            ListRowItem(
                title = "删除日志",
                subtitle = "删除后仍会继续记录新日志",
                onClick = onDeleteLogsRequest
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题") },
            text = {
                Column {
                    AppThemes.all.forEach { theme ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onPrefsChange(prefs.copy(themeId = theme.id))
                                showThemeDialog = false
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.themeId == theme.id,
                                onClick = {
                                    onPrefsChange(prefs.copy(themeId = theme.id))
                                    showThemeDialog = false
                                }
                            )
                            Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("完成") }
            }
        )
    }
}
