# 谷柜 APP 设计文档

## 1. 概述

### 1.1 产品定位

面向二次元 "吃谷" 群体的周边全生命周期管理工具，MVP 阶段聚焦核心台账管理与盈亏统计。

### 1.2 MVP 范围

**包含**:
- 模块一：藏品全生命周期台账管理（录入、编辑、状态管理、筛选搜索、图片）
- 模块三：智能盈亏与收支统计（自动计算、数据看板、分类统计、时间趋势）

**不包含**（后续迭代）:
- 模块二：可视化藏品柜（多视图、图鉴分组、趣味交互）
- 模块四：效率辅助工具（识图录入、出谷文案、换物管理）
- Excel 导入导出
- 云端同步

### 1.3 构建环境

| 组件 | 路径/版本 |
|------|-----------|
| Android SDK | `G:\AndroidSDK` |
| Platforms | android-34, android-36 |
| Build Tools | 33.0.1, 36.1.0 |
| JDK 17 | `E:\AndroidStudio\jbr` |
| Gradle | 8.2 |

### 1.4 技术栈

| 类别 | 技术选型 |
|------|----------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| 数据库 | Room 2.6.x |
| 依赖注入 | Hilt |
| 导航 | Compose Navigation |
| 图片加载 | Coil |
| 图表 | Vico |
| 异步 | Kotlin Coroutines + Flow |
| 日期 | kotlinx-datetime |

### 1.5 最低版本

- minSdk: 29 (Android 10)
- targetSdk: 34
- compileSdk: 34

---

## 2. 架构设计

### 2.1 架构模式

**MVVM + Clean Architecture 分层**

```
+----------------------------------+
|        Presentation Layer        |
|  Compose Screens + ViewModels    |
+----------------------------------+
|          Domain Layer            |
|  Use Cases + Repository Interf   |
+----------------------------------+
|           Data Layer             |
|  Room DB + DAOs + Entities       |
|  Local File Storage (images)     |
+----------------------------------+
```

### 2.2 设计原则

- **单向数据流**: UI -> ViewModel -> UseCase -> Repository -> DB
- **响应式**: 使用 Kotlin Flow 实现数据自动更新
- **单一职责**: 每个类只负责一件事
- **依赖倒置**: 高层模块依赖抽象接口，不依赖具体实现

---

## 3. 数据模型

### 3.1 核心实体: CollectibleEntity

表名: collectibles

字段列表:
- id: Long, PK, autoGenerate
- name: String (制品名称)
- category: String (品类)
- type: String (种类: 官方/同人/衍生)
- ipName: String (所属IP)
- seriesName: String (系列名称)
- characterTag: String (角色/CP, 逗号分隔)
- remark: String (备注)
- purchaseChannel: String (购买渠道)
- purchaseShop: String (店铺/卖家)
- purchaseDate: Long (购买时间戳)
- purchasePrice: Double (入手单价)
- purchaseQuantity: Int (购入数量)
- purchaseShipping: Double (购入运费)
- expectedPrice: Double (心理预期价)
- sellPrice: Double? (售出单价)
- sellQuantity: Int? (售出数量)
- sellShipping: Double? (售出运费)
- isFreeShipping: Boolean (是否包邮)
- sellDate: Long? (售出时间戳)
- buyerInfo: String? (买家信息)
- sellRemark: String? (售出备注)
- status: String (订单状态)
- storageStatus: String (存放状态)
- imagePaths: String (图片路径, 逗号分隔)
- createdAt: Long
- updatedAt: Long

### 3.2 状态枚举

订单状态:
- pending_tail 待补尾款
- pending_shipping_fee 待补邮
- pending_send 待发货
- in_transit 运输中
- owned 已拥有
- sold 已售出
- gift 赠品/付邮送
- lost 遗失/损坏

存放状态:
- in_stock 现货
- in_transit 在途
- group_storage 团长囤货
- agent_storage 代购处囤货

