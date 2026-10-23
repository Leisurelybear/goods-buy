# 动效系统（Motion）设计与落地清单

**日期：** 2026-10-23
**范围：** `ui/components/Motion.kt`、`ui/components/AnimationSpecs.kt` 与各页面的动效接入
**目标：** 在不牺牲可读性与性能的前提下，把「有反馈、有节奏、有仪式感」的动效铺到更多位置，提升使用体验。

## 1. 设计原则

1. **统一时序，不写魔法数字。** 所有时长/弹簧参数集中在 `AnimationSpecs.kt`，页面只引用命名规格。
2. **不参与测量。** 缩放、位移、透明度一律走 `Modifier.graphicsLayer`，不改变布局尺寸，避免动画引起的重排与抖动。
3. **动效只是"锦上添花"。** 信息在动画开始前就已可读；动画被系统"减少动态效果"关掉也不影响功能。
4. **克制。** 持续型动画（呼吸、流光、极光）幅度小、周期长（≥2.4s），只负责让界面"活着"，不抢注意力。
5. **可组合。** 新动效以 Modifier / 小组件形式提供，业务页面按需挂载，不做全局隐式魔法。

## 2. 新增基础设施

### `ui/components/Motion.kt`（新增）

| 能力 | API | 用途 |
|---|---|---|
| 按压回弹 | `Modifier.bounceClick(enabled, pressedScale, indication, onClick)`、`rememberPressScale(interactionSource)` | 卡片、设置行、FAB 的"按下缩小、松手弹回"手感 |
| 流光骨架 | `Modifier.shimmerBackground(shape, baseColor, highlightColor)` | 图片加载中占位、骨架屏 |
| 极光渐变 | `Modifier.auroraGradient(colors)` | Hero 横幅、FAB 的品牌底色缓慢流动 |
| 选中描边 | `Modifier.animatedBorder(visible, shape, brush, width)` | 卡片选中态描边从 0 弹性增长 |
| 依次揭示 | `StaggeredReveal(index, key, ...)` | 网格/列表项瀑布式入场 |
| 呼吸缩放 | `rememberBreathingScale(minScale, maxScale)` | 空状态图标等需要持续吸引注意的元素 |

### `ui/components/AppAsyncImage.kt`（新增）

统一图片组件：加载中显示流光骨架，加载完成 220ms 淡入，失败静默降级。
全站 `AsyncImage` 统一走它，避免"白块 → 硬切"的廉价感。

### `ui/components/CelebrationOverlay.kt`（新增）

轻量礼花层：纯 `Canvas` 绘制，不参与布局、不拦截触摸，用于保存成功、导入完成等"值得开心一下"的时刻。
动画结束由调用方把 `visible` 置回 `false`。

### `AnimationSpecs.kt`（扩充）

新增 `popSpec`（变化回弹）、`revealSpring`（揭示）、`pulseSpec`（呼吸）、`tabEnterSpec/tabExitSpec`（Tab 切换）、`pageEnterSpec/pageExitSpec`（二级页面）。

## 3. 落地位置清单

### 导航与骨架

- **底部 Tab 切换**（`NavGraph`）：淡入 + 轻微放大（0.94 → 1.0），退场放大到 1.02，形成"换板块"的空间感。
- **二级页面**（详情/表单/图鉴）：右侧推入 + 淡入；返回时当前页右推、上一级淡入并轻微放大。
- **底部导航图标**（`MainActivity`）：选中时放大 1.18 + 上浮 1dp，带弹簧回弹。
- **底部栏显隐**：滑入/滑出叠加淡入淡出。

### 列表与卡片

- **藏品卡片**（`CollectibleCard`）：按下回弹；图片用 `AppAsyncImage` 流光占位 + 淡入；选中时描边弹性出现、勾选图标缩放入场；多图轮播改为 320ms 交叉淡入。
- **网格入场**（`CollectibleListScreen` / `GalleryScreen`）：`StaggeredReveal` 按 index 每 45ms 递延（最多 270ms），形成瀑布式入场；`animateItemPlacement` 保留，排序/筛选后的位置变化依旧平滑。
- **空状态**（`EmptyState`）：入场淡入上浮，之后图标持续悬浮（±5dp）、外圈光晕明灭、图标呼吸缩放。
- **状态胶囊**（`StatusChip`）：颜色平滑过渡，状态变化时弹一下。
- **骨架块**（`SkeletonBox`）：由静态灰块改为流光。

### 看板与统计

- **数字**（`AnimatedNumber` / `StatNumber` / `AnimatedInt`）：滚动计数之外，数值变化时轻微弹一下（首次渲染不播放，避免页面刚进来就抖）；新增 `showSign` 支持盈亏 "+" 前缀。
- **Hero 横幅**（`HeroHeader`）：入场淡入上浮，底色改为缓慢流动的极光渐变。
- **统计页**（`StatisticsScreen`）：Hero 累计盈亏改为带动画的 `StatNumber`；四张指标卡依次揭示；分类卡片新增"占投入"进度条，从 0 长到目标比例。

### 表单与设置

- **表单**（`CollectibleFormScreen`）：缩略图改用 `AppAsyncImage`；保存成功后先播放 650ms 礼花再返回，让"保存成功"有仪式感。
- **「我的」页**（`ProfileScreen`）：主页 / 草稿箱 / 设置三个层级改为左右滑入滑出（方向跟随前进/返回）；设置行按下回弹；导入成功后播放礼花。
- **详情页**（`CollectibleDetailScreen`）：图片缩略图依次揭示 + 流光占位；快捷状态胶囊颜色过渡并放大选中项。

## 4. 性能与无障碍

- 缩放/位移/透明度全部走 `graphicsLayer`，不触发重排。
- 持续型动画（流光、极光、呼吸）周期长、幅度小；`StaggeredReveal` 的延迟有上限，避免长列表末尾等待过久。
- 礼花层不注册 `pointerInput`，不吞触摸事件。
- 文案与语义树不受影响：动效只改变绘制层，`contentDescription` / 语义分组维持原样。

## 5. 后续可扩展位置（未做，留作备选）

- `ImportPreviewScreen` / `EdgeFadeEditScreen` 的进度与预览过渡。
- 搜索框聚焦时的宽度/阴影过渡、清除按钮出现动画。
- 详情页图片的共享元素（shared element）大图展开。
- 系统"减少动态效果"开关接入（`LocalMotionDurationScale` / `Settings.Global.ANIMATOR_DURATION_SCALE`）后自动降级。