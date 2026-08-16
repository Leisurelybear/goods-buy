# 谷的拜（GoodsBuy）全面 Review 与实施方案

**评审日期：** 2026-08-16
**评审范围：** `app/src/main/java`、Room 数据层、Compose 界面、备份恢复、测试、构建配置、README/ROADMAP
**当前版本：** v1.3.0（`versionCode=6`）

## 1. 结论先行

当前项目已经具备一个可用的“本地藏品台账”MVP：数据模型完整，业务层有统一的部分售出核算，列表/图鉴/统计/备份的主流程齐全，代码规模适合继续迭代。架构方向（Compose + MVVM + Clean Architecture + Room + Hilt）是正确的。

但在扩大用户量、发布正式版或引入云同步之前，建议先完成三轮治理：

1. **数据安全与正确性（P0/P1）**：修复 ZIP 路径穿越、编辑覆盖 `createdAt`、搜索与状态筛选互斥、导入非事务、售出日期状态不一致等问题。
2. **领域口径与可维护性（P1）**：明确“单价/数量/运费/持仓市值/赠品和遗失”的财务定义，把状态迁移和金额校验集中到领域层，避免多个 ViewModel 各自复制规则。
3. **发布与体验基础（P1/P2）**：正式签名、开启 R8、数据库迁移策略、图片压缩与缩略图、错误/加载状态、无障碍和暗色主题，然后再做云同步和社区。

本次评审未改动业务代码，仅新增本报告。尝试执行 `./gradlew test` 时，环境在 `C:\Users\Jason\.gradle\wrapper\dists\...gradle-8.2-bin.zip.lck` 遇到“拒绝访问”，因此本文的测试结论以静态审查和现有测试代码为依据，不能替代 CI/真机验证。

## 2. 当前代码结构与数据流

```text
MainActivity / NavGraph
        │
        ├─ 藏品列表（搜索、状态筛选、排序、批量、橱窗）
        ├─ 图鉴（按 IP/系列分组、折叠、组内网格）
        ├─ 详情 / 表单（状态、图片、买入卖出字段）
        ├─ 统计（看板、分类、月度趋势）
        └─ 我的（偏好、ZIP 备份/恢复、日志）
                │
          ViewModel + StateFlow
                │
       Domain UseCase / Calculator
                │
       CollectibleRepository 接口
                │
       Room DAO → CollectibleEntity
                │
       filesDir/images（图片）
```

主要目录职责：

- `domain/model`：藏品、订单状态、库存状态、统计 DTO。
- `domain/calculator`、`domain/usecase`：盈亏、看板、分类、月度统计。
- `data/entity`、`data/db`、`data/mapper`、`data/repository`：Room 与领域模型转换。
- `ui/*`：Compose 页面、UiState、ViewModel；`ui/components` 有卡片、状态、盈亏等共享组件。
- `util/BackupManager.kt`：ZIP + JSON 清单 + 图片复制；`ImageUtils.kt`：内部图片存储。
- `ui/preferences`：基于 `SharedPreferences` 的展示设置。

整体依赖方向清楚，但目前有两个明显的边界问题：

1. 备份工具直接依赖 `CollectibleDao`，跳过 Repository 和事务边界。
2. 状态变更、删除图片、复制藏品分别散落在三个 ViewModel，容易产生行为漂移。

## 3. 功能逻辑 Review

### 3.1 藏品录入、编辑、删除

流程是“表单 StateFlow → `Collectible` → Repository → DAO”。字段覆盖面较好，支持最多 9 张图、部分售出数量、买卖运费和买家备注。删除时同时删除图片，方向正确。

需要补齐：表单目前只用“名称非空”控制保存；金额和数量的空值、负数、超量、售出状态必填条件没有统一校验。保存失败没有错误 State，用户可能看不到失败原因。图片选择器每次允许 9 张，但可重复打开并继续追加，实际数量可能超过 9。

### 3.2 订单状态

`OrderStatus` 覆盖预售、物流、持有、出售、赠品、遗失等场景。详情页、列表长按菜单、图鉴长按菜单都可以改状态。

目前状态变更没有统一的“迁移规则”：详情页保留旧售出日期，列表/图鉴在重复点选“已售出”时会覆盖日期，改回非售出状态时又可能保留旧日期；三处行为不一致。建议引入 `OrderStateMachine`，由领域层决定 `sellDate`、`sellQuantity`、`sellPrice`、`updatedAt` 的联动。

