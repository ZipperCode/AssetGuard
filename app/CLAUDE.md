[根目录](../CLAUDE.md) > **app**

# app 模块文档

---

## 模块职责

`app` 是 AssetGuard 的唯一 Android 应用模块，承载全部应用功能。当前处于初始搭建阶段，仅包含脚手架代码（Scaffold + 占位 Greeting 组件）。

| 属性 | 值 |
|------|-----|
| **模块类型** | Android Application |
| **包名** | `com.zipper.compose.assetguard` |
| **Application ID** | `com.zipper.compose.assetguard` |
| **compileSdk** | 36 |
| **minSdk** | 24（Android 7.0） |
| **targetSdk** | 36 |
| **versionCode** | 1 |
| **versionName** | 1.0 |

---

## 入口与启动

### MainActivity

- **文件**: `src/main/java/com/zipper/compose/assetguard/MainActivity.kt`
- **架构**: 单 Activity，通过 `setContent` 托管全部 Compose UI
- **特性**: 启用 Edge-to-Edge 显示（`enableEdgeToEdge()`）
- **当前内容**: `AssetGuardTheme` 包裹 `Scaffold`，内含占位 `Greeting` 组件

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AssetGuardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(name = "Android", modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
```

### AndroidManifest.xml

- **文件**: `src/main/AndroidManifest.xml`
- **启动 Activity**: `MainActivity`（`android.intent.action.MAIN` + `LAUNCHER`）
- **主题**: `Theme.AssetGuard`（Material Light NoActionBar）
- **备份**: 已配置 `backup_rules.xml` 和 `data_extraction_rules.xml`
- **权限**: 当前未声明任何权限

---

## 对外接口

当前模块处于初始阶段，尚未定义对外接口（无 API、无 Content Provider、无 AIDL）。

---

## 关键依赖与配置

### 构建插件

| 插件 | 用途 |
|------|------|
| `com.android.application` | Android 应用构建 |
| `org.jetbrains.kotlin.android` | Kotlin Android 支持 |
| `org.jetbrains.kotlin.plugin.compose` | Compose 编译器插件 |

### 运行时依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `androidx.core:core-ktx` | 1.17.0 | Android 核心 Kotlin 扩展 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.10.0 | Lifecycle 感知组件 |
| `androidx.activity:activity-compose` | 1.12.2 | Activity Compose 集成 |
| `androidx.compose:compose-bom` | 2024.09.00 | Compose 版本 BOM |
| `androidx.compose.ui:ui` | BOM 管理 | Compose UI 核心 |
| `androidx.compose.ui:ui-graphics` | BOM 管理 | Compose 图形 |
| `androidx.compose.ui:ui-tooling-preview` | BOM 管理 | Compose 预览支持 |
| `androidx.compose.material3:material3` | BOM 管理 | Material3 组件库 |

### 测试依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `junit:junit` | 4.13.2 | 单元测试框架 |
| `androidx.test.ext:junit` | 1.3.0 | AndroidX JUnit 扩展 |
| `androidx.test.espresso:espresso-core` | 3.7.0 | UI 自动化测试 |
| `androidx.compose.ui:ui-test-junit4` | BOM 管理 | Compose UI 测试 |

### Debug 依赖

| 依赖 | 用途 |
|------|------|
| `androidx.compose.ui:ui-tooling` | Compose 布局检查器 |
| `androidx.compose.ui:ui-test-manifest` | 测试 Manifest |

### 构建选项

- **Compose**: 已启用
- **Java 兼容**: Java 11（source + target）
- **JVM Target**: 11
- **ProGuard**: Release 构建未启用混淆

---

## UI 主题系统

### Theme.kt

- **文件**: `src/main/java/com/zipper/compose/assetguard/ui/theme/Theme.kt`
- **功能**: Material3 主题配置，支持亮色/暗色模式，支持 Android 12+ 动态配色
- **API**: `AssetGuardTheme(darkTheme, dynamicColor, content)`

### Color.kt

- **文件**: `src/main/java/com/zipper/compose/assetguard/ui/theme/Color.kt`
- **定义**:
  - 亮色方案: `Purple40`、`PurpleGrey40`、`Pink40`
  - 暗色方案: `Purple80`、`PurpleGrey80`、`Pink80`

### Type.kt

- **文件**: `src/main/java/com/zipper/compose/assetguard/ui/theme/Type.kt`
- **定义**: Material3 排版系统，当前仅自定义 `bodyLarge`

---

## 数据模型

当前模块尚未实现数据层。无 Room 数据库、无 ORM 模型、无 Schema 定义。

**规划中的数据层架构**:
```
数据层 (Data Layer)
├── Room Database
├── Repository Implementations
└── Data Sources (Local / Backup)
```

---

## 资源文件

| 目录 | 内容 |
|------|------|
| `res/drawable/` | 应用图标前景/背景（XML 矢量） |
| `res/mipmap-*/` | 各密度启动图标（WebP 格式） |
| `res/mipmap-anydpi-v26/` | 自适应图标定义（Android 8.0+） |
| `res/values/colors.xml` | XML 颜色资源（传统 View 系统用） |
| `res/values/strings.xml` | 字符串资源（`app_name = "AssetGuard"`） |
| `res/values/themes.xml` | XML 主题（`Theme.AssetGuard`，Material Light NoActionBar） |
| `res/xml/backup_rules.xml` | Android 备份规则 |
| `res/xml/data_extraction_rules.xml` | Android 12+ 数据提取规则 |

---

## 测试与质量

### 单元测试

- **位置**: `src/test/java/com/zipper/compose/assetguard/`
- **文件**: `ExampleUnitTest.kt`
- **框架**: JUnit 4
- **状态**: 仅占位测试（`addition_isCorrect`）

### Instrumented 测试

- **位置**: `src/androidTest/java/com/zipper/compose/assetguard/`
- **文件**: `ExampleInstrumentedTest.kt`
- **框架**: AndroidX Test Runner + JUnit4
- **Runner**: `androidx.test.runner.AndroidJUnitRunner`
- **状态**: 仅占位测试（包名验证）

### 代码质量工具

当前未配置 Lint 自定义规则、Detekt、ktlint 等静态分析工具。

---

## 常见问题 (FAQ)

**Q: 项目使用哪个 Compose 版本？**
A: 通过 BOM 2024.09.00 统一管理 Compose 组件版本，无需单独指定。

**Q: 为什么 compileSdk 使用 `release(36)` 而非直接写数字？**
A: 这是 AGP 8.13+ 的新 API，`release(36)` 表示使用 Android API 36 的 release 版本。

**Q: 如何添加新的 Compose 屏幕？**
A: 在 `com.zipper.compose.assetguard.ui` 包下创建新的 Composable 函数，并通过导航框架（尚未引入）进行路由。

---

## 相关文件清单

### 核心代码

| 文件 | 行数 | 用途 |
|------|------|------|
| `src/main/java/.../MainActivity.kt` | 47 | 应用入口 Activity |
| `src/main/java/.../ui/theme/Theme.kt` | 58 | Material3 主题 |
| `src/main/java/.../ui/theme/Color.kt` | 11 | 颜色定义 |
| `src/main/java/.../ui/theme/Type.kt` | 34 | 排版定义 |

### 配置文件

| 文件 | 用途 |
|------|------|
| `build.gradle.kts` | 模块构建配置 |
| `proguard-rules.pro` | ProGuard 规则（当前为空） |
| `src/main/AndroidManifest.xml` | 应用清单 |

### 测试文件

| 文件 | 用途 |
|------|------|
| `src/test/.../ExampleUnitTest.kt` | 单元测试占位 |
| `src/androidTest/.../ExampleInstrumentedTest.kt` | Instrumented 测试占位 |

---

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-11 15:04:21 | 更新文档 | 根据架构扫描结果重新生成模块文档，补充依赖版本、资源清单、FAQ |
| 2026-02-01 | 初始创建 | 首次创建 app 模块文档 |
