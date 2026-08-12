# 长按藏品菜单与批量选择设计

## 目标
在藏品列表页（橱窗）为每张卡片增加长按交互：弹出 Bottom Sheet 提供快速操作；从菜单进入批量选择模式，支持勾选多张卡片并批量删除。

## 功能清单

### 1. 长按 Bottom Sheet
长按任意藏品卡片 → 弹出 `ModalBottomSheet`，包含四个操作项：

| 操作 | 行为 |
|------|------|
| 快速修改状态 | 点击后直接显示所有 `OrderStatus` chip，点击即更新（无需进入详情页） |
| 进入编辑 | 导航到现有 `CollectibleFormScreen`（传入藏品 ID） |
| 复制藏品 | 克隆整条记录：所有字段 + 图片文件复制到内部存储，名称追加 ` 2026-08-12 14:30` 后缀（yyyy-MM-dd HH:mm），插入数据库 |
| 删除 | 弹出 `AlertDialog` 二次确认后再删除 |

### 2. 批量选择模式
**入口**：Bottom Sheet 中「批量选择」按钮。

**交互流程**：
1. 点击「批量选择」→ ViewModel 设置 `isBatchMode = true`
2. `CollectibleListScreen` 检测到 `isBatchMode`，切换 TopAppBar 为批量操作栏：
   - 显示已选数量「已选 3 项」
   - 「删除 N 个」按钮 → 确认后批量删除
   - 「取消」按钮退出批量模式
3. 卡片上叠加半透明勾选遮罩（右上角圆形勾选图标）
4. 点击卡片切换选中状态（不触发详情跳转）
5. 退出批量模式时清空 `selectedIds`

### 3. 数据模型变更

#### CollectibleListUiState（新增字段）
```kotlin
data class CollectibleListUiState(
    val collectibles: List<Collectible> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedStatusFilter: String? = null,
    val isBatchMode: Boolean = false,       // 新增
    val selectedIds: Set<Long> = emptySet() // 新增
)
```

### 4. ViewModel 变更（CollectibleListViewModel）

新增方法：
- `enterBatchMode()` — 进入批量模式，选中当前触发的藏品
- `toggleSelect(id: Long)` — 切换单个藏品的选中状态
- `exitBatchMode()` — 退出批量模式
- `batchDelete()` — 批量删除，每次调用 `repository.deleteCollectible(id)`
- `duplicateCollectible(id: Long)` — 复制藏品（含图片复制），插入新记录
- `quickUpdateStatus(id: Long, newStatus: OrderStatus)` — 快速修改状态

### 5. Repository 变更

`CollectibleRepository` 新增：
```kotlin
suspend fun duplicateCollectible(id: Long): Long
```

实现需在 `CollectibleRepositoryImpl` 中完成：
- 查询原藏品
- 创建新对象（id=0，name 追加时间戳，createdAt/updatedAt 用当前时间）
- 复制所有图片文件（`ImageUtils.copyImage` 或手动 File copy）
- 插入数据库，返回新 id

### 6. UI 组件变更

#### CollectibleCard.kt
新增参数：
- `onLongPress: (() -> Unit)? = null` — 长按回调
- `isSelected: Boolean = false`
- `onSelect: (() -> Unit)? = null` — 批量模式下点击回调
- `batchMode: Boolean = false` — 是否处于批量模式

选中时：右上角叠加半透明背景 + 勾选图标；`clickable` 改为根据 `batchMode` 决定触发 `onClick` 还是 `onSelect`。

#### CollectibleListScreen.kt
- 卡片传入 `onLongPress`、`isSelected`、`onSelect`、`batchMode`
- 根据 `uiState.isBatchMode` 切换 TopAppBar
- 长按弹窗用 `ModalBottomSheet` + 菜单项列表

### 7. 时间戳格式
名称后缀格式：` 2026-08-12 14:30`（yyyy-MM-dd HH:mm，不含秒，空格分隔）

### 8. 不需要改动的文件
- `CollectibleDetailScreen.kt` / `CollectibleDetailViewModel.kt` — 复用现有逻辑
- `CollectibleFormScreen.kt` / `CollectibleFormViewModel.kt` — 复用
- `NavGraph.kt` — 无需新路由
- `ImageUtils.kt` — 复用 `copyImageToInternalStorage` 或直接用标准文件复制