### 3.3 财务统计

`CollectibleAccounting` 已将购买运费按实际售出数量分摊，解决了部分售出的核心问题；详情、月度图和看板也复用了一部分口径，这是项目的优点。

仍需明确以下口径：

- `expectedPrice` 在表单中是“单价”还是整件记录总价？当前看板直接 `sumOf(expectedPrice)`，没有乘以剩余数量；如果它是单价，多件藏品的持仓市值会偏低。
- 分类统计的 `investment` 是全部购入成本，但 `profit` 只扣已结算成本；UI 又用 `investment` 计算比例，分母与利润不在同一范围。
- 赠品/遗失计入结算成本，但收入只统计 `SOLD`。这可能是产品预期，也可能需要单独显示“损耗/赠与成本”。
- 浮点 `Double` 直接存储金额，长期累计会出现小数误差；应改用分（`Long`）或 Decimal 方案。

### 3.4 搜索、筛选、排序与图鉴

列表搜索由 DAO 的 `LIKE` 查询完成，图鉴则在内存过滤。列表的搜索和状态筛选在查询分支中是互斥的：只要搜索词非空，就不会再应用状态条件，用户看到的结果可能包含其他状态。建议 DAO 提供一个同时接收 `query` 和 `status` 的查询，或统一在 Repository 层过滤。

图鉴分组、未分类置底、组内排序和折叠逻辑清晰；当前图鉴状态只存在页面内，离开页面会重置，属于可接受的体验取舍。

### 3.5 备份与恢复

ZIP 中包含 `manifest.json` 和图片，支持跳过/新增/覆盖和导入预览，指纹去重比单纯按名称更可靠。

但这是当前风险最高的模块：解压路径未校验、导入非事务、图片先复制后判重、覆盖时可能清空原图片、文件名可能碰撞，且大文件操作运行在 `viewModelScope` 默认线程。正式发布前应优先重构。

## 4. 问题清单（按优先级）

### P0：发布前必须修复

| 编号 | 问题与证据 | 影响 | 修复措施 |
|---|---|---|---|
| P0-1 | `BackupManager.kt` 在预览和导入中直接 `File(tempDir, entry.name)`，没有验证 canonical path。 | 恶意 ZIP 可通过 `../` 写入缓存目录外的文件，属于 Zip Slip。 | 仅允许 `manifest.json` 和 `images/<basename>`；解析后校验 `target.canonicalPath.startsWith(tempDir.canonicalPath + separator)`；拒绝绝对路径、符号链接和异常层级。 |
| P0-2 | `app/build.gradle.kts` 的 release 使用 debug signing，且 `isMinifyEnabled=false`。 | 正式包密钥不可控、反编译成本低，无法作为可信发布版本。 | 配置 release keystore/CI secrets；开启 R8、资源压缩；不要把 debug 签名提交到发布配置；增加签名和产物校验。 |

### P1：下一版本应修复

