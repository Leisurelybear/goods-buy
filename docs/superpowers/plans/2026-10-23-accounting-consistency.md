# 口径一致性与组合筛选实施计划（Stage 1）

**日期：** 2026-10-23
**目标版本：** v1.4.2
**来源：** `docs/code-review-and-next-steps-2026-10-23.md` 阶段 1

## 背景

二次 Review 确认上轮安全问题已修复，剩余隐患集中在"数据可信度"：状态迁移三处实现不一致导致 `sellDate` 漂移、搜索与状态筛选互斥、持仓市值按单价累加、分类统计分母口径不同。本阶段只做正确性收敛，不加新功能。

## 任务

### T1 统一状态迁移

- 新增 `domain/usecase/UpdateCollectibleStatusUseCase.kt`。
- 纯函数 `migrate(collectible, newStatus, now)`：
  - `newStatus == SOLD && sellDate == null` → `sellDate = now`
  - `newStatus == SOLD && sellDate != null` → 保留原 `sellDate`
  - `newStatus != SOLD` → `sellDate = null`
  - 恒更新 `updatedAt = now`
- `suspend operator invoke` 负责持久化，返回更新后的对象。
- 接入 `CollectibleListViewModel.quickUpdateStatus`、`GalleryViewModel.quickUpdateStatus`、`CollectibleDetailViewModel.updateStatus`；`CollectibleFormViewModel.save` 改用 `migrate`。

### T2 组合筛选

- DAO 新增 `observeCollectibles(query: String, status: String?)`：`(:status IS NULL OR status = :status)` 与 LIKE 条件同时生效。
- Repository 暴露同名方法；删除被取代的 `searchCollectibles` / `getCollectiblesByStatus`。
- 列表 VM 合并为单一 `flatMapLatest`，同时传 `query` 与 `status`。
- 同步更新测试中的 `FakeCollectibleRepository`。

### T3 `expectedPrice` 语义

- 明确定义为**预期单价**。
- `GetDashboardSummaryUseCase.holdingValue` 改为 `Σ(expectedPrice × remainingQuantity)`（仅非结算状态）。
- 表单/详情文案标注「（单价）」。

### T4 分类统计口径

- `CategoryStat` 拆分为 `grossInvestment` / `realizedCost` / `holdingValue` / `revenue` / `profit`。
- 利润率分母统一为 `realizedCost`（与 `ProfitLossCalculator` 一致）。
- UI 同时展示购入成本与已结算成本。

### T5 测试

- `UpdateCollectibleStatusUseCaseTest`：进入 SOLD 写日期、重复 SOLD 保留、离开 SOLD 清空、`updatedAt` 刷新。
- `GetDashboardSummaryUseCaseTest`：持仓市值 = 单价 × 剩余数量；利润与利润率。
- `GetCategoryStatsUseCaseTest`：各口径字段与利润率分母。
- `CollectibleListViewModelTest`：搜索与状态筛选同时生效。

## 明确不做

- 数据库索引与 Migration：搜索使用前导通配 `LIKE '%x%'`，B-tree 索引无法命中；加 `@Index` 需改 schema + 升 version + Migration，风险收益不匹配。索引 / FTS5 / `exportSchema` 归入 Stage 2 数据层专项。
- 搜索 debounce：与列表页 `TextFieldValue` 双向同步存在冲突风险，单独评估。

## 验收

- `./gradlew testDebugUnitTest` 通过。
- `./gradlew assembleDebug` 通过。
- 手工核对：同一藏品从列表/图鉴/详情/表单改状态后 `sellDate` 结果一致。
