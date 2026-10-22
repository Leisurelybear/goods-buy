# 谷的拜（GoodsBuy）二次 Review 与下一步计划

**评审日期：** 2026-10-23
**当前版本：** v1.4.1（`versionCode=9`）
**基线：** 对比 `docs/code-review-and-product-plan-2026-08-16.md`（v1.3.0）
**验证方式：** 静态审查 + `./gradlew testDebugUnitTest`（BUILD SUCCESSFUL，10 个测试类通过）+ `assembleDebug` 成功

## 1. 结论先行

v1.3.0 评审提出的 P0/P1 安全问题与发布工程化问题**大部分已经落地**：ZIP 路径穿越/炸弹防护、导入事务化、导出命名与输出流判空、表单领域校验、`createdAt` 保留、正式签名 + R8、CI 发版流程都已到位，代码质量较上一轮有实质提升。

当前剩余的隐患集中在两类：

1. **口径一致性问题**（会影响用户对账，属于"数据可信度"问题，不是崩溃）：搜索与状态筛选仍互斥；状态迁移三处实现不一致导致 `sellDate` 漂移；持仓市值按单价直接累加；分类统计的投入/利润分母不同范围。
2. **数据层演进能力**：数据库仍是 `version = 1 / exportSchema = false`，没有索引、没有 Migration、`imagePaths` 仍是逗号拼接。任何一次字段变更都会触发用户数据丢失风险，这是继续加功能前的硬性前置。

功能侧仍以"记录 + 统计"为核心，ROADMAP 里的"出谷文案、Excel、补款提醒、云同步、应用锁"都尚未开始；建议先做完口径与数据层治理，再做文案/Excel 这类高性价比功能。

## 2. 上轮问题验收

### 已修复

| 编号 | 问题 | 证据 |
|---|---|---|
| P0 Zip Slip / Zip Bomb | 已加固 | `BackupManager.extractZip` 校验规范路径、拒绝绝对路径/`:`/空字节、去重条目、限制 `MAX_ZIP_ENTRIES`/`MAX_ENTRY_BYTES`/`MAX_TOTAL_BYTES`，并拒绝压缩比 > 100 的条目 |
| P1-1 `createdAt` 被清零 | 已修复 | `CollectibleFormViewModel.initialize` 载入 `createdAt`，`save()` 使用 `state.createdAt` |
| P1-4 导入非事务 | 已修复 | `database.withTransaction { updateCollectibles; insertCollectibles }` |
| P1-5 先复制图片再判重 | 已修复 | 先算 `plans`，仅复制 `action != SKIP` 的图片；覆盖时无图片则保留原 `imagePaths` |
| P1-6 导出命名/空流误报成功 | 已修复 | 图片用 `UUID` 前缀命名；`openOutputStream(...) ?: throw IOException` |
| P1-7 表单无领域校验 | 已修复 | 新增 `domain/validation/CollectibleValidator.kt`，字段级错误 + `fieldErrors` 回显 |
| P2-2 备份在主线程序列化 | 已修复 | `ProfileViewModel` 用 `withContext(Dispatchers.IO)` 包裹预览/导入/导出 |
| 发布工程化 | 已修复 | `release` 开启 `isMinifyEnabled`/`isShrinkResources`，签名走 `RELEASE_*` 属性；CI 有 `apksigner verify`、artifact 与 GitHub Release |
| 删除撤销 | 已修复 | `UndoDeleteManager` + `PendingDeletion`，列表/详情/图鉴统一入口 |

### 仍存在

