# 添加照片支持直接拍照 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在添加藏品页支持调用系统相机拍照，拍完立即进入「图片渐隐」编辑页，编辑确认后入列表、取消则丢弃。

**Architecture:** 表单页持有独立的待处理拍摄状态 `pendingCapturePath`，与现有 `editingImageIndex` 并列驱动编辑覆盖层（二选一显示，`EdgeFadeEditScreen` 零改动）。新增 `CameraCaptureContract`（`ACTION_IMAGE_CAPTURE` + FileProvider `EXTRA_OUTPUT` 写 `filesDir/images/{UUID}.jpg`）。确认时 `addCapturedImage(resultPath, sourcePath)` 入列表并在烘焙后清理拍摄原图；取消时 `discardCapturedImage` 删除文件。Manifest/FileProvider 基础设施已就绪，无新依赖、无运行时权限。

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (ModalBottomSheet), Activity Result API, androidx.core FileProvider。

---

## 构建命令约定

所有 Gradle 命令必须使用 JDK 17 前缀并加显式超时（PowerShell）：

```bash
$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat <任务> 2>&1 | Out-String
```

- 单任务编译：timeout 600000 ms
- 全量测试/构建：timeout 600000 ms

工作目录：`G:\Coding_Project\IdeaProjects\goods_collector`，分支 `feature/edge-fade`。

---

### Task 1: 表单 ViewModel 扩展（addCapturedImage / discardCapturedImage）—— TDD

**Files:**
- Create: `app/src/test/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModelTest.kt`
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModel.kt`（在 `addImages(...)` 之后、`replaceImagePath(...)` 之前插入两个方法）

- [ ] **Step 1: 写失败测试**

创建 `app/src/test/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModelTest.kt`：

```kotlin
package com.goodsbuy.app.ui.collectible.form