### 3.3 盈亏计算（实时计算，不存储）

计算公式:
- 总成本 = purchasePrice * purchaseQuantity + purchaseShipping
- 总营收 = sellPrice * sellQuantity + (isFreeShipping ? 0 : sellShipping)
- 盈亏金额 = 总营收 - 总成本
- 盈亏比例 = 盈亏金额 / 总成本 * 100%

领域模型 ProfitLoss:
- totalCost: Double
- totalRevenue: Double
- profitAmount: Double
- profitRate: Double

### 3.4 统计看板数据模型

DashboardSummary:
- totalInvestment: Double (总投入成本)
- totalRevenue: Double (总售出收入)
- holdingValue: Double (持仓市值)
- totalProfit: Double (累计盈亏金额)
- totalProfitRate: Double (整体盈亏比例)
- totalCount: Int (藏品总数量)
- ownedCount: Int (已拥有数量)
- soldCount: Int (已售出数量)

CategoryStat:
- categoryName: String
- count: Int
- investment: Double
- revenue: Double
- profit: Double

MonthlyStat:
- yearMonth: String (2026-08)
- expense: Double
- income: Double

---

## 4. 页面设计

### 4.1 导航结构

底部导航栏: [藏品柜] | [统计] | [我的]

### 4.2 藏品柜 Tab

顶部状态 Tab（横向滚动）:
- 全部 | 已拥有 | 待发货 | 待补邮 | 待补尾款 | 已售出

主体区域:
- 搜索栏（支持制品名、系列、角色、店铺模糊搜索）
- 筛选按钮（按种类、渠道、系列、角色、盈亏情况筛选）
- 视图切换按钮（网格/列表）
- 藏品卡片列表

藏品卡片（网格视图）:
- 封面图片（若无则显示默认图标）
- 制品名称
- 购入价格
- 状态标签（颜色标记）

藏品卡片（列表视图）:
- 缩略图
- 名称 + IP + 系列
- 价格 + 状态
- 盈亏标记

详情页:
- 顶部图片轮播（左右滑动）
- 基础信息区
- 购入信息区
- 售出信息区
- 备注区
- 底部操作栏（编辑、标记售出、删除）

新增/编辑表单页:
- 基础信息输入区
- 购入信息输入区
- 售出信息输入区（可展开）
- 图片上传区（拍照/相册选图）
- 保存按钮

### 4.3 统计 Tab

总览看板:
- 总投入成本、总售出收入
- 累计盈亏金额（大字+颜色区分盈/亏）
- 整体盈亏比例
- 持仓市值、藏品总数

分类统计:
- 按 IP / 系列 / 品类 切换
- 各分类的收支与盈亏列表
- 排行（最赚钱/最亏钱）

时间趋势:
- 按月收支柱状图
- 支出/收入双线对比

### 4.4 我的 Tab

- 设置入口
- 数据管理（备份/导出占位）
- 关于

---

## 5. 依赖清单

| 类别 | 依赖 | 用途 |
|------|------|------|
| UI | Jetpack Compose BOM 2024.02.x | 声明式 UI |
| UI | Material3 | Material Design 3 |
| UI | Activity Compose 1.8.x | Activity 集成 |
| UI | Navigation Compose 2.7.x | 页面导航 |
| DB | Room Runtime 2.6.x | ORM 数据库 |
| DB | Room KTX 2.6.x | Room 协程支持 |
| DI | Hilt Android 2.50 | 依赖注入 |
| DI | Hilt Navigation Compose 1.1.x | Hilt + Navigation |
| Image | Coil Compose 2.5.x | 图片加载 |
| Chart | Vico Compose 2.0.x | 统计图表 |
| Async | Kotlinx Coroutines 1.7.x | 协程 |
| DateTime | kotlinx-datetime 0.5.x | 日期时间 |

---

## 6. 项目结构

