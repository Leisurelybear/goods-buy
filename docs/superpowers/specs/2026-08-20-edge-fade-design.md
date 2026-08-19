# 图片边缘渐隐透明

## 目标

为藏品图片提供「边缘渐隐透明」编辑能力：把图片四周的杂乱背景逐渐隐藏，只保留中心主体，四周变为透明。透明效果烘焙进图片文件本身，因此列表、详情、图鉴、备份导出、分享等所有展示位置自动生效。

## 需求要点

1. **入口**：添加藏品页缩略图列表，点击某张缩略图进入「图片渐隐编辑」页。不点击则按原图使用，不强制编辑。
2. **形状选择**：编辑页提供 4 种遮罩形状，以选项卡切换，默认「椭圆」：
   - 椭圆（默认）：椭圆径向渐隐。
   - 圆形：正圆径向渐隐。
   - 圆角矩形：圆角矩形柔边遮罩。
   - 矩形：直角矩形柔边遮罩。
3. **滑块语义**：单个「渐隐强度」滑块 0%–100%。
   - 0%：不做任何渐隐（保持原图）。
   - 100%：只保留中心一小块区域（椭圆/圆/矩形约 15% 尺寸）可见，四周全透明。
   - 中间值平滑过渡。切换形状时滑块值保留、预览实时更新。
4. **保留原图可重调**：首次编辑时自动备份原图，之后随时可从原图重新调整（任意调大调小），并可「恢复原图」。
5. **预览背景**：浅灰棋盘格，让透明区域可见。

## 图片处理流程（方案A：烘焙透明 PNG）

新增 `ImageUtils.fadeEdgesToTransparent(context, srcPath, shape, intensity)`，在 IO 线程执行：

1. 解码原图并压缩，长边 ≤ 2048px（控内存）。
2. 按形状与强度生成 ARGB 遮罩：
   - 椭圆/圆形：`RadialGradient` shader（圆形按短边、椭圆按各自半轴），配合矩阵缩放实现椭圆；中心不透明 → 边缘透明。
   - 圆角矩形/矩形：绘制「保留区域」实心遮罩 → 对遮罩边缘做模糊羽化（`BlurMaskFilter` / `RenderEffect`）得到柔和过渡。
   - 强度映射：滑块 0%–100% 映射保留区域尺寸 100%→约 15%，渐变带宽度固定为保留尺寸的约 30%。
3. `PorterDuff.DST_IN` 合成遮罩与源图。
4. 存为 PNG（`{base}_transparent.png`），保留透明度。展示路径更新指向该 PNG。

结果透明已烘焙进文件：所有 `AsyncImage` 渲染点、备份导出、分享自动一致，无 DB 变更、无渲染层改动。

## 原图保留与文件约定

- 首次编辑时把原图备份为伴生文件 `{base}_orig.jpg`（图片本体）与 `{base}_orig.???`（如非 jpg，按实际格式）；展示路径改指向 `{base}_transparent.png`。
- 再次进入编辑时从 `_orig` 文件读取原始像素，可任意重调。
- 「恢复原图」：删除 `_transparent.png` 与 `_orig` 文件，展示路径恢复为原图路径。
- 文件清理：`removeImagePath`、`discardDraft`、`save` 中删除图片时，同步删除伴生 `_orig` / `_transparent.png` 文件。

## 已知限制

- 备份/恢复只导出 `imagePaths` 指向的文件，`_orig` 伴生文件不随备份走：恢复后图片保留透明效果，但不再能重调（此时以 PNG 本身为底）。可接受。
- 0% 强度确认时不做处理，直接恢复为原图（删除已生成的透明 PNG 与 _orig 备份）。

## 代码改动点

1. `app/src/main/java/com/goodsbuy/app/util/ImageUtils.kt`
   - 新增 `fadeEdgesToTransparent(...)` 处理函数。
   - 新增伴生文件辅助：`getOrigPath(path)`、`getTransparentPath(path)`、`deleteCompanionFiles(path)`。
2. 新增 `app/src/main/java/com/goodsbuy/app/ui/collectible/form/EdgeFadeEditScreen.kt`
   - 编辑页 UI：形状选项卡 + 棋盘格预览 + 滑块 + 恢复原图 + 确认/取消。
   - 独立小页面 + ViewModel（或并入现有 form 状态），便于测试。
3. `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormScreen.kt`
   - 缩略图包一层可点击 → 进入编辑页（携带图片路径与索引）。
4. `app/src/main/java/com/goodsbuy/app/ui/collectible/form/CollectibleFormViewModel.kt`
   - 编辑确认后替换 `imagePaths` 对应项。
   - 补齐删除/保存时的伴生文件清理。
5. 单元测试：遮罩透明区域占比、强度→保留尺寸映射、伴生文件增删逻辑。

## 不做的事

- 不改 DB schema、不改备份格式。
- 不改动所有 `AsyncImage` 渲染点。
- 不引入第三方裁剪/编辑库（uCrop 等不支持羽化渐隐）。

## 测试

- 新增 `ImageUtils` 相关单测（遮罩算法、文件生命周期）。
- 既有测试不受影响。
- 编译验证：`compileDebugKotlin` / `testDebugUnitTest` / `assembleDebug` 通过。