| 编号 | 问题 | 现状证据 | 影响 |
|---|---|---|---|
| P1-2 | 状态迁移三处不一致 | `CollectibleListViewModel.quickUpdateStatus`：`SOLD` 时**总是**刷新 `sellDate`；`CollectibleDetailViewModel.updateStatus`：仅 `sellDate == null` 时设置；`CollectibleFormViewModel.save`：非 `SOLD` 时**清空** `sellDate`。三者规则不同，且快速改状态离开 `SOLD` 时**保留旧日期** | 月度收入归属月份可能被悄悄改写；同一藏品从不同入口操作结果不同 |
| P1-3 | 搜索与状态筛选互斥 | `CollectibleListViewModel.uiState`：`if (query.isNotBlank()) searchCollectibles(query) else if (status != null) getCollectiblesByStatus(status) else getAll()`；DAO 也没有组合查询 | "搜索 + 已售出"返回错误结果，与 UI 上同时可见的搜索框和筛选条矛盾 |
| P1-8 | `expectedPrice` 口径未定义 | `GetDashboardSummaryUseCase`：`holdingValue = collectibles.sumOf { it.expectedPrice }`，未乘 `remainingQuantity` | 多数量藏品的持仓市值被低估 |
| P1-9 | 分类统计分母不一致 | `GetCategoryStatsUseCase`：`investment` = 全部 `purchaseTotal`，`profit` = `revenue - realizedCost`（仅已结算） | 用户看到的"投入"与"利润率"无法对账 |
| P2-3 | 无 DB 迁移/索引 | `AppDatabase(version = 1, exportSchema = false)`；`CollectibleEntity` 无 `@Index` | 首次改 schema 即面临数据丢失；`status`/`ipName` 查询无索引 |
| P2-4 | `imagePaths` 逗号拼接 | `CollectibleMapper` 用 `split(",")`/`joinToString(",")` | 文件名含逗号即损坏解析（当前 UUID 名安全，属隐患） |
| P2-5 | 新增图片未压缩 | `ImageUtils.copyImageToInternalStorage` 原样复制字节，`MAX_FULL_DIMENSION`/`sampleSizeFor` 只用于渐隐编辑路径 | 相册原图直接进 `filesDir`，存储膨胀、列表解码变慢 |
| P2-7 | 搜索无 debounce | `CollectibleListViewModel.onSearchQueryChange` 直接写入 `_searchQuery` 触发查库；`CollectibleListScreen` 无 `debounce` | 每次按键一次 Room 查询 |

## 3. 本轮新发现

1. **测试覆盖存在结构性缺口**：现有 10 个测试类集中在 calculator/validator/gallery/theme，但 `CollectibleListViewModel`（组合筛选）、`GetDashboardSummaryUseCase`（持仓市值）、`GetCategoryStatsUseCase`、`BackupManager`（Zip Slip/回滚）**没有任何测试**。上轮列出的"备份安全测试"和"Room 组合查询测试"未落地。
2. **主题系统已具备多主题骨架但只有一个主题**：`AppThemes.all` 只注册 `DreamyPurpleTheme`，`themeId` 偏好已持久化并在 `MainActivity` 生效，但设置页主题选择器无实际可选项；且没有"跟随系统/浅色/深色"三态开关（目前只能跟随系统）。
3. **`duplicateCollectible` 图片复制用 `System.currentTimeMillis()` 前缀**（`CollectibleListViewModel`），同一毫秒内复制多张同名图存在覆盖风险，与 `BackupManager` 已改用的 UUID 命名不一致。
4. **统计页筛选是内存过滤**（`StatisticsViewModel`），数据量大时每次筛选全量重算；短期无碍，量级上万条时需下推。
5. **DAO 存在未被 UI 使用的接口**：`searchByName`、`getSoldCollectibles`、`deleteCollectiblesByIds` 在 UI 层无调用点，属于历史遗留，建议清理或补使用方。
6. **`CollectibleValidator` 未校验 `sellDate` 与买入日期关系**，也未校验 `expectedPrice` 为空时按 `0.0` 落库（`save()` 里 `?: 0.0`），"未填预期价"和"预期价为 0"无法区分。

## 4. 优化建议

### 4.1 正确性（优先）

