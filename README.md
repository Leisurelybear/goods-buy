# 谷的拜 (GoodsBuy)

面向二次元「吃谷」群体的周边全生命周期管理工具，一站式记录买入/卖出、自动统计盈亏。

当前版本：**v1.3.1**

## 功能（MVP）

- **藏品管理** — 录入、编辑、分类管理；支持订单状态（持有 / 已售 / 待出）与图片附件
- **盈亏统计** — 自动计算累计、月度、分类维度的收支与利润；可视化图表展示
- **数据看板** — 总投资、总营收、持仓价值、盈亏率一目了然
- **橱窗展示** — 网格化藏品列表，自定义每行列数和卡片大小
- **备份与恢复** — ZIP 一键备份（含图片）、导入恢复；导入前预览并支持去重/覆盖/新增策略
- **批量操作** — 列表长按进入批量模式，批量改状态/删除
- **图鉴模式** — 按 IP/系列分组查看藏品，支持搜索、折叠/展开、进入单个分组和横向浏览；可在设置中开启首页切换入口

## 截图

<!-- 放置截图到 docs/screenshots/ 目录，并取消下面的注释 -->

### 藏品列表（橱窗模式）
<!-- ![藏品列表](docs/screenshots/collection_grid.png) -->

### 藏品详情
<!-- ![藏品详情](docs/screenshots/collectible_detail.png) -->

### 添加/编辑藏品
<!-- ![添加藏品](docs/screenshots/add_collectible.png) -->

### 盈亏统计
<!-- ![盈亏统计](docs/screenshots/statistics.png) -->

### 数据看板
<!-- ![数据看板](docs/screenshots/dashboard.png) -->

### 设置页面
<!-- ![设置页面](docs/screenshots/settings.png) -->

## 技术栈

| 类别 | 技术选型 |
|------|----------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room 2.6.x |
| 依赖注入 | Hilt |
| 导航 | Compose Navigation |
| 图片加载 | Coil |
| 图表 | Vico |
| 异步 | Coroutines + Flow |

## 环境要求

- minSdk: 28 (Android 9)
- JDK 17
- Android SDK 34

## 构建

```bash
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 架构

MVVM + Clean Architecture 分层：

```
表现层（Compose 界面 + ViewModel）
      ↓
  领域层（Use Case + Repository 接口）
      ↓
   数据层（Room 数据库 + 本地文件存储）
```

## License

[更新记录](CHANGELOG.md)

[English README](README_en.md)

## 许可证

MIT
