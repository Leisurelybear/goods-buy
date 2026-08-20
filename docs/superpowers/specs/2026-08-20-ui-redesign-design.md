# 谷的拜（GoodsBuy）UI 现代化重构设计

**日期：** 2026-08-20
**状态：** 已获用户确认（方向 / 暗色 / 深度 / 组件 / 布局 / 动效）
**目标版本：** v1.5.0

## 1. 背景与目标

当前 UI 使用 Material 3 默认紫色模板主题，视觉停留在"模板 App"阶段：默认配色、卡片堆叠、扁平无层次。目标是做一次全量现代化重构，让 App 符合现代应用风格，同时保留全部既有功能。

**已确认决策：**
- 设计方向 **A · 梦幻粉紫渐变**（少女感、柔和渐变、奶油底色，贴合吃谷圈审美）
- 暗色模式 **暗夜紫**（深紫夜空底 + 亮粉紫渐变，与浅色同源）
- 深度 **全量现代化重构**（所有屏幕重设计，不只换肤）
- 架构 **主题体系先行 + 逐屏改造**（主题可扩展，未来支持多皮肤零成本）

## 2. 主题体系

### 2.1 多主题架构

新增 `ThemeConfig` 数据类，一个主题 = 一套完整配置：

```kotlin
data class ThemeConfig(
    val id: String,
    val label: String,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme,
    val brandGradient: List<Color>,   // 浅色模式品牌渐变
    val darkBrandGradient: List<Color>, // 深色模式品牌渐变
    val shapes: AppShapes
)
```

- `GoodsBuyTheme(theme = ...)` 作为入口，接收 `ThemeConfig` 并提供 `LocalAppTheme` CompositionLocal，供全局读取品牌渐变等扩展 token。
- 本次内置默认主题「梦幻粉紫」（`id = "dreamy_purple"`），浅色 + 暗夜紫深色成对。
- 未来多皮肤：在「我的 → 显示设置」加主题选择器，`PreferencesRepository` 持久化主题 id，新增主题只需新增一份 `ThemeConfig`，业务代码零改动。
- 深浅模式跟随系统（`isSystemInDarkTheme()`），暂不提供应用内手动深浅切换（可后续加）。

### 2.2 配色 token（梦幻粉紫）

| Token | 浅色 | 深色（暗夜紫） |
|---|---|---|
| primary | `#C77DFF` | `#B388FF` |
| secondary | `#FF8FAB` | `#FF9EBB` |
| tertiary | `#FFB3D9` | `#FFB3D9` |
| background | `#FFF0F5` | `#1F1730` |
| surface | `#FFF5F9` | `#251B3A` |
| surfaceVariant | `#F7E6EF` | `#2A1E3F` |
| surfaceContainer | `#FBEAF2` | `#2E2342` |
| onSurface | `#4A4A5A` | `#F0EAFB` |
| onSurfaceVariant | `#7A7286` | `#B8AFCB` |
| primaryContainer | `#F0DFFF` | `#3A2A5A` |
| onPrimaryContainer | `#3D1F63` | `#F0DFFF` |
| secondaryContainer | `#FFDDE8` | `#4A2A45` |
| onSecondaryContainer | `#5E1F3A` | `#FFDDE8` |
| 品牌渐变 | `#FF9EBB → #B388FF` | `#6A3E9C → #B388FF` |

状态色（保留现有 10 个色值）以「彩色圆点 + 浅色底胶囊」形式呈现，不再用硬色块。

### 2.3 字体（Typography）

- 沿用系统字体，扩展现有 `Typography`：
  - `displaySmall` 28sp/粗，用于 Hero 数字与页面大标题
  - `titleLarge` 20sp/粗，页面分区标题
  - `titleMedium` 16sp/粗，卡片标题
  - `bodyMedium` 15sp，正文
  - 金额/统计数字使用加粗 + 固定宽度感（`FontFeatureSettings` 或等宽数字），保证滚动时数字不错位

### 2.4 形状（Shapes）

- 卡片圆角 20dp
- 按钮 28dp（胶囊）
- 输入框 16dp
- 图片角 12dp
- 小型 chip 8dp

## 3. 组件库（ui/components 扩展）

全部基于现有 M3 组件改造，不引入新依赖（Vico 图表保留）。每个组件独立 `@Composable`，逐步替换旧实现。

| 组件 | 说明 |
|---|---|
| `HeroHeader` | 页面顶部品牌渐变横幅（大圆角卡），承载看板数字/欢迎语；可复用渐变 token |
| `CollectibleCard`（改造） | 圆角 16dp；底部信息栏由纯黑条改为半透明渐变遮罩；状态改彩色圆点 |
| `StatusChip`（改造） | 彩色圆点 + `secondaryContainer` 浅色底胶囊 |
| `SearchBar` | 胶囊形，`surfaceContainer` 底无边框，`Search` 图标 + 清除按钮 |
| `FilterChip`（统一） | 选中态主题色填充，状态筛选带彩色圆点 |
| `GradientCard` | 渐变底信息卡（统计/看板用），渐变来自 `ThemeConfig.brandGradient` |
| `StatNumber` | 加粗数字 + ¥ 前缀，复用 `AnimatedNumber` 滚动动画 |
| `FAB` | 主题渐变、28dp 圆角，内容自定义（「+」/ 图标） |
| `ListRowItem` | 设置/列表行：彩色圆形图标底 + 标题 + 副标题 + 右箭头 |
| `EmptyState`（改造） | 大圆角插画卡 + 引导文案 + 可选按钮 |
| `SectionHeader` | 分区标题（左对齐 + 可选副标题） |
| `SkeletonBox` | 柔和骨架屏占位（渐变 shimmer），加载态使用 |

