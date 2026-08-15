# 图鉴模式折叠 + 组内横向滑动

## 目标

优化已上线的图鉴模式（v1.1.0）交互体验：
1. 每个分组（含「未分类」组）的组头可点击折叠/展开，折叠状态在会话内记忆，退出页面或切换维度后重置为全部展开。
2. 组内藏品从「网格矩阵」改为「一行横向可滑动」（LazyRow），卡片尺寸沿用收藏柜 `prefs.cardSize` 设置。

不改动 ViewModel、分组逻辑与数据层。

## 功能清单

### 1. 组头折叠/展开
- `GalleryGroupHeader` 变为可点击，点击 toggle 该组的折叠状态。
- 折叠时组体不渲染；展开时渲染横向滑动行。
- 组头显示折叠箭头图标：展开时 `Icons.AutoMirrored.Filled.KeyboardArrowUp`（或 `ExpandLess`），折叠时 `KeyboardArrowDown`（或 `ExpandMore`）。
- 右侧「N 件」计数保留。
- **折叠状态存储**：Composable 内 `remember { mutableStateOf(setOf<String>()) }` 保存已折叠的组名集合；`UNCATEGORIZED_NAME`（"未分类"）组同样参与折叠。
- 切换 `GroupBy` 或页面重建后自动重置为全部展开（会话内记忆，不持久化）。

### 2. 组内横向滑动
- 页面主体从 `LazyVerticalGrid` 改为 `LazyColumn`。
- 每个 `GalleryGroup` 是一个 section：
  - 组头 `item`（全宽）。
  - 若组未折叠，追加 `item` 内含一个 `LazyRow`，每项为 `CollectibleCard`，卡片尺寸 `prefs.cardSize.dp`。
- 交互不变：点卡片进详情，长按弹 `LongPressMenu`（无批量选择），删除确认对话框不变。
- 空数据 → `EmptyState`。

## 不做的事

- 不持久化折叠状态。
- 不提供「全部展开/全部折叠」按钮。
- 不改 ViewModel / GalleryGrouping / 数据层。
- 不新增纯函数单测（折叠逻辑为纯 UI 状态）。

## 测试

- 既有 `GalleryGroupingTest`（6）、`GalleryViewModelTest`（6）、`CollectibleNameUtilsTest`（3）不受影响。
- 编译验证：`compileDebugKotlin` / `testDebugUnitTest` / `assembleDebug` 通过。
