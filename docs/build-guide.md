# 构建说明（Build Guide）

本文说明 goods_collector 项目如何成功构建 Debug APK。

## 前置条件

| 项目 | 说明 |
|------|------|
| 系统 | Windows（本文以 PowerShell 为准） |
| JDK | 17（Dragonwell）`C:\Users\Jason\.jdks\dragonwell-17.0.18` |
| SDK | Android SDK `G:\AndroidSDK`（见 `local.properties`） |
| Gradle | 项目自带 wrapper（`gradlew.bat`），无需单独安装 |

> 系统默认 JDK 是 8（corretto-1.8），AGP 8.2+ 需要 JDK 11+，因此构建时必须显式指定 JAVA_HOME 为 JDK 17。

## 构建命令

在项目根目录 `G:\Coding_Project\IdeaProjects\goods_collector` 执行：

```powershell
$env:JAVA_HOME = "C:\Users\Jason\.jdks\dragonwell-17.0.18"; & .\gradlew.bat assembleDebug *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"
```

判断成功的标准：

- 输出 `EXITCODE=0`
- `build.log` 末尾出现 `BUILD SUCCESSFUL`

产物位置：

```
app\build\outputs\apk\debug\app-debug.apk
```

## 当前构建状态（2026-08-16）

**构建尚未成功**。命令本身可正常执行（JDK 17 + gradlew.bat 能启动构建），但代码存在 23 个编译错误，导致 `compileDebugKotlin` 失败。错误全部位于未提交的改动中（`git status` 里的 `M`/`??` 文件），需先修复后才能产出 APK。

### 编译错误清单

| 文件 | 错误 |
|------|------|
| `ui/components/AnimationSpecs.kt:15,21,24,27,30,33,36` | Type parameter of a property must be used in its receiver type |
| `ui/gallery/GalleryScreen.kt:155,157,158,164` | Type mismatch / Unresolved reference `id`（参数顺序颠倒） |
| `ui/gallery/GalleryScreen.kt:176,185` | Unresolved reference `onSearchChange` |
| `ui/gallery/GalleryScreen.kt:168` | 实验性 Foundation API，需要 `@OptIn` |
| `ui/gallery/GalleryScreen.kt:240` | `@Composable` 调用位置错误 |
| `ui/collectible/list/CollectibleListScreen.kt:184` | Unresolved reference `onSearchChange` |
| `ui/collectible/list/CollectibleListScreen.kt:296` | 实验性 Foundation API，需要 `@OptIn` |
| `ui/collectible/list/LongPressMenu.kt:24` | Unresolved reference `offset` |
| `ui/components/EmptyState.kt:14` | Unresolved reference `offset` |
| `ui/components/ProfitLossText.kt:15` | `State<Color>` 无法作为委托（缺 `getValue`） |
| `ui/components/StatusChip.kt:18` | `State<Color>` 无法作为委托 |
| `ui/components/StatusChip.kt:28` | `Modifier.background` 重载歧义 |

### 修复方向（概要）

1. **AnimationSpecs.kt**：属性类型参数不能脱离 receiver 使用，需改为函数或显式类型参数。
2. **GalleryScreen.kt / CollectibleListScreen.kt**：`onSearchChange` 引用未定义，需补回调参数；`Collectible` 列表与 `id` 的参数顺序/类型写反。
3. **LongPressMenu.kt / EmptyState.kt**：`offset` 引用未定义。
4. **ProfitLossText.kt / StatusChip.kt**：`remember { mutableStateOf(...) }` 的返回值需要用 `by` 解构（缺少 import `androidx.compose.runtime.getValue`）。
5. 实验性 API 处补 `@OptIn(ExperimentalFoundationApi::class)`。

修复全部编译错误后重新执行构建命令即可得到 APK。

## 常见问题

**问题：提示 `compatible with Java 8`**
原因：JAVA_HOME 未指向 JDK 17，Gradle 用了系统默认 JDK 8。
解决：构建前先设置 `$env:JAVA_HOME`。

**问题：`build.log` 里错误行被截断**
原因：`*> build.log` 重定向保留了控制台宽度换行，一行错误会被拆成两行。
解决：用上面的脚本重建（按 `^e: ` 前缀 + 拼接续行），或直接用 IDE 编译查看。

## 相关文件

- 构建命令封装：`.claude/skills/build-apk/SKILL.md`
- 版本信息：`build.gradle.kts`（AGP 8.2.2 / Kotlin 1.9.22 / Hilt 2.50）