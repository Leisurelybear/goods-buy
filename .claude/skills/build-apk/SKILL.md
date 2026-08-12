---
name: build-apk
description: 构建 Android Debug APK。使用 Dragonwell JDK 17 解决 Gradle 插件兼容性，输出到 app/build/outputs/apk/debug/app-debug.apk
license: MIT
---

# Build APK

为 goods_collector 项目构建 Debug APK。

## 前置条件

- 项目根目录：`G:/Coding_Project/IdeaProjects/goods_collector`
- JDK 17 路径：`C:/Users/Jason/.jdks/dragonwell-17.0.18`
- Gradle wrapper：`./gradlew`（Windows Git Bash）

## 构建命令

```bash
JAVA_HOME=/c/Users/Jason/.jdks/dragonwell-17.0.18 bash gradlew assembleDebug
```

## 输出位置

```
app/build/outputs/apk/debug/app-debug.apk
```

## 常见问题

**编译失败：`compatible with Java 8`**
原因：系统默认 JDK 8，但 AGP 8.2+ 需要 JDK 11+。
解决：用上述命令指定 JDK 17。

**Hilt DI 错误：`android.content.Context cannot be provided`**
原因：缓存失效后暴露 ViewModel 缺少 `@ApplicationContext` 注解。
解决：检查注入 `Context` 的 ViewModel，确保构造函数参数标注 `@ApplicationContext`。

**`combinedClickable` 或 `HorizontalDivider` 报错**
原因：Material 3 实验性 API 需要 opt-in。
解决：在 Composable 函数上加 `@OptIn(ExperimentalFoundationApi::class)` 或 `@OptIn(ExperimentalMaterial3Api::class)`。
