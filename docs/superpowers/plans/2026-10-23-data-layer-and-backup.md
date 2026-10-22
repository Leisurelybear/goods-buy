# 数据层与备份加固实施计划（Stage 2）

**日期：** 2026-10-23
**目标版本：** v1.4.2
**来源：** `docs/code-review-and-next-steps-2026-10-23.md` 阶段 2
**前置：** `docs/superpowers/plans/2026-10-23-accounting-consistency.md`（Stage 1，已完成）

## 背景

Stage 1 收敛了口径一致性。本阶段解决「数据层演进能力」与「备份链路可靠性」两件事：数据库仍是 `version = 1 / exportSchema = false` 且无索引，任何字段变更都可能丢用户数据；备份导入缺少阶段化进度与失败重试，图片入库未压缩。

本阶段只做数据层与备份加固，不加新功能。

## 任务

### T1 Schema 治理与首个 Migration（已完成）

- `app/build.gradle.kts`：`kapt` 增加 `correctErrorTypes = true` 与 `room.schemaLocation`。
- `CollectibleEntity` 增加 `@Index`：`status` / `createdAt` / `sellDate`。
- `AppDatabase`：`version = 2`、`exportSchema = true`、`MIGRATION_1_2` + `MIGRATIONS` 数组。
- 索引清单以数据形式声明（`MIGRATION_1_2_INDICES: List<IndexSpec>`），Migration SQL 由它生成，避免"实体加了索引但忘了写迁移"。
- `DatabaseModule` 改为 `.addMigrations(*AppDatabase.MIGRATIONS)`，不再依赖 `fallbackToDestructiveMigration`。
- schema JSON 提交到 `app/schemas/`（`1.json` / `2.json`）。

### T2 备份安全加固（已完成）

- `extractZip` 拆出可测的 `internal fun extractZip(input: InputStream, tempDir: File)`。
- 限额提升为 `internal const val`（`MAX_ZIP_ENTRIES` / `MAX_ENTRY_BYTES` / `MAX_TOTAL_BYTES` / `MAX_COMPRESSION_RATIO`）便于测试。
- 新增 `internal fun isAllowedEntry(name)`：只接受 `manifest.json` 与扁平 `images/<file>`。

### T3 图片入库管线（已完成）

- `ImageUtils.copyImageToInternalStorage`：`ImageDecoder` 解码（自动应用 EXIF 方向）→ 长边压缩到 2048 → 白底展平（JPEG 无 alpha）→ JPEG q85 保存为 `<uuid>.jpg`。
- 同时生成 `<uuid>_thumb.jpg`（512 长边；源图含 alpha 时用 PNG）。
- 新增 `thumbnailPathFor` / `regenerateThumbnail` / `writeThumbnail` / `flattenOntoWhite`；`baseOfImage` 与 `companionPaths` 认识缩略图后缀，删除藏品时一并清理。
- 渐隐（edge fade）应用/还原后重新生成缩略图；列表卡片改用缩略图解码。

### T4 导入阶段化进度与失败重试（已完成）

- `BackupStage`（读取 / 校验 / 复制图片 / 写入 / 清理）+ `import(..., onProgress)`。
- `ProfileViewModel`：`importStage` / `importError`；失败时**保留预览**，用户可原地重试。
- `ImportPreviewScreen`：`READY / IMPORTING / FAILED` 三态，失败态展示错误与「重试导入」。

### T5 测试补齐（已完成）

- `SchemaMigrationConsistencyTest`：解析导出的 schema JSON，断言迁移 SQL 与实体索引一致。
- `BackupManagerSecurityTest`：路径穿越 / 绝对路径 / Windows 盘符 / 嵌套路径 / 规范化重名 / 条目过多 / 单条超大 / 压缩炸弹 / 正常路径。
- `ImageUtilsPathTest`：缩略图后缀识别、`thumbnailPathFor` 回退链、伴随文件删除。

### T6 导出阶段化进度与失败重试（本阶段收尾）

- 新增 `ExportStage`（准备数据 / 打包图片 / 写入文件）。
- `BackupManager.export` 改为返回 `Result<Unit>` 并接受 `onProgress` 回调。
- `ProfileViewModel`：`isExporting` / `exportProgress` / `exportStage` / `exportError`，并记住目标 `Uri` 以便重试。
- `ProfileScreen`：导出中显示进度对话框；失败显示错误 + 「重试导出」/「取消」。

### T7 导出/导入限额自洽

- 导出不再受任何图片数量限制，但导入侧 `MAX_ZIP_ENTRIES` 曾为 2000：用户自己的备份可能**无法被自己导入**。
- 提升导入限额到 20000（总量 200MB / 单条 32MB / 压缩比 100 的限制不变，仍能拦住 zip 炸弹），并在常量处注明"必须高于导出侧可能产生的规模"。

### T8 导入时补生成缩略图

- `ImageUtils` 已改为「原图 + 缩略图」双尺寸，但恢复备份时只复制原图，列表会一直解码全尺寸图（编辑过一次才会生成缩略图）。
- 导入复制每张图片后调用 `ImageUtils.regenerateThumbnail(base, path)`，与拍摄/相册入库走同一套参数（512 长边，带采样解码，开销可控）。

### T9 覆盖导入的旧图片清理

- `OVERWRITE` 分支替换 `imagePaths` 后，旧图片文件留在 `filesDir/images` 里不再被任何记录引用。
- 事务提交后，按「是否仍被其他藏品引用」清理旧图片（避免误删共享图片）。
- 导入失败回滚时把 `deleteImage` 换成 `deleteImageWithCompanions`，避免残留缩略图。

## 明确不做

- 未使用 Robolectric：该环境的 Robolectric 会挂死测试 JVM，相关依赖已移除，不重新引入。
- `imagePaths` 逗号拼接改独立表：需要一次破坏性迁移与 UI 全链路改造，收益/风险不匹配，留待后续。
- 搜索 FTS5 / 分页：前导通配 `LIKE` 无法命中 B-tree 索引，FTS5 属于独立专项。
- 搜索 debounce：与列表页 `TextFieldValue` 双向同步冲突，单独评估。

## 验收

- `./gradlew testDebugUnitTest` 通过（91 项）。
- `./gradlew assembleDebug` 通过。
- 手工核对：导出过程中能看到阶段与进度；导出失败后可直接重试；导入失败后可直接重试。