| 编号 | 问题与证据 | 影响 | 修复措施 |
|---|---|---|---|
| P1-1 | 编辑保存时 `createdAt = if (state.id != null) 0 else ...`（`CollectibleFormViewModel.kt`）。 | 编辑一条记录会把创建时间清零，按创建时间排序和图鉴排序异常；覆盖导出数据也会传播错误时间。 | 编辑前保留原实体的 `createdAt`；只更新 `updatedAt`。保存应使用 loaded entity 或在 ViewModel 中缓存原值。 |
| P1-2 | 列表/图鉴快速改状态时 `sellDate` 规则与详情页不同；重复设置 SOLD 会刷新日期，改回非 SOLD 可能保留日期。 | 月度收入归属月份改变，历史数据被悄悄篡改。 | 领域层统一迁移：首次 SOLD 才设置日期；离开 SOLD 明确清除或保留并标记“历史售出日期”，由产品定案；所有状态更新写入 `updatedAt`。 |
| P1-3 | 列表查询中“有搜索词”优先于“状态筛选”。 | “搜索 + 已售出”结果错误。 | SQL 同时处理 `query`、`status`，并为空查询做索引友好分支；增加组合测试。 |
| P1-4 | `BackupManager.import` 逐条写 DAO，没有 `Room @Transaction`；中途异常会留下半份数据和已复制图片。 | 导入失败后数据不一致，重试还会出现重复图片。 | DAO 增加批量写入事务；先校验清单和图片，再一次性提交；失败时回滚数据库并删除本次生成的图片。 |
| P1-5 | 导入会先复制全部图片，再决定 SKIP；覆盖模式遇到空图片列表会把原 `imagePaths` 清空。 | 产生孤儿文件；覆盖备份可能损坏现有图片关联。 | 先按记录判重，再只复制需要的图片；覆盖时无图片应保留原图（除非用户明确选择清空）；建立导入批次目录和清理任务。 |
| P1-6 | 导出图片名使用 `System.currentTimeMillis()`，同一毫秒可能冲突；`openOutputStream(outputUri)?.use { ... }` 为空时仍返回 `true`。 | 多图导出偶发失败；输出 URI 无法打开时误报成功。 | 使用 UUID/内容哈希命名；显式判断 OutputStream 非空；导出后校验 ZIP 可读和清单数量。 |
| P1-7 | 表单无领域校验，`purchaseQuantity`、`sellQuantity`、价格可为空/为 0，售出数量可超过购入数量。 | 统计被静默截断或产生不符合用户认知的数据。 | 新增 `CollectibleValidator`：名称、数量 > 0、售出量范围、SOLD 必须有日期/价格（或明确“未知”）、金额非负；UI 展示字段级错误，不用 `?: 0` 吞掉输入错误。 |
| P1-8 | `expectedPrice` 看起来是单价，但看板直接累加，不乘剩余数量。 | 多数量记录的持仓市值可能低估。 | 明确字段为“预期单价”或“记录总价”；若为单价，计算 `expectedPrice * remainingQuantity`，并补测试和文案。 |
| P1-9 | 分类统计的投资、利润、比例使用不同成本范围；赠品/遗失没有单独损耗字段。 | 用户会看到“投入”和“利润率”无法对账。 | 定义 `grossInvestment`、`realizedCost`、`holdingCost`、`lossCost`、`giftCost` 等指标；UI 显示口径说明和可点击明细。 |

### P2：体验、性能与维护性

| 编号 | 观察 | 建议 |
|---|---|---|
| P2-1 | ZIP 解压没有大小、条目数、压缩比限制。 | 增加最大压缩包、单文件、总解压大小和条目数量；拒绝异常压缩比，避免 Zip Bomb。 |
| P2-2 | `BackupManager`、`ImageUtils`、日志写文件在默认主线程调用路径上。 | 使用 `Dispatchers.IO`；导入/导出/图片处理提供可取消进度和前台通知。 |
| P2-3 | 数据库版本为 1、`exportSchema=false`，没有迁移策略。 | 在版本 2 前打开 schema 导出；每次字段变更写 Migration 和迁移测试，禁止生产环境 destructive migration。 |
| P2-4 | `imagePaths` 以逗号拼接保存。 | 当前 UUID 文件名没有逗号，未来 URI/文件名包含逗号会损坏解析；改为 Room 独立图片表或 JSON/内置序列化。 |
| P2-5 | 图片原样复制却统一命名 `.jpg`，没有压缩、方向修正和缩略图。 | 增加 EXIF 方向处理、尺寸/质量限制、WebP/JPEG 缩略图；列表使用缩略图，详情使用原图。 |
| P2-6 | 图片 `AsyncImage` 使用文件路径且没有尺寸 resolver；网格卡片固定宽度。 | 使用 `contentScale` 与 `SizeResolver`，避免大图解码；按网格单元宽度自适应，兼容平板和横屏。 |
| P2-7 | 搜索每次按键直接查库，没有 debounce；DAO 的多列 LIKE 无索引。 | 300ms debounce、规范化搜索列/FTS5；数据量增长后按分页加载。 |
| P2-8 | Loading、空数据、异常状态多数只有空白/通用 EmptyState。 | 每个页面提供 Loading、Error（含重试）、Empty 三态；导入/保存失败要有持久 Snackbar 或字段错误。 |
| P2-9 | `READ_MEDIA_IMAGES`、`CAMERA` 权限与当前 Photo Picker/相机功能不完全匹配。 | 优先使用系统 Photo Picker，移除不必要权限；真正接入相机时再按 API 版本申请运行时权限。 |
| P2-10 | 默认主题是 `android:Theme.Material.Light.NoActionBar`，没有暗色/动态色；版本号在 UI 中硬编码。 | 使用 Material 3 DayNight、动态色开关和无障碍对比度；从 `BuildConfig.VERSION_NAME` 显示版本。 |
| P2-11 | 卡片图片 `contentDescription=null`、部分图标无语义，图表只提供一段长描述。 | 为图片提供名称/状态摘要；图标提供操作语义；图表增加可访问数据表/逐点读屏。 |
| P2-12 | 状态、删除、复制逻辑在 List/Gallery/Detail 三个 ViewModel 重复。 | 抽出 `UpdateCollectibleStatusUseCase`、`DeleteCollectibleUseCase`、`DuplicateCollectibleUseCase`，统一日志、时间戳、图片清理。 |
| P2-13 | 仅有财务、月度、图鉴和名称工具测试，Backup、DAO、表单校验、迁移、Compose 无障碍测试缺失。 | 建立测试金字塔（见第 7 节）。 |

