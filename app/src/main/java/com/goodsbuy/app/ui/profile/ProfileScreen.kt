package com.goodsbuy.app.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.util.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesRepository: PreferencesRepository,
    onNavigateBack: () -> Unit = {},
    dao: CollectibleDao = hiltViewModel()
) {
    var showSettings by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = BackupManager.import(context, uri, dao)
            result.fold(
                onSuccess = { count ->
                    snackbarHostState.showSnackbar("成功导入 $count 条藏品")
                },
                onFailure = { e ->
                    snackbarHostState.showSnackbar("导入失败: ${e.message}")
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (showSettings) "显示设置" else "我的") },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = {
                            showSettings = false
                            preferencesRepository.save(prefs)
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
            } else {
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
                                scope.launch {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_TITLE, "谷的拜备份_${System.currentTimeMillis()}.zip")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "保存备份"))
                                }
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
