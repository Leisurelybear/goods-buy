# 构建说明（Build Guide）

本文说明 goods_collector 项目如何成功构建 Debug APK。

## 前置条件

| 项目 | 说明 |
|------|------|
| 系统 | Windows（本文以 PowerShell 为准） |
| JDK | 17（Dragonwell 或其他发行版均可） |
| SDK | Android SDK（见 `local.properties`） |
| Gradle | 项目自带 wrapper（`gradlew.bat`），无需单独安装 |

> 系统默认 JDK 可能是 8，AGP 8.2+ 需要 JDK 11+，因此构建时必须显式指定 JAVA_HOME 为 JDK 17。

## 构建命令

在项目根目录执行：

```powershell
$env:JAVA_HOME = "<你的 JDK 17 路径>"; & .\gradlew.bat assembleDebug *> build.log; Write-Output "EXITCODE=$LASTEXITCODE"
```

判断成功的标准：

- 输出 `EXITCODE=0`
- `build.log` 末尾出现 `BUILD SUCCESSFUL`

产物位置：

```
app\build\outputs\apk\debug\app-debug.apk
```

## 常见问题

**问题：提示 `compatible with Java 8`**
原因：JAVA_HOME 未指向 JDK 17，Gradle 用了系统默认 JDK 8。
解决：构建前先设置 `$env:JAVA_HOME`。

**问题：`build.log` 里错误行被截断**
原因：`*> build.log` 重定向保留了控制台宽度换行，一行错误会被拆成两行。
解决：用上面的脚本重建，或直接用 IDE 编译查看。

## 相关文件

- 构建命令封装：`.claude/skills/build-apk/SKILL.md`
- 版本信息：`build.gradle.kts`（AGP 8.2.2 / Kotlin 1.9.22 / Hilt 2.50）