## 5. 建议的目标架构

### 5.1 领域模型拆分

将一个超宽的 `Collectible` 拆成语义更清晰的值对象：

```text
Collectible
 ├─ Identity(name, ip, series, character, category, type)
 ├─ Purchase(orderId?, channel, shop, date, unitPrice, quantity, shipping)
 ├─ Sale(unitPrice?, quantity?, shipping?, freeShipping, date, buyerRef)
 ├─ Lifecycle(orderStatus, storageStatus)
 ├─ Media(images)
 └─ Audit(createdAt, updatedAt)
```

金额统一使用“分”为 `Long`（或明确的 Decimal），数据库保留迁移兼容字段。`buyerInfo` 默认视为敏感字段，未来同步时必须单独加密/脱敏。

### 5.2 用例与事务

建议新增：

- `SaveCollectibleUseCase`：校验、状态迁移、创建/更新时间、图片差异处理。
- `UpdateCollectibleStatusUseCase`：唯一状态迁移入口。
- `CalculatePortfolioSummaryUseCase`：统一所有财务指标和口径说明。
- `ExportBackupUseCase` / `ImportBackupUseCase`：校验、版本转换、事务、进度、清理。
- `ObserveCollectiblesUseCase(query, status, sort, page)`：统一搜索和筛选。

Repository 暴露领域接口；Room DAO 只处理实体和批量事务，BackupManager 不再直接持有 DAO。

### 5.3 数据库演进

推荐表结构：

- `collectibles`：主记录，保留生命周期和摘要。
- `collectible_images`：`collectibleId`、路径、排序、宽高、缩略图路径、哈希。
- `sales`（可选，二期）：一条藏品多次部分出售，替代单一 `sellPrice/sellQuantity`。
- `orders`（预售/拼团阶段）：付款节点、尾款、补邮、物流和提醒。

为 `ipName`、`seriesName`、`status`、`purchaseDate`、`sellDate` 建索引；搜索量大时使用 FTS5。每次 schema 变更都要有 Migration、备份兼容和回滚策略。

## 6. 界面与动画方案

原则是“反馈明确、动效服务信息层级、可关闭/可减弱”。所有动画默认 150–300ms，遵循系统 `Animator duration scale`，不使用无限循环装饰动画。

### 6.1 首页/列表

- 首次加载：卡片骨架屏淡入，真实数据到达时 `AnimatedContent` 交叉淡入（200ms）。
- 搜索/筛选：列表使用 `animateItemPlacement`；结果数量变化用 `AnimatedContent`，避免整页闪烁。
- 卡片按压：`combinedClickable` 配合轻微缩放 0.98 和阴影变化（100ms），长按进入批量模式时选中卡片加边框和半透明遮罩。
- 批量删除：先播放选中项向下淡出，再提交数据库；提供 Snackbar“已删除，撤销”并保留可撤销事务窗口。
- 空状态：插画/图标仅做一次 600ms 的淡入 + 上移，不做持续晃动。

### 6.2 表单与状态

- 表单分区使用 `AnimatedVisibility` 展开卖出信息；状态从非 SOLD 切换到 SOLD 时，价格/数量区域出现 200ms 高亮并滚动到第一个错误字段。
- 保存成功：顶部保存图标变为 Check，Snackbar 显示“已保存”；不要仅靠瞬时 Toast。
- 状态 Chip：颜色和文字同时过渡，`animateColorAsState` 150ms；状态迁移失败保持原状态并显示原因。
- 图片添加/删除：网格使用 `animateItemPlacement` + `fadeIn/fadeOut`；删除前提供撤销，真正删除文件延迟到撤销窗口结束。