import android.content.Context
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CollectibleFormViewModelTest {

    private lateinit var tmpDir: File

    @Before
    fun setUp() {
        tmpDir = Files.createTempDirectory("form").toFile()
    }

    @After
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    private fun newViewModel(): CollectibleFormViewModel = CollectibleFormViewModel(
        repository = mockk<CollectibleRepository>(relaxed = true),
        context = mockk<Context>(),
        draftStore = mockk<CollectibleDraftStore>(relaxed = true),
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    )

    @Test
    fun `addCapturedImage appends result and respects max of 9`() {
        val vm = newViewModel()
        repeat(9) { vm.addCapturedImage("/tmp/img$it.jpg", "/tmp/src$it.jpg") }
        vm.addCapturedImage("/tmp/overflow.jpg", "/tmp/overflow-src.jpg")
        assertEquals(9, vm.uiState.value.imagePaths.size)
        assertFalse(vm.uiState.value.imagePaths.contains("/tmp/overflow.jpg"))
    }

    @Test
    fun `addCapturedImage deletes source when result differs`() {
        val src = File(tmpDir, "shot.jpg")
        val result = File(tmpDir, "shot_transparent.png")
        src.writeText("x")
        result.writeText("x")
        val vm = newViewModel()
        vm.addCapturedImage(result.absolutePath, src.absolutePath)
        assertFalse(src.exists())
        assertTrue(result.exists())
        assertEquals(listOf(result.absolutePath), vm.uiState.value.imagePaths)
    }

    @Test
    fun `addCapturedImage keeps source when result equals source`() {
        val src = File(tmpDir, "shot.jpg")
        src.writeText("x")
        val vm = newViewModel()
        vm.addCapturedImage(src.absolutePath, src.absolutePath)
        assertTrue(src.exists())
        assertEquals(listOf(src.absolutePath), vm.uiState.value.imagePaths)
    }

    @Test
    fun `discardCapturedImage deletes the target file and companions`() {
        val base = File(tmpDir, "shot").absolutePath
        val jpg = "$base.jpg"
        val orig = "${base}_orig.jpg"
        val png = "${base}_transparent.png"
        listOf(jpg, orig, png).forEach { File(it).writeText("x") }
        val vm = newViewModel()
        vm.discardCapturedImage(jpg)
        listOf(jpg, orig, png).forEach { assertFalse(File(it).exists()) }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat testDebugUnitTest --tests "com.goodsbuy.app.ui.collectible.form.CollectibleFormViewModelTest" 2>&1 | Out-String`（timeout 600000）
Expected: 编译失败，提示 `Unresolved reference: addCapturedImage` / `discardCapturedImage`。

- [ ] **Step 3: 实现最小代码**

在 `CollectibleFormViewModel.kt` 的 `addImages(...)` 方法之后、`replaceImagePath(...)` 之前插入：

```kotlin
    fun addCapturedImage(resultPath: String, sourcePath: String) {
        if (_uiState.value.imagePaths.size >= MAX_IMAGE_COUNT) return
        _uiState.update { state ->
            state.copy(imagePaths = (state.imagePaths + resultPath).take(MAX_IMAGE_COUNT))
        }
        if (resultPath != sourcePath) ImageUtils.deleteImage(sourcePath)
        scheduleDraftSave()
    }

    fun discardCapturedImage(path: String) {
        ImageUtils.deleteImageWithCompanions(path)
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: BUILD SUCCESSFUL，4 个测试全过。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModel.kt app/src/test/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModelTest.kt
git commit -m "feat: 表单支持拍摄图片入列表与丢弃（addCapturedImage/discardCapturedImage）"
```

---

### Task 2: CameraCaptureContract

**Files:**
- Create: `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CameraCaptureContract.kt`

- [ ] **Step 1: 写 contract**

创建 `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CameraCaptureContract.kt`：

```kotlin
package com.goodsbuy.app.ui.collectible.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 调用系统相机拍照，照片写入 App 内部 filesDir/images/{UUID}.jpg。
 * 相机取消/失败时删除已创建的空文件，不留残留。
 */
class CameraCaptureContract : ActivityResultContract<Unit, Uri?>() {

    private var pendingFile: File? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        val dir = File(context.filesDir, "images")
        dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        pendingFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        val file = pendingFile ?: return null
        pendingFile = null
        return if (resultCode == Activity.RESULT_OK && file.exists()) {
            Uri.fromFile(file)
        } else {
            file.delete()
            null
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat compileDebugKotlin 2>&1 | Out-String`（timeout 600000）
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/form/CameraCaptureContract.kt
git commit -m "feat: 系统相机拍照 contract（FileProvider EXTRA_OUTPUT + 失败清理）"
```

---

### Task 3: CollectibleFormScreen 集成

**Files:**
- Modify: `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt`

按顺序应用以下 6 处编辑，然后编译。

- [ ] **Step 1: 加 import**

在 `import androidx.activity.compose.BackHandler` 之前插入：

```kotlin
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
```

在 `import androidx.compose.material.icons.filled.Check` 附近（Icons.filled 一组）追加：

```kotlin
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
```

在 `import androidx.compose.material3.MaterialTheme` 附近追加：

```kotlin
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
```

在 `import androidx.compose.runtime.getValue` 附近追加：

```kotlin
import androidx.compose.runtime.rememberCoroutineScope
```

在 `import androidx.compose.ui.Alignment` 附近追加：

```kotlin
import androidx.compose.ui.platform.LocalContext
```

- [ ] **Step 2: 加状态与相机 launcher**

把现有的（约第 60-66 行）：

```kotlin
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = editingImageIndex != null) { editingImageIndex = null }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GalleryImagePickerContract(maxItems = 9)
    ) { uris -> viewModel.addImages(uris) }
```

替换为：

```kotlin
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var pendingCapturePath by remember { mutableStateOf<String?>(null) }
    var showImageSourceMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = editingImageIndex != null || pendingCapturePath != null) {
        pendingCapturePath?.let { viewModel.discardCapturedImage(it) }
        pendingCapturePath = null
        editingImageIndex = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = GalleryImagePickerContract(maxItems = 9)
    ) { uris -> viewModel.addImages(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = CameraCaptureContract()
    ) { uri ->
        uri?.path?.let { pendingCapturePath = it }
    }

    val hasCamera = LocalContext.current.packageManager
        .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
```

- [ ] **Step 3: 改「添加」按钮为打开菜单**

把图片区域现有的 FilledTonalButton（约第 157-164 行）：

```kotlin
                 FilledTonalButton(
                     onClick = { imagePickerLauncher.launch(Unit) },
                     enabled = uiState.imagePaths.size < 9
                 ) {
                     Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                     Spacer(modifier = Modifier.width(6.dp))
                     Text("从相册添加")
                 }
```

替换为：

```kotlin
                 FilledTonalButton(
                     onClick = { showImageSourceMenu = true },
                     enabled = uiState.imagePaths.size < 9
                 ) {
                     Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                     Spacer(modifier = Modifier.width(6.dp))
                     Text("添加图片")
                 }
```

- [ ] **Step 4: 加底部菜单**

在 `if (uiState.hasDraft) { ... }` 块结束之后、「// 图片区域」注释之前插入：

```kotlin
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
                                     if (intent.resolveActivity(LocalContext.current.packageManager) != null) {
                                         try {
                                             cameraLauncher.launch(Unit)
                                         } catch (_: Exception) {
                                             scope.launch { snackbarHostState.showSnackbar("无法打开相机") }
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
```

- [ ] **Step 5: 覆盖层双来源 + 确认/取消处理**

把文件末尾的覆盖层（约第 383-393 行）：

```kotlin
    uiState.imagePaths.getOrNull(editingImageIndex ?: -1)?.let { editPath ->
        EdgeFadeEditScreen(
            sourcePath = editPath,
            onCancel = { editingImageIndex = null },
            onDone = { newPath ->
                editingImageIndex?.let { index -> viewModel.replaceImagePath(index, newPath) }
                editingImageIndex = null
            }
        )
    }
```

替换为：

```kotlin
    val editPath = editingImageIndex?.let { uiState.imagePaths.getOrNull(it) } ?: pendingCapturePath
    editPath?.let { path ->
        EdgeFadeEditScreen(
            sourcePath = path,
            onCancel = {
                pendingCapturePath?.let { viewModel.discardCapturedImage(it) }
                pendingCapturePath = null
                editingImageIndex = null
            },
            onDone = { newPath ->
                if (pendingCapturePath != null) {
                    viewModel.addCapturedImage(newPath, pendingCapturePath!!)
                } else {
                    editingImageIndex?.let { index -> viewModel.replaceImagePath(index, newPath) }
                }
                pendingCapturePath = null
                editingImageIndex = null
            }
        )
    }
```

- [ ] **Step 6: 编译验证**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat compileDebugKotlin 2>&1 | Out-String`（timeout 600000）
Expected: BUILD SUCCESSFUL（如出现 `Icons.Default.PhotoCamera/PhotoLibrary` 未解析，检查 material-icons-extended 依赖，build.gradle.kts 已含）。

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt
git commit -m "feat: 添加图片底部菜单（相册选择/拍照）+ 拍摄后进渐隐编辑"
```

---

### Task 4: 全量验证

- [ ] **Step 1: 全量单元测试**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat testDebugUnitTest 2>&1 | Out-String`（timeout 600000）
Expected: BUILD SUCCESSFUL，`CollectibleFormViewModelTest` 4 项 + 既有全部测试通过。

- [ ] **Step 2: 完整构建**

Run: `$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat assembleDebug 2>&1 | Out-String`（timeout 600000）
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 3: 手动验证清单（真机）**

- 图片 <9 张时点「添加图片」→ 底部菜单出现「从相册选择 / 拍照」。
- 点「拍照」→ 系统相机打开；拍完 → 直接进入「图片渐隐」编辑页。
- 编辑页调形状/强度 → 确认 → 缩略图出现，显示透明结果；进入图库/详情均一致。
- 再次点缩略图 → 可重调（`_orig` 保留）。
- 拍照后编辑页取消/返回 → 不入列表，`filesDir/images` 无残留文件。
- 拍照后 0% 强度直接确认 → 原图入列表，缩略图正常，文件存在。
- 9 张已满 → 「添加图片」禁用。
- 无相机 App 的环境（或模拟器）→ 点拍照提示「未找到相机应用」；无相机硬件则菜单不显示「拍照」。

---

## 设计要点备忘（实现时对照）

- `pendingCapturePath` 与 `editingImageIndex` 互斥（不同时非空），覆盖层用 `?:` 兜底。
- 拍摄确认后：烘焙（result ≠ source）时只删拍摄原图（`deleteImage`，不删同 base 的 `_orig`/`_transparent.png`）；未烘焙（result == source，0% 确认/恢复原图）时保留原图。
- 拍摄取消：`discardCapturedImage` 用 `deleteImageWithCompanions`（此时只有原图文件，安全）。
- `EdgeFadeEditScreen`、`ImageUtils` 零改动。
- 无需 Manifest 改动（CAMERA 权限、FileProvider、file_paths 均已就绪）；无运行时权限请求。