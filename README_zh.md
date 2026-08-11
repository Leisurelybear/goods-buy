# 谷的拜 (GoodsBuy)

面向二次元「吃谷」群体的周边全生命周期管理工具，一站式记录买入/卖出、自动统计盈亏。

## 功能（MVP）

- **藏品管理** — 录入、编辑、分类管理；支持订单状态（持有 / 已售 / 待出）与图片附件
- **盈亏统计** — 自动计算累计、月度、分类维度的收支与利润；可视化图表展示
- **数据看板** — 总投资、总营收、持仓价值、盈亏率一目了然

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

- minSdk: 29 (Android 10)
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

## 许可证

MIT