### 6.3 图鉴

- 分组折叠用 `AnimatedVisibility`（展开 220ms，淡入 + 高度变化），Chevron 旋转 180°。
- 进入组详情使用共享元素（若稳定性不足，采用同名标题的淡入/上移过渡），返回时恢复滚动位置。
- 切换 IP/系列：保留搜索文本，列表使用 `AnimatedContent`，避免重建整个 Scaffold。

### 6.4 统计与备份

- 数字看板使用 400ms 的 `animateFloatAsState` 从旧值过渡到新值，金额保留两位小数。
- 月度图表按点位顺序 500ms 绘制线段，提供“跳过动画/减少动效”设置；数据变化时只更新变化月份。
- 导入导出显示明确阶段：读取 → 校验 → 复制图片 → 写入数据库 → 清理；进度条与“已处理/总数”同时展示，失败保留重试入口。
- 破坏性操作（删除、覆盖导入）使用确认对话框 + 错误色，不用夸张震动；可选一次短触觉反馈。

## 7. 测试与质量门禁

### 7.1 必补测试

1. **领域单测**：数量边界、部分出售、赠品/遗失、零成本、预期市值口径、状态迁移、金额舍入。
2. **备份安全测试**：路径穿越、绝对路径、重复文件名、空图片覆盖、损坏 ZIP、超大条目、版本不兼容、导入中途失败回滚。
3. **Room 测试**：查询组合（搜索 + 状态）、排序、迁移、批量事务、唯一指纹。
4. **ViewModel 测试**：表单字段错误、保存失败、状态更新的日期/更新时间、导入进度和取消。
5. **Compose UI 测试**：导航、筛选、批量选择/撤销、无障碍语义、暗色主题对比度。
6. **性能测试**：1k/10k 条藏品、9 张大图、连续搜索、批量导入和低端机滚动帧率。

### 7.2 CI 门禁

- PR 必须通过 `testDebugUnitTest`、lint、detekt/ktlint、assembleDebug。
- Release 分支必须通过 `assembleRelease`、签名校验、R8 mapping 上传和基本真机冒烟。
- 为导入包做安全扫描；不允许提交 keystore、买家信息样例和真实用户图片。

## 8. 分阶段实施计划

### 阶段 A：安全与正确性（1 个迭代，建议 1–2 周）

**目标：** 可安全发布 v1.3.1。

- 修复 Zip Slip、Zip Bomb 限制、OutputStream 空值误报。
- 修复 `createdAt`、状态迁移、组合筛选、`updatedAt`。
- 增加表单校验和金额/数量边界测试。
- 导入改为预校验 + 事务 + 批次图片清理。
- 补充备份模块单测和人工构造恶意 ZIP 的安全测试。

**验收：** 恶意包无法写出临时目录；导入失败数据库无半成品；编辑不改变创建时间；搜索与状态筛选结果可对账。

### 阶段 B：体验与性能（2–3 个迭代）

- 图片压缩、缩略图、EXIF 处理和分页/FTS 搜索。
- Loading/Error/Empty 三态、撤销删除、草稿箱、补款/补邮提醒。
- Material 3 DayNight、动态色、无障碍、动效和减少动效设置。
- 数据库 v2（图片独立表）及迁移工具。

**验收：** 1k 条数据列表滚动稳定；9 张大图录入不卡顿；断网/权限/空间不足都有可理解错误提示。

### 阶段 C：效率与交易辅助（中期）

- Excel 导入导出和模板版本化。
- 出谷文案生成（闲鱼/微博/小红书模板，用户确认后复制，不自动发布）。
- 运费/set/捆物分摊、订单节点、物流提醒、预算与月度预警。
- 换物、拼团记录采用独立交易模型，不污染单件藏品的基础成本。

### 阶段 D：云同步基础设施（先于社区）

- 账号、设备、同步游标、冲突版本（`updatedAt + deviceId + revision`）。
- 本地优先、离线可用、端到端加密备份；敏感买家信息默认不同步或加密。
- 删除采用 tombstone，避免多设备“删了又回来”；提供冲突预览和手动合并。

## 9. 基于 App 构建“谷子社区”的产品方向