## 4. 各屏幕布局

### 4.1 藏品柜（首页）

- 顶部 `HeroHeader` 品牌渐变横幅：标题「谷的拜」+ 总持仓价值 & 盈亏率速览
- 胶囊 `SearchBar` + 状态 `FilterChip`（彩色圆点）
- 排序控制保留（可折叠），样式统一
- 网格 `CollectibleCard`（现代化）
- 右下角渐变 `FAB`「+」添加藏品
- 底部导航保留（藏品柜 / 统计 / 我的）
- 批量模式与长按菜单沿用现有交互，仅换样式

### 4.2 统计

- 顶部 `HeroHeader` 大字展示总盈亏（金额 + 盈亏率）
- 总览改为 2×2 `GradientCard`：总投入 / 总营收 / 累计盈亏 / 持仓市值
- 月度趋势图 `MonthlyTrendChart` 卡片化
- 分类统计：`ListRowItem` + 盈亏色，附数量进度条
- 筛选 chip 统一

### 4.3 我的

重构为「设置中心」：
- 顶部品牌卡片（App 名 + 版本号）
- 分组 `ListRowItem`：
  - **数据**：导出备份 / 导入备份
  - **草稿箱**（数量角标）
  - **显示设置**（每行数量/卡片大小/字体/显示开关/草稿间隔/轮询/图鉴入口/日志）
  - **图鉴入口**（非首页时显示）
  - **关于**
- 设置/草稿保留现有页内切换模式，仅换新样式，不改导航结构

### 4.4 图鉴

- 分组头卡片化：封面缩略图 + 组名 + 数量 + 展开箭头
- 搜索胶囊化
- 折叠/展开动画、组内网格保留，卡片用新样式

### 4.5 详情

- 顶部大图 Hero（圆角底部、状态 chip 浮层、多图轮播保留）
- 快捷改状态 = 彩色圆点 chip 行
- 基础 / 购入 / 卖出 / 盈亏分区卡片 + `SectionHeader`

### 4.6 表单

- 分区式：图片 → 基础信息 → 购入信息 → 卖出信息 → 盈亏
- 输入框统一 16dp 圆角；分区 `SectionHeader`
- 保存按钮品牌渐变胶囊

## 5. 动效与交互

- **主题切换**：切换浅/深色（未来切皮肤）用 `animateColorAsState` 平滑过渡配色
- **导航过渡**：统一滑动 + fade（250–300ms），Tab 间淡入淡出，层级页左右滑入
- **卡片**：按压缩放 0.97 + 阴影提升；批量选中轻微上浮 + 圆点选中态
- **数字**：`AnimatedNumber` 滚动过渡 400ms
- **列表**：`animateItemPlacement`（排序/筛选平滑移动）
- **加载/空态**：骨架屏（粉紫 shimmer）；空态插画卡淡入
- **无障碍**：遵循系统「减弱动态效果」；动画默认 150–300ms、不循环；装饰动画不无限播放

## 6. 实现范围与文件改动

涉及文件（预计）：

- `ui/theme/`：重写 `Theme.kt`、`Color.kt`、`Type.kt`；新增 `ThemeConfig.kt`、`AppShapes.kt`、渐变 token
- `ui/navigation/`：`MainActivity.kt` 底部栏样式、FAB 挂载点
- `ui/components/`：新增 `HeroHeader.kt`、`SearchBar.kt`、`GradientCard.kt`、`StatNumber.kt`、`AppFAB.kt`、`ListRowItem.kt`、`SectionHeader.kt`、`SkeletonBox.kt`；改造 `CollectibleCard.kt`、`StatusChip.kt`、`EmptyState.kt`
- 各屏幕：`CollectibleListScreen`、`StatisticsScreen`、`ProfileScreen`、`GalleryScreen`、`CollectibleDetailScreen`、`CollectibleFormScreen`、`ImportPreviewScreen`、`EdgeFadeEditScreen` 应用新组件与主题
- `ui/preferences/`：`PreferencesRepository` 预留主题字段（本次不暴露 UI，仅预留）

**不做的事：** 不新增业务功能；不改变数据层与领域逻辑；不改动备份格式；不引入新依赖；不改底部导航的 3 个 Tab 结构。

## 7. 验证

- `./gradlew assembleDebug` 构建通过
- `./gradlew test` 既有单测全部通过（不得因换肤破坏）
- 真机/模拟器冒烟：浅色 + 暗色两套主题下，所有屏幕可达、无文字对比度问题
- 批注：本次为纯 UI 改动，Room/领域/备份代码零改动