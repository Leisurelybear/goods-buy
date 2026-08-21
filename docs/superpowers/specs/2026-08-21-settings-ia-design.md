# 设置页信息架构重组设计（Settings IA Redesign）

日期：2026-08-21
状态：已确认（用户批准）

## 背景与问题

当前全部设置项塞在「我的 → 显示设置」单张卡片内，靠分割线分隔，存在：

1. **名不副实**：日志、草稿自动保存、图鉴入口等与「显示」无关的项混在「显示设置」下。
2. **无分组**：14 个偏好项 + 2 个日志动作平铺一张卡，扫读性差。
3. **组件不统一**：开关用裸 Row+Switch，日志两行是手写 Row，未走 ListRowItem。
4. **细节瑕疵**：卡内标题与 TopAppBar 重复；「设置」行箭头由 ArrowBack 旋转冒充；
   「关于谷的拜」空 onClick 渲染成可点但无响应；步进器 `-`/`−` 字符不一致且无边界禁用态；多处缩进错乱。

## 目标 / 非目标

**目标**
- 单页分组式设置页：五组卡片，组名即用户心智。
- 行控件统一走 ListRowItem；步进器统一图标与边界态。
- 补上 themeId 的 UI 入口（主题选择器占位）。
- 顺带修复上述细节瑕疵。

**非目标（YAGNI）**
- 不拆多子页；不加设置搜索；不做主题预览缩略图；
- 排序字段/方向不进设置页（维持首页排序栏就地编辑）。

## 信息架构

入口：「我的」页「设置」行保留，副标题改为 *外观 · 首页 · 图鉴 · 日志*。
页面：TopAppBar 标题「设置」（原「显示设置」）；删除卡内重复标题。

```
设置
├─ 外观
│   ├─ 主题            ListRowItem，trailing=当前主题名，点击弹单选对话框
│   ├─ 每行展示数量     步进器 1–4
│   ├─ 卡片大小         步进器 100–200dp，步长 20
│   ├─ 字体大小         FilterChip 小/中/大
│   ├─ 显示名称         Switch 行
│   ├─ 显示价格         Switch 行
│   └─ 显示状态         Switch 行
├─ 首页行为
│   ├─ 显示排序栏       Switch 行
│   ├─ 多图自动轮询     Switch 行
│   └─ 轮询间隔         步进器 1–60 秒；关闭轮询时整行禁用
├─ 编辑与草稿
│   └─ 自动保存间隔     SegmentedButton 0.5 / 1 / 2 秒
├─ 图鉴
│   └─ 入口位置         SegmentedButton 藏品柜 / 我的
└─ 日志
    ├─ 启用记录         Switch 行
    ├─ 导出日志         ListRowItem（仅当存在 app.log 或 crash.log）
    └─ 删除日志         ListRowItem，error 色（同上条件；沿用确认对话框）
```

## 组件约定

- **组容器**：`SectionHeader(title)` + `Card { Column(spacedBy(0)) }`，行间用 HorizontalDivider（与「我的」主页一致）。
- **Switch 行**：`ListRowItem(title, subtitle?, trailing = { Switch(...) })`，不再使用 SettingToggleRow（删除该私有组件）。
- **步进器行**：`ListRowItem(title, subtitle?, trailing = 步进器)`；步进器统一 `Icons.Default.Remove` / `Icons.Default.Add`，
  到边界时 `enabled = false`（替换现有文本 `-`/`−`）。
- **主题对话框**：`AlertDialog` + RadioButton 列表，数据源 `AppThemes.all`（id+label），
  确认后 `prefs.copy(themeId = id)` + save。当前仅一套主题，UI 天然渲染为单项。
- **箭头**：可点击行的指示统一 ChevronRight（ListRowItem 内部已有），禁止再用旋转 ArrowBack。
- **关于谷的拜**：改为纯展示（不传 onClick，ListRowItem 不渲染点击态与箭头）。

## 数据流（不变）

- 沿用 `var prefs by remember { mutableStateOf(preferencesRepository.preferencesState.value) }`
  + 每次修改 `preferencesRepository.save(prefs)`。
- 主题即时生效链路已具备：MainActivity 在组合中读取 `preferencesState.value.themeId`
  （Compose State 订阅触发重组）→ `AppThemes.byId(themeId)`，非法 id 回退梦幻粉紫。
- 日志开关仍同步调用 `AppLogger.setEnabled(it)`；导出/删除逻辑不变。

## 边界与错误处理

- 无日志文件时隐藏导出/删除两行（现状逻辑保留）。
- 删除日志保留现有 AlertDialog 二次确认。
- themeId 持久化值非法时由 `AppThemes.byId` 兜底，不崩溃。
- 轮询间隔行在 `homeImageAutoRotate=false` 时 enabled=false（视觉置灰，不可交互）。

## 测试与验收

- 现有 58 个单元测试保持通过（本改动为纯 UI 层，无可单测的新领域逻辑）。
- 手动验收清单：
  1. 五组各自修改一项 → 杀进程重开，全部持久化。
  2. 主题对话框选择后立即生效（当前仅一项，验证对话框交互与持久化即可）。
  3. 关闭「多图自动轮询」→ 轮询间隔行置灰。
  4. 无日志文件时不显示导出/删除；开启日志并产生文件后出现。
  5. 「关于谷的拜」不可点击、无箭头；各可点行箭头为 ChevronRight。
  6. 步进器到边界按钮置灰；1–4 / 100–200 / 1–60 边界正确。

## 涉及文件

- 修改：`app/src/main/java/com/goodsbuy/app/ui/profile/ProfileScreen.kt`（主要工作量）
- 复用：`ui/components/ListRowItem.kt`、`SectionHeader.kt`、`theme/AppThemes`
- 不改：PreferencesRepository（字段已齐备）、MainActivity、其余屏幕
