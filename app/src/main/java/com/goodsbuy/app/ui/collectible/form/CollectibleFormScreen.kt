package com.goodsbuy.app.ui.collectible.form

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleFormScreen(
    collectibleId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: CollectibleFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDraftSaved by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.draftSavedAt) {
        if (uiState.draftSavedAt > 0L) {
            showDraftSaved = true
            delay(1_500)
            showDraftSaved = false
        }
    }
    val draftSavedAlpha by animateFloatAsState(
        targetValue = if (showDraftSaved) 1f else 0f,
        animationSpec = tween(180),
        label = "draft_saved_alpha"
    )

var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var pendingCapturePath by remember { mutableStateOf<String?>(null) }
    var showImageSourceMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    BackHandler(enabled = editingImageIndex != null || pendingCapturePath != null) {
        if (pendingCapturePath != null) {
            pendingCapturePath?.let { viewModel.discardCapturedImage(it) }
            pendingCapturePath = null
        } else {
            editingImageIndex = null
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GalleryImagePickerContract(maxItems = 9)
    ) { uris -> viewModel.addImages(uris) }

    val cameraContract = remember { CameraCaptureContract() }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = cameraContract
    ) { uri -> if (uri != null) pendingCapturePath = uri.path }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(Unit)
        } else {
            scope.launch { snackbarHostState.showSnackbar("需要相机权限才能拍照") }
        }
    }

    LaunchedEffect(collectibleId) { viewModel.initialize(collectibleId) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSaveError()
        }
    }

    val hasSellInfo = uiState.sellPrice.isNotBlank() ||
        uiState.sellQuantity.isNotBlank() ||
        uiState.sellShipping.isNotBlank() ||
        uiState.isFreeShipping ||
        uiState.buyerInfo.isNotBlank() ||
        uiState.sellRemark.isNotBlank()
    val showSellSection = hasSellInfo || uiState.status in listOf(
        OrderStatus.LISTED, OrderStatus.SOLD, OrderStatus.GIFT, OrderStatus.LOST
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (collectibleId != null) "编辑藏品" else "添加藏品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    Row(
                        modifier = Modifier.width(64.dp).alpha(draftSavedAlpha),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("已保存", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving && !uiState.isSaved,
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                if (uiState.isSaved) Icons.Default.Check else Icons.Default.Save,
                                contentDescription = "保存",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.hasDraft) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("发现未完成草稿", style = MaterialTheme.typography.titleSmall)
                        Text("可以恢复上次编辑的内容，或直接丢弃。", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::restoreDraft) { Text("恢复草稿") }
                            OutlinedButton(onClick = viewModel::discardDraft) { Text("丢弃") }
                        }
                    }
                }
            }

             // 图片区域
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
             ) {
                 Column {
                      SectionHeader(title = "藏品图片")
                     Text(
                         "已选择 ${uiState.imagePaths.size}/9 张",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 }
FilledTonalButton(
                     onClick = { showImageSourceMenu = true },
                     enabled = uiState.imagePaths.size < 9
                 ) {
                     Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                     Spacer(modifier = Modifier.width(6.dp))
                     Text("添加图片")
                 }
             }
             AnimatedVisibility(
                 visible = uiState.imagePaths.isNotEmpty(),
                 enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                 exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
             ) {
                 Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     uiState.imagePaths.forEachIndexed { index, path ->
                         androidx.compose.foundation.layout.Box(
                             modifier = Modifier.clickable { editingImageIndex = index }
                         ) {
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

              SectionHeader(title = "基础信息")
             OutlinedTextField(
                 value = uiState.name,
                 onValueChange = { viewModel.updateField("name", it) },
                 label = { Text("制品名称") },
                 placeholder = { Text("例如：宝可梦卡牌 路卡利欧 SR") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true,
                 isError = uiState.fieldErrors.containsKey("name"),
                 supportingText = { uiState.fieldErrors["name"]?.let { Text(it) } }
             )
             OutlinedTextField(
                 value = uiState.category,
                 onValueChange = { viewModel.updateField("category", it) },
                 label = { Text("品类") },
                 placeholder = { Text("例如：卡牌、毛绒、手办") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true
             )
             OutlinedTextField(
                 value = uiState.type,
                 onValueChange = { viewModel.updateField("type", it) },
                 label = { Text("种类") },
                 placeholder = { Text("例如：SR、常规款、限定款") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true
             )
             OutlinedTextField(
                 value = uiState.ipName,
                 onValueChange = { viewModel.updateField("ipName", it) },
                 label = { Text("所属IP") },
                 placeholder = { Text("例如：宝可梦、初音未来") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true
             )
             OutlinedTextField(
                 value = uiState.seriesName,
                 onValueChange = { viewModel.updateField("seriesName", it) },
                 label = { Text("系列名称") },
                 placeholder = { Text("例如：剑盾强化包、雪初音系列") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true
             )
             OutlinedTextField(
                 value = uiState.characterTag,
                 onValueChange = { viewModel.updateField("characterTag", it) },
                 label = { Text("角色/CP") },
                 placeholder = { Text("例如：路卡利欧、皮卡丘") },
                 modifier = Modifier.fillMaxWidth(),
                 singleLine = true
             )
             NumTextField(
                 value = uiState.purchasePrice,
                 onValueChange = { viewModel.updateField("purchasePrice", it) },
                 label = "购买单价",
                 placeholder = "例如：5000",
                 isDecimal = true,
                 isError = uiState.fieldErrors.containsKey("purchasePrice"),
                 errorText = uiState.fieldErrors["purchasePrice"],
                 modifier = Modifier.fillMaxWidth()
             )
             NumTextField(
                 value = uiState.purchaseQuantity,
                 onValueChange = { viewModel.updateField("purchaseQuantity", it) },
                 label = "购入数量",
                 placeholder = "例如：1",
                 modifier = Modifier.fillMaxWidth(),
                 isError = uiState.fieldErrors.containsKey("purchaseQuantity"),
                 errorText = uiState.fieldErrors["purchaseQuantity"]
             )
             NumTextField(
                 value = uiState.purchaseShipping,
                 onValueChange = { viewModel.updateField("purchaseShipping", it) },
                 label = "购入运费",
                 placeholder = "例如：500",
                 isDecimal = true,
                 modifier = Modifier.fillMaxWidth(),
                 isError = uiState.fieldErrors.containsKey("purchaseShipping"),
                 errorText = uiState.fieldErrors["purchaseShipping"]
             )
             NumTextField(
                 value = uiState.expectedPrice,
                 onValueChange = { viewModel.updateField("expectedPrice", it) },
                 label = "心理预期价",
                 placeholder = "例如：7000",
                 isDecimal = true,
                 modifier = Modifier.fillMaxWidth(),
                 isError = uiState.fieldErrors.containsKey("expectedPrice"),
                 errorText = uiState.fieldErrors["expectedPrice"]
             )
             OutlinedTextField(
                 value = uiState.purchaseChannel,
                 onValueChange = { viewModel.updateField("purchaseChannel", it) },
                 label = { Text("购买渠道") },
                 placeholder = { Text("例如：日本乐天、酷爱、闲鱼") },
                 modifier = Modifier.fillMaxWidth()
             )
             OutlinedTextField(
                 value = uiState.purchaseShop,
                 onValueChange = { viewModel.updateField("purchaseShop", it) },
                 label = { Text("店铺/卖家") },
                 placeholder = { Text("例如：宝可梦官方店铺、某鱼卖家XXX") },
                 modifier = Modifier.fillMaxWidth()
             )

             // 卖出信息 - animated visibility based on status
             AnimatedVisibility(
                 visible = showSellSection,
                 enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                 exit = shrinkVertically(tween(250)) + fadeOut(tween(250))
             ) {
                 Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                      SectionHeader(title = "卖出信息")
                     NumTextField(
                         value = uiState.sellPrice,
                         onValueChange = { viewModel.updateField("sellPrice", it) },
                         label = "售出单价",
                         placeholder = "例如：7000",
                         isDecimal = true,
                         modifier = Modifier.fillMaxWidth(),
                         isError = uiState.fieldErrors.containsKey("sellPrice"),
                         errorText = uiState.fieldErrors["sellPrice"]
                     )
                     NumTextField(
                         value = uiState.sellQuantity,
                         onValueChange = { viewModel.updateField("sellQuantity", it) },
                         label = "售出数量",
                         placeholder = "例如：1",
                         modifier = Modifier.fillMaxWidth(),
                         isError = uiState.fieldErrors.containsKey("sellQuantity"),
                         errorText = uiState.fieldErrors["sellQuantity"]
                     )
                     NumTextField(
                         value = uiState.sellShipping,
                         onValueChange = { viewModel.updateField("sellShipping", it) },
                         label = "售出运费",
                         placeholder = "例如：500（买家承担）",
                         isDecimal = true,
                         modifier = Modifier.fillMaxWidth(),
                         isError = uiState.fieldErrors.containsKey("sellShipping"),
                         errorText = uiState.fieldErrors["sellShipping"]
                     )
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                     ) {
                         Text("包邮（卖家承担运费）", style = MaterialTheme.typography.bodyMedium)
                         Switch(checked = uiState.isFreeShipping, onCheckedChange = { viewModel.updateFreeShipping(it) })
                     }
                     OutlinedTextField(
                         value = uiState.buyerInfo,
                         onValueChange = { viewModel.updateField("buyerInfo", it) },
                         label = { Text("买家信息") },
                         placeholder = { Text("例如：闲鱼用户XXX、微博@XXX") },
                         modifier = Modifier.fillMaxWidth()
                     )
                     OutlinedTextField(
                         value = uiState.sellRemark,
                         onValueChange = { viewModel.updateField("sellRemark", it) },
                         label = { Text("售出备注") },
                         placeholder = { Text("例如：已发货、买家确认收货") },
                         modifier = Modifier.fillMaxWidth(),
                         minLines = 2
                     )
                 }
             }

             SectionHeader(title = "状态")
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(value = uiState.status.displayName, onValueChange = {}, readOnly = true, label = { Text("订单状态") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    OrderStatus.entries.forEach { status ->
                        DropdownMenuItem(text = { Text(status.displayName) }, onClick = { viewModel.updateStatus(status); statusExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = uiState.remark,
                onValueChange = { viewModel.updateField("remark", it) },
                label = { Text("备注") },
                placeholder = { Text("例如：转赠给好友小葵，附赠原袋") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }

    val editingPath = editingImageIndex?.let { uiState.imagePaths.getOrNull(it) } ?: pendingCapturePath
    editingPath?.let { editPath ->
        EdgeFadeEditScreen(
            sourcePath = editPath,
            onCancel = {
                if (pendingCapturePath != null) {
                    viewModel.discardCapturedImage(editPath)
                    pendingCapturePath = null
                } else {
                    editingImageIndex = null
                }
            },
            onDone = { newPath ->
                if (pendingCapturePath != null) {
                    viewModel.addCapturedImage(newPath, editPath)
                    pendingCapturePath = null
                } else {
                    editingImageIndex?.let { index -> viewModel.replaceImagePath(index, newPath) }
                    editingImageIndex = null
                }
            }
        )
    }

    if (showImageSourceMenu) {
        ModalBottomSheet(onDismissRequest = { showImageSourceMenu = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showImageSourceMenu = false
                        imagePickerLauncher.launch(Unit)
                    }
                )
                if (hasCamera) {
                    ListItem(
                        headlineContent = { Text("拍照") },
                        leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceMenu = false
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        cameraLauncher.launch(Unit)
                                    } catch (e: Exception) {
                                        com.goodsbuy.app.util.AppLogger.e("Camera", "launch failed", e)
                                        scope.launch { snackbarHostState.showSnackbar("无法打开相机") }
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("未找到相机应用") }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
fun NumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isDecimal: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.isEmpty()) {
                onValueChange("")
            } else if (isDecimal) {
                if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
                    onValueChange(newValue)
                }
            } else {
                if (newValue.all { it.isDigit() }) {
                    onValueChange(newValue)
                }
            }
        },
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        modifier = modifier,
        singleLine = true,
        isError = isError,
        supportingText = { if (errorText != null) Text(errorText) }
    )
}