```
goods_collector/
  app/
    src/main/
      java/com/graincabinet/app/
        GrainCabinetApp.kt
        di/
          DatabaseModule.kt
          RepositoryModule.kt
          UseCaseModule.kt
        data/
          db/
            AppDatabase.kt
            CollectibleDao.kt
            Converters.kt
          entity/
            CollectibleEntity.kt
          repository/
            CollectibleRepositoryImpl.kt
          mapper/
            CollectibleMapper.kt
        domain/
          model/
            Collectible.kt
            ProfitLoss.kt
            DashboardSummary.kt
            CategoryStat.kt
            MonthlyStat.kt
          repository/
            CollectibleRepository.kt
          usecase/
            GetAllCollectiblesUseCase.kt
            GetCollectibleByIdUseCase.kt
            InsertCollectibleUseCase.kt
            UpdateCollectibleUseCase.kt
            DeleteCollectibleUseCase.kt
            CalculateProfitLossUseCase.kt
            GetDashboardSummaryUseCase.kt
            GetCategoryStatsUseCase.kt
            GetMonthlyStatsUseCase.kt
          calculator/
            ProfitLossCalculator.kt
        ui/
          theme/
            Color.kt
            Theme.kt
            Type.kt
          navigation/
            NavGraph.kt
            Screen.kt
          components/
            CollectibleCard.kt
            StatusChip.kt
            SearchBar.kt
            EmptyState.kt
          collectible/
            list/
              CollectibleListScreen.kt
              CollectibleListViewModel.kt
              CollectibleListUiState.kt
            detail/
              CollectibleDetailScreen.kt
              CollectibleDetailViewModel.kt
              CollectibleDetailUiState.kt
            form/
              CollectibleFormScreen.kt
              CollectibleFormViewModel.kt
              CollectibleFormUiState.kt
          statistics/
            StatisticsScreen.kt
            StatisticsViewModel.kt
            StatisticsUiState.kt
          profile/
            ProfileScreen.kt
      res/
        values/strings.xml
    build.gradle.kts
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
```

---

## 7. 状态颜色规范

| 状态 | 颜色 | 色值 |
|------|------|------|
| 待补尾款 | 橙色 | #FF9800 |
| 待补邮 | 蓝色 | #2196F3 |
| 待发货 | 紫色 | #9C27B0 |
| 运输中 | 青色 | #00BCD4 |
| 已拥有 | 绿色 | #4CAF50 |
| 已售出 | 灰色 | #9E9E9E |
| 赠品/付邮送 | 粉色 | #E91E63 |
| 遗失/损坏 | 红色 | #F44336 |

盈利 = 绿色 (#4CAF50)，亏损 = 红色 (#F44336)

---

## 8. 交互规范

### 8.1 手势
- 列表左滑：快速调出编辑、删除按钮
- 下拉刷新：刷新列表数据

### 8.2 快捷操作
- 悬浮按钮 (+)：新增藏品
- 详情页底部：一键标记售出
- 搜索栏：实时搜索反馈

### 8.3 容错机制
- 删除前二次确认弹窗
- 表单未保存离开时提示
- 图片上传失败提示重试

---

## 9. 开发里程碑

1. 脚手架搭建: Gradle 配置、Hilt、Room、导航框架
2. 数据层实现: 实体、DAO、Repository、数据库
3. 藏品列表页: 列表展示、状态 Tab、搜索筛选
4. 藏品表单页: 新增/编辑、图片上传
5. 藏品详情页: 信息展示、操作栏
6. 统计页: 看板、分类统计、趋势图
7. 我的页: 基础设置入口
8. 调试构建: 编译 APK、真机测试

---

## 10. 风险与约束

- **JDK 版本**: 需使用 JDK 17 编译（Android Studio JBR），Java 8 不够
- **APK 大小**: Compose + Hilt + Room 会增加 APK 体积，预计 10-15MB
- **图片存储**: 本地文件存储需注意 Android 分区存储（Scoped Storage）限制
- **最低版本**: Android 10 (API 29) 不支持部分最新 API，需做兼容