建议把社区定位为“以个人藏品台账为基础的兴趣内容社区”，先做展示和内容沉淀，再谨慎进入交易，不要一开始做开放式二手市场。

### 9.1 内容类型

- **藏品卡/收藏柜分享**：从本地藏品生成可公开卡片，默认隐藏价格、店铺、买家和订单号。
- **开箱与晒单**：图文/短视频、购买渠道可选、标签（IP/系列/角色/品类）。
- **出谷记录**：展示状态变化和心得，交易金额默认仅自己可见。
- **攻略与知识**：展会、预售时间线、验货、保存、防潮、真假辨识、邮寄包装。
- **活动与同好**：线下展会、拼团/团购信息、同城交换（初期只做信息发布）。
- **愿望单/图鉴缺口**：用户公开“想收/已收”进度，形成 IP 图鉴和收藏成就。

### 9.2 社区 MVP（建议 3 个迭代）

1. 账号与个人主页（可匿名昵称）+ 公开藏品卡。
2. 帖子、图片、标签、点赞、收藏、评论和关注。
3. 举报、拉黑、内容审核、敏感词和版权投诉闭环。
4. 分享链接和二维码；未登录用户可查看公开内容但不能获取私密字段。

核心指标：公开卡片创建率、首帖发布率、7 日留存、有效互动率、举报处理时长，而不是单纯 PV。

### 9.3 后端建议

```text
App（本地 Room）
   ├─ Sync Gateway（鉴权、限流、设备游标）
   ├─ Content API（帖子、评论、标签、关注）
   ├─ Media Service（压缩、审核、CDN、EXIF 清理）
   ├─ Moderation（机审 + 人审 + 举报工单）
   └─ Search/Feed（按兴趣、IP、时间和质量分发）
```

内容实体至少包括 `authorId`、`visibility`、`contentStatus`、`createdAt`、`editedAt`、`moderationReason` 和软删除字段。图片上传必须服务端重新编码、去 EXIF、限制尺寸和类型。Feed 初期使用时间线 + 关注流，后续再引入质量/兴趣排序。

### 9.4 风险与治理

- **隐私**：买家信息、订单截图、地址和真实姓名禁止默认公开；发布前做字段级预览和脱敏提示。
- **版权**：用户上传官方图、杂志图、他人照片时提供来源字段、投诉和下架流程；不要默认承诺商业授权。
- **交易诈骗**：社区与交易分离；若未来做交易，必须有担保、争议、信用和风控，不直接复用“出谷文案”作为交易凭证。
- **未成年人**：限制公开联系方式和线下见面引导，增加年龄与安全提示。
- **刷量与骚扰**：频率限制、设备/IP 风控、评论折叠、拉黑和举报 SLA。
- **数据主权**：始终提供完整导出和账号注销；云端删除要覆盖媒体、索引和备份副本的保留策略。

### 9.5 社区商业化（后置）

可考虑会员云空间、高级统计、主题/皮肤、展会合作和品牌内容，但不应以售卖用户隐私数据或强制公开价格为代价。任何商业化都要保留本地核心功能可用。

## 10. 可执行 Backlog（按优先级）

**立即：** Zip Slip/Zip Bomb、release 签名、编辑创建时间、组合筛选、状态迁移、导入事务、表单校验。
**短期：** 图片管线、错误状态、金额分单位、数据库迁移、撤销删除、补款提醒、Excel。
**中期：** 文案生成、捆物分摊、预算、换物/拼团模型、应用锁。
**长期：** 云同步、端到端加密、公开藏品卡、内容社区、审核和搜索推荐。

每个 Backlog 项都应包含：产品口径、数据迁移、异常路径、埋点、单测/界面测试、灰度开关和回滚方案。建议先建立 `docs/adr/`，为财务口径、同步冲突、隐私公开字段和社区审核各写一份 ADR，避免未来继续在多个 ViewModel 中复制隐含规则。

## 11. 最终评价

项目已经从“能记录”走到了“能管理和统计”，v1.3.0 的部分售出核算和图鉴功能是很好的基础。当前最大的隐患不是功能数量不足，而是备份安全、状态/财务口径一致性和发布工程化不足。完成阶段 A 后再扩展提醒、文案和 Excel，完成云同步的数据模型后再开放社区内容，能显著降低返工、数据损坏和隐私事故的概率。