- 引入 `UpdateCollectibleStatusUseCase` 作为**唯一**状态迁移入口：首次进入 `SOLD` 才写 `sellDate`；离开 `SOLD` 时按产品决策"清空"或"保留并标记历史售出日期"；每次更新写 `updatedAt`。详情/列表/图鉴/表单全部改为调用它。
- DAO 增加组合查询（`query` + `status` 同时生效），空查询走索引友好分支；列表 `flatMapLatest` 改为单流。
- 明确 `expectedPrice` 语义（建议"预期单价"），持仓市值改为 `expectedPrice * remainingQuantity`，并在 UI 加口径说明。
- 统一分类统计口径：拆出 `grossInvestment` / `realizedCost` / `holdingCost` / `giftCost` / `lossCost`，比例统一以"已结算成本"为分母，并在 UI 提供可点开的明细。

### 4.2 数据层

- 打开 `exportSchema = true` 并提交 schema JSON；为 `status`、`ipName`、`seriesName`、`purchaseDate`、`sellDate` 建 `@Index`；引入 `Migration` 与 `MigrationTestHelper` 测试；禁止生产 `fallbackToDestructiveMigration`。
- 中期把 `imagePaths` 迁移到独立 `collectible_images` 表（含排序、宽高、缩略图路径、哈希），解决逗号拼接隐患，并为缩略图/原图双尺寸铺路。

### 4.3 性能与体验

- 搜索加 300ms debounce；数据量上千后引入分页或 FTS5。
- 图片入库时统一按 EXIF 旋转 + 限制长边（如 2048）+ 生成列表缩略图（如 512），列表用缩略图、详情用原图；`AsyncImage` 补 `SizeResolver`。
- 三态状态：Loading / Error（含重试）/ Empty 统一到每个页面；导入导出显示阶段化进度（读取→校验→复制图片→写库→清理）。
- 无障碍：给卡片/状态胶囊补 `contentDescription` 与语义分组；暗色对比度纳入检查。

### 4.4 工程

- 补测试：备份安全（路径穿越/超大条目/中途失败回滚）、组合筛选、持仓市值、分类口径、状态迁移。
- CI 增加 `lint` 与 `assembleDebug` 门禁（当前只有 `testDebugUnitTest assembleRelease`）。
- 清理未使用 DAO 接口；统一图片命名策略到 UUID；把 `ProfileViewModel` 的备份逻辑下沉为 `ExportBackupUseCase` / `ImportBackupUseCase`，让 `BackupManager` 不再直接持有 DAO。

## 5. 功能建议

**高性价比（复用现有数据，1 个迭代内可交付）**

- 出谷文案一键生成（ROADMAP 已列，闲鱼/微博/小红书模板，本地拼装 + 复制，不自动发布）。
- Excel 导出/导入（仓库已有 `convert_excel.py`、`goods_buy_import_template.xlsx`、`import-data-converter` skill，打通到 App 内即可）。
- 补款/补邮/发货节点提醒（`purchaseDate` + 状态已有，接 `WorkManager` + 通知）。
- 预算管理（月度/年度吃谷预算与超支预警，纯本地计算）。

**进阶（需先完成数据层治理）**

- 应用锁（生物识别/密码），隐私保护成本低、用户感知强。
- 换物管理（以谷换谷记录与盈亏折算）。
- 拼团管理（团长/代收代发）。
- 深色模式手动三态开关 + 第二套主题皮肤（主题骨架已就绪，主要是补配置和选择器）。
- 云同步（先做本地优先 + 冲突版本 `updatedAt + deviceId + revision`，再做端到端加密备份）。
- 识图录入 / 市价参考 / 社区（依赖后端，后置）。

## 6. 下一步（建议执行顺序）

### 阶段 1：口径与正确性（1 个迭代）— 已完成
- [x] `UpdateCollectibleStatusUseCase` 统一状态迁移，消除 `sellDate` 漂移
- [x] 列表组合筛选（搜索 + 状态），DAO 组合查询 + 索引
- [x] `expectedPrice` 口径定案并修正持仓市值
- [x] 分类统计口径统一 + UI 明细
- [x] 补 `CollectibleListViewModel` / `GetDashboardSummaryUseCase` / `GetCategoryStatsUseCase` 测试

