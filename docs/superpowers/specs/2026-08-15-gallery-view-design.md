# 图鉴模式设计

## 目标
新增「图鉴模式」：按 IP / 系列对藏品分类分组，组头展示名称与件数统计，帮助用户一眼看清自己的收藏结构（如某个 IP 下有几个系列、每个系列有几件）。不涉及进度/目标套数统计。

## 功能清单

### 1. 入口（可设置切换）
新增偏好项「图鉴入口位置」，可选「首页」或「我的」，默认「我的」。存放于 `GridPreferences`（新增布尔字段 `galleryEntryHome: Boolean = false`，表示入口是否放首页）。

- **入口在「我的」**（默认）：`ProfileScreen` 主卡片内新增一行「图鉴模式」，图标 `Icons.Default.PhotoLibrary`，副标题「按 IP/系列分类查看」，点击跳转图鉴页。
- **入口在「首页」**：`CollectibleListScreen` 搜索栏上方显示「网格 | 图鉴」两个 `FilterChip`（与现有状态筛选 chip 风格一致），点击「图鉴」跳转图鉴页；此时「我的」页隐藏「图鉴模式」行。
- 设置 UI：`ProfileScreen` 的「显示设置」卡片内新增一行「图鉴入口位置」，用两个 `FilterChip`（首页 / 我的）切换，写入偏好并持久化。

### 2. 图鉴页（独立路由）
- 新路由 `Screen.Gallery : Screen("gallery")`，注册到 `NavGraph`。
- `TopAppBar`：返回按钮 + 标题「图鉴模式」。
- 顶部两个 `FilterChip`：「按 IP」/「按系列」，切换分组维度（与首页状态筛选风格一致）。
- 分组列表主体：`LazyColumn`，每组一个区块：
  - 组头：组名 + 「N 件」。
  - 组内：藏品网格，复用 `CollectibleCard`，列数沿用 `prefs.columns`。
- 空数据 → `EmptyState`。

### 3. 分组逻辑
- 数据源：`CollectibleRepository.getAllCollectibles()`（现有方法，零数据库改动）。
- 分组维度：
  - **按 IP**：key = `collectible.ipName`。
  - **按系列**：key = `collectible.seriesName`。
- key 为空白字符串 → 归入「未分类」组。
- 组排序：按组内件数降序；「未分类」固定排最后。
- 组内藏品排序：沿用 `ipName` → `seriesName` → `createdAt DESC` 的顺序（稳定即可，不额外引入排序 UI）。

### 4. 交互
- 点藏品卡片 → 进入详情页（`Screen.CollectibleDetail`，复用现有导航）。
- 长按藏品卡片 → 弹出现有 `LongPressMenu` 的「快速改状态 / 进入编辑 / 复制藏品 / 删除」四项（**不包含**「批量选择」按钮，批量是藏品柜网格视图的能力）。`GalleryViewModel` 提供对应方法：`quickUpdateStatus`、`duplicateCollectible`（复用 `CollectibleNameUtils`）、`batchDeleteSingle`（删除）。
- 默认维度：按 IP。
- `LongPressMenu.kt` 新增参数 `showBatchSelect: Boolean = true`：为 `false` 时隐藏底部的「批量选择」行（藏品柜仍传默认 `true`，图鉴页传 `false`）。

### 5. 状态与 ViewModel
新增 `GalleryViewModel` + `GalleryUiState`：

```kotlin
enum class GroupBy { IP, SERIES }

data class GalleryGroup(
    val name: String,          // 组名，「未分类」固定文案
    val count: Int,
    val collectibles: List<Collectible>
)

data class GalleryUiState(
    val groupBy: GroupBy = GroupBy.IP,
    val groups: List<GalleryGroup> = emptyList(),
    val isLoading: Boolean = true
)
```

`GalleryViewModel` 暴露：
- `uiState: StateFlow<GalleryUiState>` — combine 全量藏品 Flow 与 `_groupBy` 后做分组聚合。
- `fun setGroupBy(GroupBy)` — 切换维度。

分组聚合为纯函数，便于单元测试。

### 6. 不需要改动的文件
- `AppDatabase` / `CollectibleDao` / `CollectibleRepository` — 复用现有查询。
- `CollectibleDetailScreen` / `CollectibleFormScreen` — 复用。
- `MainActivity` — 底部导航不变（图鉴是独立页，不进底部导航）。

## 测试

### 单元测试（app/src/test）
新增 `GalleryGroupingTest`，覆盖分组纯函数：
- 按 IP 分组：key 正确、件数正确。
- 按系列分组：key 正确、件数正确。
- 空 key 归入「未分类」。
- 组排序：按件数降序，「未分类」固定最后。
- 空列表 → 空分组。
- 含空白字符的 key（`"  "`）按空白处理归入「未分类」。

## 入口偏好持久化
- `GridPreferences` 新增字段 `galleryEntryHome: Boolean = false`。
- `PreferencesRepository` 新增 `PREF_GALLERY_ENTRY_HOME = "gallery_entry_home"` 的读写。
- `CollectibleListScreen` 与 `ProfileScreen` 根据该字段决定是否显示对应入口。
