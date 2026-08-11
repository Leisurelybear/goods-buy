package com.goodsbuy.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.preferences.GridPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(preferencesRepository: PreferencesRepository) {
    var showSettings by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }

    LaunchedEffect(preferencesRepository) {
        prefs = preferencesRepository.preferencesState.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    } else {
                        IconButton(onClick = {
                            showSettings = false
                            preferencesRepository.save(prefs)
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "完成")
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
                // Settings section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("显示设置", style = MaterialTheme.typography.titleMedium)

                        // Columns setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("每行展示数量", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (prefs.columns > 1) {
                                        prefs = prefs.copy(columns = prefs.columns - 1)
                                        preferencesRepository.save(prefs)
                                    }
                                }) {
                                    Text("-", style = MaterialTheme.typography.headlineMedium)
                                }
                                Text("${prefs.columns}", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    if (prefs.columns < 4) {
                                        prefs = prefs.copy(columns = prefs.columns + 1)
                                        preferencesRepository.save(prefs)
                                    }
                                }) {
                                    Text("+", style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }

                        // Card size setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("卡片大小", style = MaterialTheme.typography.bodyLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    if (prefs.cardSize > 100) {
                                        prefs = prefs.copy(cardSize = prefs.cardSize - 20)
                                        preferencesRepository.save(prefs)
                                    }
                                }) {
                                    Text("-", style = MaterialTheme.typography.headlineMedium)
                                }
                                Text("${prefs.cardSize}dp", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = {
                                    if (prefs.cardSize < 200) {
                                        prefs = prefs.copy(cardSize = prefs.cardSize + 20)
                                        preferencesRepository.save(prefs)
                                    }
                                }) {
                                    Text("+", style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }

                        HorizontalDivider()

                        // Info visibility toggles
                        SettingToggleRow(
                            label = "显示名称",
                            checked = prefs.showName,
                            onCheckedChange = {
                                prefs = prefs.copy(showName = it)
                                preferencesRepository.save(prefs)
                            }
                        )
                        SettingToggleRow(
                            label = "显示价格",
                            checked = prefs.showPrice,
                            onCheckedChange = {
                                prefs = prefs.copy(showPrice = it)
                                preferencesRepository.save(prefs)
                            }
                        )
                        SettingToggleRow(
                            label = "显示状态",
                            checked = prefs.showStatus,
                            onCheckedChange = {
                                prefs = prefs.copy(showStatus = it)
                                preferencesRepository.save(prefs)
                            }
                        )
                    }
                }
            } else {
                // Normal profile section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("设置", style = MaterialTheme.typography.bodyLarge)
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
