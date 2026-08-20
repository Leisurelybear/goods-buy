# 添加照片支持直接拍照

## 目标

在添加藏品时，允许用户直接调用系统相机拍照，而不只是从相册选择已有照片。拍照后立即进入「图片渐隐」编辑页，编辑完成才加入图片列表；取消则丢弃照片。覆盖用户手边没有现成照片、需要现场拍摄藏品的使用场景。

## 需求要点

1. **入口**：图片区域的「添加」按钮由直接打开相册选择器，改为弹出底部菜单，提供两个选项：
   - 从相册选择（走现有系统图片选择器，行为不变）
   - 拍照（调用系统相机）
2. **拍照方式**：系统相机 Intent（`ACTION_IMAGE_CAPTURE` + FileProvider `EXTRA_OUTPUT`），不引入新依赖、无需运行时权限请求。Manifest 已声明 `CAMERA` 权限与 FileProvider、`file_paths.xml` 已暴露 `images/` 目录，无需任何 Manifest 改动。
3. **拍完直接进编辑页**：照片拍完即打开「图片渐隐」编辑页（复用 `EdgeFadeEditScreen`，零改动）。
   - 编辑**确认** → 结果路径加入图片列表，缩略图出现。
   - 编辑**取消/返回** → 丢弃照片（删除文件，不入列表）。
4. **上限**：9 张已满时「添加」按钮禁用（现有逻辑），菜单不可达；拍照同样受此约束。
5. **相机不可用**：无相机硬件时菜单隐藏「拍照」项；有硬件但无相机 App 时点击提示「未找到相机应用」。

## 实现方案（方案 A：独立待处理拍摄状态）

核心思路：把「拍摄结果」当作一个独立的待确认来源，不先污染图片列表再回滚。表单持有 `pendingCapturePath`，与现有的 `editingImageIndex` 并列驱动编辑覆盖层，二选一显示。

### 组件

**新增 `CameraCaptureContract.kt`**（`ui/collectible/form/` 包）
- `ActivityResultContract<Unit, Uri?>`：
  - `createIntent(context, input)`：生成目标文件 `filesDir/images/{UUID}.jpg`，用 FileProvider 转成 `content://` URI，放入 `Intent(ACTION_IMAGE_CAPTURE)` 的 `EXTRA_OUTPUT`，并加 `FLAG_GRANT_WRITE_URI_PERMISSION`。
  - `parseResult(resultCode, intent)`：`RESULT_OK` 返回该 URI；否则删除已创建的目标文件（相机取消/失败不留空文件）。
- 目标文件路径在 contract 实例内暂存（createIntent 与 parseResult 之间）。

**`CollectibleFormScreen.kt` 改动**
- 新增状态：`pendingCapturePath: String?`、`showImageSourceMenu: Boolean`。
- 「从相册添加」按钮 → 打开 `ModalBottomSheet` 菜单（从相册选择 / 拍照）。
- 相机 launcher：`rememberLauncherForActivityResult(CameraCaptureContract()) { uri -> 成功则 pendingCapturePath = 文件路径 }`。
- 覆盖层双来源：`editingImageIndex?.let { imagePaths.getOrNull(it) } ?: pendingCapturePath`，非空则显示 `EdgeFadeEditScreen(sourcePath = editPath, onCancel = closeEditor, onDone = onEditorDone)`。
  - `closeEditor`：若 `pendingCapturePath` 非空 → `viewModel.discardCapturedImage(path)`；清空两个状态。
  - `onEditorDone(newPath)`：若 `pendingCapturePath` 非空 → `viewModel.addImage(newPath)`；否则 → `viewModel.replaceImagePath(index, newPath)`；清空两个状态。
- `BackHandler` 改为覆盖 `editingImageIndex != null || pendingCapturePath != null`。

**`CollectibleFormViewModel.kt` 改动**
- 新增 `addCapturedImage(resultPath, sourcePath)`：追加 `resultPath` 到 `imagePaths`（`(state.imagePaths + resultPath).take(MAX_IMAGE_COUNT)`）→ `scheduleDraftSave()`；当 `resultPath != sourcePath`（已烘焙 PNG）时删除拍摄原图 `sourcePath`（避免孤儿文件），否则保留（原图即最终图，0% 确认/恢复原图场景）。
- 新增 `discardCapturedImage(path)`：`ImageUtils.deleteImageWithCompanions(path)` 丢弃拍摄文件（取消场景；此时未烘焙，仅有原图，安全）。

**`EdgeFadeEditScreen.kt`**：零改动。

### 数据流

```
点「添加」(图片<9) → ModalBottomSheet
  ├─ 从相册选择 → 现有 imagePickerLauncher → addImages(uri 列表)      [不变]
  └─ 拍照 → 系统相机 App → 写入 filesDir/images/{UUID}.jpg
       ├─ 成功 → pendingCapturePath = 文件路径 → 覆盖层打开编辑页
       │    ├─ 确认 → addCapturedImage(resultPath, sourcePath) → 入列表；
       │    │        若 resultPath ≠ sourcePath（已烘焙）删除拍摄原图，保留 _orig + PNG；
       │    │        否则保留原图（0% 确认/恢复原图）
       │    └─ 取消/返回 → discardCapturedImage(path) → 文件删除
       └─ 相机取消/失败 → contract 删除空文件 → 无残留
```

### 错误处理

- 无相机 App：启动前 `context.packageManager.resolveActivity` 检查，找不到 → Snackbar「未找到相机应用」。
- 无相机硬件：`PackageManager.FEATURE_CAMERA_ANY` 缺失时菜单隐藏「拍照」项。
- 目标文件创建失败：contract 返回 null，Snackbar 提示。
- 拍照后文件已有但编辑取消：`discardCapturedImage` 用 `deleteImageWithCompanions` 清理（即使编辑过程中产生伴生文件也一并清理）。

### 测试

- `CollectibleFormViewModel` 新增单元测试（mockk 仓库/草稿存储 + 真实临时文件）：
  - `addCapturedImage` 追加路径并遵守 9 张上限；`resultPath != sourcePath` 时删除 sourcePath；相等时保留。
  - `discardCapturedImage` 删除目标文件及伴生文件。
- 拍照 contract 依赖 Android 框架（FileProvider/ContentUri），不引入 Robolectric，不做 JVM 单测。

## 代码改动点

1. 新增 `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CameraCaptureContract.kt`。
2. `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt`：菜单、`pendingCapturePath`、覆盖层双来源、BackHandler。
3. `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModel.kt`：`addImage`、`discardCapturedImage`。
4. 测试：`CollectibleFormViewModelTest`（或扩展现有测试文件）。

## 范围确认

- 无新增依赖、无 Manifest 改动、无运行时权限请求。
- 相册选择流程与拍照流程共享同一覆盖层与编辑逻辑，行为一致。
- 拍摄照片同样走 `ImageDecoder` 解码，EXIF 方向与展示一致。