### 阶段 2：数据层与备份（1 个迭代）— 已完成
- [x] `exportSchema = true` + `@Index` + 首个 `Migration` + 迁移测试
- [x] 补 `BackupManager` 安全与回滚测试
- [x] 图片入库压缩 + EXIF 旋转 + 缩略图
- [x] 导出/导入阶段化进度与失败重试

实施记录见 `docs/superpowers/plans/2026-10-23-accounting-consistency.md`（阶段 1）与
`docs/superpowers/plans/2026-10-23-data-layer-and-backup.md`（阶段 2）。

### 阶段 3：高性价比功能（1-2 个迭代）
- [ ] 出谷文案一键生成
- [ ] Excel 导出/导入打通 App
- [ ] 补款/补邮提醒（WorkManager + 通知）
- [ ] 预算管理与超支预警
- [ ] 深色模式三态开关 + 第二套主题

### 阶段 4：进阶
- [ ] 应用锁
- [ ] 换物 / 拼团模型
- [ ] 云同步（本地优先 + 冲突版本 + 端到端加密）
- [ ] 识图录入 / 市价参考 / 社区（依赖后端）

## 7. 阶段 2 复查补充（2026-10-23 实施时发现）

阶段 2 落地过程中又发现三处与备份链路相关的隐患，已一并修复：

| 编号 | 问题 | 现状 | 处理 |
|---|---|---|---|
| N1 | 导入限额与导出规模不自洽 | 导入侧 `MAX_ZIP_ENTRIES` 原为 2000，而导出侧不限图片数量；藏品较多时用户自己的备份会被自己的导入拒绝 | 条目上限提到 20000、单条上限提到 32MB（总量 200MB 与压缩比限制不变）；限额改为可注入的 `ZipLimits`，并新增"默认限额必须高于导出规模"的守卫测试 |
| N2 | 导入不生成缩略图 | `ImageUtils` 已改为"原图 + 缩略图"双尺寸，但恢复备份时只复制原图，列表会解码全尺寸图 | 导入复制图片后按同一套参数补生成缩略图（`decodeDownscaled` 带采样，开销可控） |
| N3 | 覆盖导入会泄漏旧图片 | `OVERWRITE` 分支替换 `imagePaths` 后，旧图片文件留在 `filesDir/images` 里不再被引用 | 提交事务后按"是否仍被其他藏品引用"清理旧图片；同时把导入回滚的 `deleteImage` 换成 `deleteImageWithCompanions`，避免残留缩略图 |

阶段 2 之后追加完成：

- CI 门禁：新增 `.github/workflows/ci.yml`，在 PR 与 `main` 推送时执行 `lintDebug` + `testDebugUnitTest` + `assembleDebug`（当前 lint 基线为 0 error / 20 warning），失败时上传 lint 报告与测试结果；原有的 `release.yml` 只负责发版，职责分离。

仍待处理（留待阶段 3 或专项）：

- 搜索无 debounce，且数据量上万后需要 FTS5 或分页；当前 `LIKE '%x%'` 无法命中索引。
- `imagePaths` 仍是逗号拼接字符串，文件名含逗号即解析错乱；应迁移到独立图片表。
- `StatisticsViewModel` 的筛选是内存过滤，未下推数据库。
- DAO 里仍有 UI 未使用的接口（`searchByName` / `getSoldCollectibles` / `deleteCollectiblesByIds`）。
- 备份预览/导入的失败信息来自异常 `message`，面向用户的可读性一般，可改为错误码 + 文案映射。
- 分支保护（required status checks）需要仓库管理员在 GitHub 设置里勾选 CI，工作流文件本身无法声明。
- 导出/导入缺少 UI 层的自动化测试（`BackupManager.export`/`import` 依赖 `Context`/`Uri`），当前只覆盖了可纯函数化的部分。