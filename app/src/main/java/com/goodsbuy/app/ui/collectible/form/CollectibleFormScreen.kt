package com.goodsbuy.app.ui.collectible.form

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.model.StorageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleFormScreen(
    collectibleId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: CollectibleFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addImagePath(uri.toString())
        }
    }

    LaunchedEffect(collectibleId) {
        if (collectibleId != null) viewModel.loadCollectible(collectibleId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (collectibleId != null) "编辑藏品" else "添加藏品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }, enabled = uiState.name.isNotBlank()) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
             // 图片区域
             Text("藏品图片", style = MaterialTheme.typography.titleMedium)
             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 Button(onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                     Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                     Spacer(modifier = Modifier.width(4.dp))
                     Text("添加图片")
                 }
                 Text("${uiState.imagePaths.size}/9", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
             }
             if (uiState.imagePaths.isNotEmpty()) {
                 Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     uiState.imagePaths.forEachIndexed { index, path ->
                         androidx.compose.foundation.layout.Box {
                             AsyncImage(
                                 model = path,
                                 contentDescription = null,
                                 modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                 contentScale = ContentScale.Crop
                             )
                             IconButton(
                                 onClick = { viewModel.removeImagePath(index) },
                                 modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).size(24.dp)
                             ) {
                                 Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                             }
                         }
                     }
                 }
             }

             Text("基础信息", style = MaterialTheme.typography.titleMedium)
             OutlinedTextField(value = uiState.name, onValueChange = { viewModel.updateField("name", it) }, label = { Text("制品名称*") }, modifier = Modifier.fillMaxWidth())
             OutlinedTextField(value = uiState.category, onValueChange = { viewModel.updateField("category", it) }, label = { Text("品类") }, modifier = Modifier.fillMaxWidth())
             OutlinedTextField(value = uiState.ipName, onValueChange = { viewModel.updateField("ipName", it) }, label = { Text("所属IP") }, modifier = Modifier.fillMaxWidth())
             OutlinedTextField(value = uiState.seriesName, onValueChange = { viewModel.updateField("seriesName", it) }, label = { Text("系列名称") }, modifier = Modifier.fillMaxWidth())
             OutlinedTextField(value = uiState.characterTag, onValueChange = { viewModel.updateField("characterTag", it) }, label = { Text("角色/CP") }, modifier = Modifier.fillMaxWidth())

             Text("购入信息", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = uiState.purchasePrice, onValueChange = { viewModel.updateField("purchasePrice", it) }, label = { Text("入手单价") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.purchaseQuantity, onValueChange = { viewModel.updateField("purchaseQuantity", it) }, label = { Text("购入数量") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.purchaseShipping, onValueChange = { viewModel.updateField("purchaseShipping", it) }, label = { Text("购入运费") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.expectedPrice, onValueChange = { viewModel.updateField("expectedPrice", it) }, label = { Text("心理预期价") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.purchaseChannel, onValueChange = { viewModel.updateField("purchaseChannel", it) }, label = { Text("购买渠道") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = uiState.purchaseShop, onValueChange = { viewModel.updateField("purchaseShop", it) }, label = { Text("店铺/卖家") }, modifier = Modifier.fillMaxWidth())

            Text("状态", style = MaterialTheme.typography.titleMedium)
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(value = uiState.status.displayName, onValueChange = {}, readOnly = true, label = { Text("订单状态") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    OrderStatus.entries.forEach { status ->
                        DropdownMenuItem(text = { Text(status.displayName) }, onClick = { viewModel.updateStatus(status); statusExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = uiState.remark, onValueChange = { viewModel.updateField("remark", it) }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }
}
