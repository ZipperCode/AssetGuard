# UI/UX 设计方案：日夜模式切换 + 简化记账流程

**设计时间**：2026-02-27
**目标平台**：Android (Jetpack Compose + Material3)
**设计规范**：Material3 Design System，专业蓝金配色

---

## 功能 A：日夜模式切换

### A.1 设计目标

#### A.1.1 用户目标
- 根据个人偏好或使用环境（白天/夜晚）自由切换应用外观
- 设置后无需反复操作，偏好持久化保存
- "跟随系统"选项减少决策负担

#### A.1.2 业务目标
- 覆盖浅色模式用户群，扩大用户基础
- 提升品牌专业感（完整的主题系统体现产品成熟度）
- 修复当前硬编码暗色的技术债

### A.2 现状分析

#### A.2.1 主题系统现状

| 模块 | 文件 | 现状 | 需要改动 |
|------|------|------|----------|
| Theme.kt | `ui/theme/Theme.kt` | `darkTheme` 参数默认 `true`，硬编码暗色 | 需要读取用户偏好驱动 |
| Color.kt | `ui/theme/Color.kt` | 亮色/暗色配色方案已完整定义 | 无需改动 |
| ExtendedColorScheme.kt | `ui/theme/ExtendedColorScheme.kt` | Light/Dark 扩展色已完整定义 | 无需改动 |
| MainActivity.kt | `MainActivity.kt` | `AssetGuardTheme { }` 未传 `darkTheme` 参数 | 需传入用户偏好值 |
| 各 Screen | 多处硬编码 | `Color(0xFF0C0C0C)` 等暗色写死 | 需替换为语义化 token |

#### A.2.2 硬编码颜色审计（必须修复）

以下硬编码颜色在浅色模式下会导致问题，需要替换为 MaterialTheme token：

| 文件 | 硬编码 | 应替换为 |
|------|--------|----------|
| `HomeScreen.kt` L130 | `containerColor = Color(0xFF0C0C0C)` | `MaterialTheme.colorScheme.background` |
| `HomeScreen.kt` L146-148 | TopAppBar `containerColor/titleContentColor` 硬编码 | `MaterialTheme.colorScheme.background/onBackground` |
| `HomeScreen.kt` L174 | `.background(Color(0xFF0C0C0C))` | `MaterialTheme.colorScheme.background` |
| `HomeScreen.kt` L343-344 | KpiIndicator `backgroundColor = Color(0xFF1A1A1D)` | `MaterialTheme.colorScheme.surfaceVariant` |
| `HomeScreen.kt` L379 | DueSoonCard `Color(0xFF141417)` | `MaterialTheme.colorScheme.surfaceContainer` 或 `surfaceVariant` |
| `HomeScreen.kt` L319,402 | MoneyText/Text `color = Color.White` | `MaterialTheme.colorScheme.onSurface` |
| `PersonCard` L429 | `Color(0xFF141417)` | `MaterialTheme.colorScheme.surfaceVariant` |
| `AssetGuardBottomBar.kt` L18,27 | 引用 `DividerDark`/`TextMuted` 顶层常量 | 改用 `MaterialTheme.colorScheme.outlineVariant`/`onSurfaceVariant` |

### A.3 设置页面 -- 外观区块设计

#### A.3.1 区块位置

在设置页面中，"外观"属于高频使用的个性化设置，应位于**最顶部**（通知设置之前）。视觉优先级最高。

```
+---------------------------------------------+
|  TopAppBar: "设置"                           |
+---------------------------------------------+
|                                             |
|  [外观] <-- 新增区块，最高位置               |
|  +----------------------------------------+ |
|  |  [太阳] 浅色  |  [月亮] 深色  | [系统] | |
|  |            三段式选择器                  | |
|  +----------------------------------------+ |
|                                             |
|  [通知提醒]  <-- 现有区块下移               |
|  +----------------------------------------+ |
|  |  通知权限                               | |
|  |  提醒时间 / 提前天数 / 仅逾期            | |
|  |  支付方式管理                           | |
|  +----------------------------------------+ |
|                                             |
|  [数据备份]                                 |
|  ...                                        |
|  [关于]                                     |
+---------------------------------------------+
```

#### A.3.2 选项展示方式：SingleChoiceSegmentedButtonRow

**选用理由**：
- Material3 官方推荐 `SegmentedButton` 用于 2-5 个互斥选项
- 比 RadioButton 更紧凑，比 Dialog 更直观
- 一眼可见当前选择状态，无需额外点击
- 适合"浅色/深色/系统"这类三选一场景

**否决方案对比**：

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| SegmentedButton (选用) | 直观、紧凑、Material3 原生 | 选项超过 4 个时不适合 | 最佳 -- 3 个选项完美匹配 |
| RadioButton 列表 | 清晰 | 占用空间大，视觉冗余 | 不适合设置页的紧凑布局 |
| Dialog 弹窗 | 不占页面空间 | 多一次点击，不够直观 | 过于隐蔽 |
| 下拉菜单 | 紧凑 | 不直观，移动端体验差 | 不推荐 |

#### A.3.3 布局草图（外观 Card）

```
+--------------------------------------------------+
|  外观                     <-- 区块标题 primary 色  |
|                                                    |
|  +----------------------------------------------+ |
|  |  [太阳图标] 主题                              | |
|  |  根据您的偏好调整应用外观                     | |
|  |                                                | |
|  |  +----------+  +----------+  +--------------+ | |
|  |  | [太阳]   |  | [月亮]   |  | [手机+齿轮] | | |
|  |  |  浅色    |  |  深色    |  |  跟随系统    | | |
|  |  +----------+  +----------+  +--------------+ | |
|  |     (未选中)      (选中/高亮)     (未选中)     | |
|  +----------------------------------------------+ |
+--------------------------------------------------+
```

#### A.3.4 三选项详细定义

| 选项 | 值 (枚举) | 图标 | 行为 |
|------|-----------|------|------|
| 浅色 | `LIGHT` | `Icons.Outlined.LightMode` | 强制 `darkTheme = false` |
| 深色 | `DARK` | `Icons.Outlined.DarkMode` | 强制 `darkTheme = true` |
| 跟随系统 | `SYSTEM` (默认) | `Icons.Outlined.SettingsSuggest` | `darkTheme = isSystemInDarkTheme()` |

### A.4 组件拆分

#### A.4.1 组件树

```
SettingsScreen (改造)
├── ThemeSettingSection  <-- 新增
│   └── ThemeSegmentedButton  <-- 新增
├── NotificationSettingCard (现有)
├── BackupSettingCard (现有)
└── AboutCard (现有)
```

#### A.4.2 数据模型：ThemeMode 枚举

```kotlin
// 文件: data/model/ThemeMode.kt
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;  // 默认值
}
```

#### A.4.3 UserPreferences 扩展

在 `UserPreferences` data class 中新增字段：

```kotlin
data class UserPreferences(
    // ...现有字段...
    val themeMode: ThemeMode = ThemeMode.SYSTEM  // 新增
)
```

DataStore 存储键：`"theme_mode"` (存储字符串 `"LIGHT"` / `"DARK"` / `"SYSTEM"`)

#### A.4.4 核心组件：ThemeSettingSection

```kotlin
// 文件: ui/settings/ThemeSettingSection.kt (或直接内联在 SettingsScreen 中)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingSection(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
)
```

**Props 接口**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `currentThemeMode` | `ThemeMode` | 当前选中的主题模式 |
| `onThemeModeChanged` | `(ThemeMode) -> Unit` | 用户切换时的回调 |
| `modifier` | `Modifier` | 外部传入的修饰符 |

**样式要点**：
- 整体使用 `Card` 容器，`containerColor = MaterialTheme.colorScheme.surfaceVariant`
- 内部使用 `SingleChoiceSegmentedButtonRow`
- 选中项使用 Material3 默认高亮（`checkedContainerColor` 自动处理）
- 每个按钮包含图标 + 文字

**参考实现结构**：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingSection(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, Icons.Outlined.LightMode, R.string.theme_light),
        Triple(ThemeMode.DARK, Icons.Outlined.DarkMode, R.string.theme_dark),
        Triple(ThemeMode.SYSTEM, Icons.Outlined.SettingsSuggest, R.string.theme_system),
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_section_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(MaterialTheme.spacing.md))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_theme))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_theme_desc))
                    },
                    leadingContent = {
                        Icon(Icons.Outlined.Palette, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                Spacer(Modifier.height(MaterialTheme.spacing.sm))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { index, (mode, icon, labelRes) ->
                        SegmentedButton(
                            selected = currentThemeMode == mode,
                            onClick = { onThemeModeChanged(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = currentThemeMode == mode
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(
                                            SegmentedButtonDefaults.IconSize
                                        )
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }
        }
    }
}
```

### A.5 交互流程

#### A.5.1 数据流向

```
用户点击 SegmentedButton
    |
    v
SettingsViewModel.updateThemeMode(mode: ThemeMode)
    |
    v
UserPreferencesRepository.saveThemeMode(mode)  -- DataStore 持久化
    |
    v
UserPreferences Flow 发射新值
    |
    +---> SettingsScreen 重组（更新 SegmentedButton 选中态）
    |
    +---> MainActivity 观察 themeMode Flow
              |
              v
          AssetGuardTheme(darkTheme = resolvedDarkTheme)
              |
              v
          整个 Compose 树重组，颜色自动切换
```

#### A.5.2 MainActivity 改造要点

```kotlin
// MainActivity.kt 改造后
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AssetGuardApplication).container

        setContent {
            // 从 DataStore 观察主题偏好
            val themeMode by container.userPreferencesRepository
                .observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AssetGuardTheme(darkTheme = darkTheme) {
                // ...现有 Scaffold + NavGraph...
            }
        }
    }
}
```

#### A.5.3 状态转换表

| 当前状态 | 用户操作 | 下一状态 | UI 变化 |
|----------|----------|----------|---------|
| 深色 (当前默认) | 点击"浅色" | 浅色 | 整体配色切换为浅色方案，SegmentedButton 高亮移至"浅色" |
| 深色 | 点击"跟随系统" | 跟随系统 | 根据系统设置决定，SegmentedButton 高亮移至"跟随系统" |
| 浅色 | 点击"深色" | 深色 | 整体配色切换为深色方案 |
| 跟随系统 | 系统切换暗色模式 | 跟随系统(暗) | 自动跟随，无需用户操作 |
| 任意 | 杀进程重启 | 上次保存的模式 | DataStore 持久化，启动时恢复 |

#### A.5.4 切换动效建议

**方案 A（推荐 -- 简洁可靠）：原生重组**

Compose 的 `MaterialTheme` 颜色切换本身就是一次重组，不需要额外动画。Material3 的颜色 token 切换是瞬时的。这是 Google 官方应用（如 Settings、Clock）采用的方式。

- 优点：零额外代码，行为可预测，无性能开销
- 实现：不需要额外代码

**方案 B（进阶 -- 如果需要平滑过渡）：animateColorAsState**

在 `AssetGuardTheme` 中对 `colorScheme` 的每个颜色值应用 `animateColorAsState`，实现 200-300ms 的渐变过渡。

```kotlin
// 概念示例（仅在需要平滑动画时使用）
val animatedPrimary by animateColorAsState(
    targetValue = colorScheme.primary,
    animationSpec = tween(300)
)
// ...对每个颜色 token 同理
// 然后构建一个新的 colorScheme 传入 MaterialTheme
```

- 优点：视觉过渡更自然
- 缺点：需要包装所有颜色 token，代码量较大
- **建议**：第一版使用方案 A，后续迭代考虑方案 B

### A.6 新增字符串资源

```xml
<!-- 设置 - 外观 -->
<string name="settings_section_appearance">外观</string>
<string name="settings_theme">主题</string>
<string name="settings_theme_desc">根据您的偏好调整应用外观</string>
<string name="theme_light">浅色</string>
<string name="theme_dark">深色</string>
<string name="theme_system">跟随系统</string>
```

### A.7 无障碍访问

| 实践 | 实施方法 |
|------|----------|
| SegmentedButton 语义 | Material3 的 `SegmentedButton` 原生支持 `Role.RadioButton` 语义 |
| 选中状态播报 | `selected = true` 时屏幕阅读器自动播报"已选择" |
| 图标 contentDescription | 每个选项的图标设 `contentDescription = null`（由按钮文字承担） |
| 区块标题关联 | 使用 `semantics { heading() }` 标注区块标题 |

### A.8 开发交付清单 -- 功能 A

- [ ] 新增 `ThemeMode` 枚举 (`data/model/ThemeMode.kt`)
- [ ] `UserPreferences` 添加 `themeMode` 字段
- [ ] `UserPreferencesRepository` 添加 `observeThemeMode()` / `saveThemeMode()` 方法
- [ ] `SettingsViewModel` 添加 `updateThemeMode()` 方法
- [ ] `SettingsScreen` 顶部插入外观设置区块（含 `SingleChoiceSegmentedButtonRow`）
- [ ] `MainActivity` 读取 `themeMode` 驱动 `AssetGuardTheme(darkTheme = ...)`
- [ ] 修复所有硬编码颜色（见 A.2.2 审计表）
- [ ] `strings.xml` 添加外观相关字符串
- [ ] 验证浅色/深色/跟随系统三种模式在所有页面的表现

---

## 功能 B：简化记账流程

### B.1 设计目标

#### B.1.1 用户目标
- "只想快速记一笔账"的用户能在 2 步内完成操作（首页 -> 借条表单 -> 保存）
- 不需要先创建联系人再创建借条
- 对于已有联系人，能快速搜索选择
- 对于新联系人，输入名字即可自动创建

#### B.1.2 业务目标
- 将核心操作路径从 4 步 3 次页面切换缩减为 2 步 1 次页面切换
- 提升用户活跃度和留存（降低使用门槛）
- 保持现有路径兼容，不破坏已有用户习惯

### B.2 现状痛点分析

#### B.2.1 当前流程（4 步）

```
步骤 1: 首页 FAB -> PersonForm (创建联系人)
步骤 2: 返回首页 -> 点击联系人卡片 -> PersonDetail
步骤 3: PersonDetail FAB -> LoanForm (需要 personId)
步骤 4: 填写借条信息 -> 保存
```

**问题**：
- 首页 FAB 只能"新增联系人"，无法直接新增借条
- LoanForm 强依赖 `personId` 作为导航参数
- 新用户首次使用必须先理解"联系人"概念

#### B.2.2 目标流程（2 步）

```
步骤 1: 首页 FAB -> 选择"新建借条" -> LoanForm (增强版)
步骤 2: 在表单中选择/创建联系人 + 填写借条信息 -> 保存
```

### B.3 HomeScreen FAB 改造方案

#### B.3.1 方案选型：Speed Dial FAB（展开式 FAB）

**选用理由**：
- 保持单一 FAB 入口的简洁性
- 点击展开后显示 2 个子项，符合 Material3 的 Small FAB 模式
- 用户已习惯 FAB 的"创建"语义
- 不增加底部导航栏的复杂度

**否决方案**：

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Speed Dial FAB (选用) | 优雅、不占空间、可扩展 | 需自行实现展开动画 | 最佳 |
| Extended FAB + Menu | 容易实现 | Extended FAB 占空间大 | 不够优雅 |
| 底部 Sheet 菜单 | 选项多时适合 | 只有 2 个选项，过于沉重 | 过度设计 |
| 直接改为"新建借条" | 最简单 | 丢失"新建联系人"入口 | 不兼容 |

#### B.3.2 Speed Dial FAB 布局草图

```
未展开状态：
                           +-----+
                           | (+) |  <-- 主 FAB (Add 图标)
                           +-----+

展开状态（从下往上弹出）：
                   "新建借条"  +--------+
                               | [文档+] | <-- 子 FAB 1
                               +--------+

                  "新建联系人"  +--------+
                               | [人+]  | <-- 子 FAB 2
                               +--------+

                               +-----+
                               | (x) |  <-- 主 FAB 变为关闭图标
                               +-----+

半透明遮罩覆盖整个屏幕（点击遮罩关闭）
```

#### B.3.3 Speed Dial FAB 组件定义

```kotlin
// 文件: ui/components/SpeedDialFab.kt

data class SpeedDialItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
    mainIcon: ImageVector = Icons.Default.Add,
    mainCloseIcon: ImageVector = Icons.Default.Close,
    mainContentDescription: String? = null
)
```

**状态管理**：
- `isExpanded: Boolean` -- 展开/收起状态（内部管理）

**动画规格**：
- 子 FAB 弹出：`spring(dampingRatio = 0.7f, stiffness = 300f)`
- 子 FAB 入场间隔：50ms stagger
- 主 FAB 图标旋转：`animateFloatAsState` 0 -> 45 度
- 遮罩层：`animateFloatAsState` alpha 0 -> 0.32f

**交互细节**：
- 点击主 FAB：展开/收起子项
- 点击子项：执行对应操作 + 自动收起
- 点击遮罩层：收起
- 按返回键：收起（如果已展开）

**参考实现结构**：

```kotlin
@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
    mainIcon: ImageVector = Icons.Default.Add,
    mainCloseIcon: ImageVector = Icons.Default.Close,
    mainContentDescription: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(200)
    )

    // 遮罩层（expanded 时全屏半透明）
    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = false }
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
    ) {
        // 子 FAB 列表（反转以从下往上排列）
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 标签
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = item.label,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        // 小 FAB
                        SmallFloatingActionButton(
                            onClick = {
                                expanded = false
                                item.onClick()
                            }
                        ) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    }
                }
            }
        }

        // 主 FAB
        FloatingActionButton(
            onClick = { expanded = !expanded }
        ) {
            Icon(
                imageVector = if (expanded) mainCloseIcon else mainIcon,
                contentDescription = mainContentDescription,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}
```

#### B.3.4 HomeScreen FAB 改造后的使用方式

```kotlin
// HomeScreen.kt 中 floatingActionButton 改造
floatingActionButton = {
    if (!uiState.isSelectionMode) {
        SpeedDialFab(
            items = listOf(
                SpeedDialItem(
                    icon = Icons.Default.NoteAdd,  // 或 PostAdd
                    label = stringResource(R.string.home_add_loan),
                    onClick = onAddLoan  // 新增回调
                ),
                SpeedDialItem(
                    icon = Icons.Default.PersonAdd,
                    label = stringResource(R.string.home_add_person),
                    onClick = onAddPerson
                ),
            )
        )
    }
}
```

**HomeScreen 签名变更**：

```kotlin
@Composable
fun HomeScreen(
    container: AppContainer,
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onAddLoan: () -> Unit,     // 新增
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
)
```

### B.4 LoanFormScreen 改造方案 -- 联系人选择/创建

#### B.4.1 路由改造

当前路由 `loan_form/{personId}?loanId={loanId}` 中 `personId` 为必填路径参数。

改造为 `personId` 可选：

```kotlin
// Screen.kt 改造
data object LoanForm : Screen("loan_form?personId={personId}&loanId={loanId}") {
    fun createRoute(personId: Long? = null, loanId: Long? = null): String {
        val params = mutableListOf<String>()
        personId?.let { params.add("personId=$it") }
        loanId?.let { params.add("loanId=$it") }
        return if (params.isEmpty()) "loan_form"
        else "loan_form?${params.joinToString("&")}"
    }
}
```

导航注册改造：

```kotlin
composable(
    route = Screen.LoanForm.route,
    arguments = listOf(
        navArgument("personId") { type = NavType.LongType; defaultValue = -1L },
        navArgument("loanId") { type = NavType.LongType; defaultValue = -1L }
    )
) { backStackEntry ->
    val personId = backStackEntry.arguments?.getLong("personId")?.takeIf { it != -1L }
    val loanId = backStackEntry.arguments?.getLong("loanId")?.takeIf { it != -1L }
    LoanFormScreen(
        personId = personId,  // 现在为 nullable
        loanId = loanId,
        container = container,
        onBack = { navController.popBackStack() }
    )
}
```

#### B.4.2 LoanFormScreen 增强 -- 联系人选择区块

**两种入口场景**：

| 场景 | personId | 表现 |
|------|----------|------|
| 从 PersonDetail 进入 | 非 null | 联系人已锁定，显示为只读信息条，不可更改 |
| 从首页 FAB 进入 | null | 显示联系人搜索/创建区块 |

#### B.4.3 增强后的表单布局草图

```
从首页进入 (personId = null):
+---------------------------------------------+
|  TopAppBar: "新建借条"    [<- 返回]          |
+---------------------------------------------+
|                                              |
|  +----------------------------------------+ |
|  | 联系人 *               <-- 新增区块     | |
|  | +------------------------------------+ | |
|  | | [搜索图标] 搜索联系人或输入新名称... | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 下拉搜索结果：                         | |
|  | +------------------------------------+ | |
|  | | [头像] 张三  138****1234            | | |
|  | +------------------------------------+ | |
|  | | [头像] 张小明                       | | |
|  | +------------------------------------+ | |
|  | | [+] 新建联系人 "张"                 | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 金额（元）*                            | |
|  | +------------------------------------+ | |
|  | | [Y] 0.00                           | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 借款日期                               | |
|  | +------------------------------------+ | |
|  | | 2026-02-27                         | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 到期日（可选）   [清除]                | |
|  | +------------------------------------+ | |
|  | | 未设置                              | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 支付方式 *                             | |
|  | +------------------------------------+ | |
|  | | 微信                          [v]  | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 备注                                   | |
|  | +------------------------------------+ | |
|  | |                                    | | |
|  | +------------------------------------+ | |
|  +----------------------------------------+ |
|                                              |
|  [============= 保存 =================]     |
+---------------------------------------------+

从 PersonDetail 进入 (personId = 123):
+---------------------------------------------+
|  TopAppBar: "新建借条"    [<- 返回]          |
+---------------------------------------------+
|                                              |
|  +----------------------------------------+ |
|  | 联系人                                  | |
|  | +------------------------------------+ | |
|  | | [头像] 张三  138****1234  [锁定]    | | |
|  | +------------------------------------+ | |
|  |                                        | |
|  | 金额（元）*                            | |
|  | ...（后续字段与现有相同）              | |
|  +----------------------------------------+ |
|                                              |
+---------------------------------------------+
```

#### B.4.4 联系人搜索/创建组件：PersonSelector

```kotlin
// 文件: ui/components/PersonSelector.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSelector(
    // 当前选中的联系人（已有联系人）
    selectedPerson: PersonEntity?,
    // 输入的新联系人名称（当选择"新建"时）
    newPersonName: String,
    // 搜索输入文本
    query: String,
    // 搜索结果
    suggestions: List<PersonEntity>,
    // 是否锁定（从 PersonDetail 进入时为 true）
    isLocked: Boolean,
    // 验证错误
    error: Int?,  // string resource id
    // 回调
    onQueryChanged: (String) -> Unit,
    onPersonSelected: (PersonEntity) -> Unit,
    onNewPersonSelected: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
)
```

**交互流程详细说明**：

1. **初始状态**（personId = null）：
   - 显示一个 `OutlinedTextField`，placeholder: "搜索联系人或输入新名称..."
   - 输入框前方有搜索图标

2. **用户输入搜索文字**：
   - 每次输入变化触发 `onQueryChanged`
   - ViewModel 根据输入文字查询 `PersonRepository.searchByName(query)`
   - 匹配结果显示在输入框下方的下拉列表中

3. **下拉列表内容**：
   - 匹配的已有联系人列表（头像 + 名称 + 电话）
   - 列表末尾始终显示一个"新建联系人 '{输入文字}'"的选项（带 `+` 图标）
   - 无匹配结果时只显示"新建"选项

4. **选择已有联系人**：
   - 点击后 `onPersonSelected(person)` 被调用
   - 输入框变为只读，显示选中联系人信息（头像 + 名称）
   - 右侧显示清除按钮 (X)，可重新选择

5. **选择"新建联系人"**：
   - 点击后 `onNewPersonSelected(query)` 被调用
   - 输入框变为只读，显示 "[+] {名称}（新建）"
   - 右侧显示清除按钮
   - 保存借条时自动创建 PersonEntity

6. **锁定模式**（personId 非 null）：
   - 显示为只读信息条（不可编辑）
   - 展示已关联联系人的头像、名称、电话
   - 右侧显示锁定图标
   - 无清除按钮

**参考实现结构**：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSelector(
    selectedPerson: PersonEntity?,
    newPersonName: String,
    query: String,
    suggestions: List<PersonEntity>,
    isLocked: Boolean,
    error: Int?,
    onQueryChanged: (String) -> Unit,
    onPersonSelected: (PersonEntity) -> Unit,
    onNewPersonSelected: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedPerson != null || newPersonName.isNotBlank()

    Column(modifier = modifier) {
        if (isLocked && selectedPerson != null) {
            // 锁定模式 -- 只读显示
            LockedPersonChip(person = selectedPerson)
        } else if (hasSelection) {
            // 已选择 -- 显示选中状态 + 清除按钮
            SelectedPersonChip(
                person = selectedPerson,
                newName = newPersonName,
                onClear = onClear
            )
        } else {
            // 搜索模式 -- 输入框 + 下拉
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded && query.isNotBlank(),
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        onQueryChanged(it)
                        expanded = it.isNotBlank()
                    },
                    label = { Text(stringResource(R.string.loan_form_label_person)) },
                    placeholder = {
                        Text(stringResource(R.string.loan_form_person_hint))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    isError = error != null,
                    supportingText = error?.let { resId ->
                        { Text(stringResource(resId)) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    singleLine = true,
                    colors = inputColors,
                )

                ExposedDropdownMenu(
                    expanded = expanded && query.isNotBlank(),
                    onDismissRequest = { expanded = false }
                ) {
                    // 已有联系人匹配项
                    suggestions.forEach { person ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarView(name = person.name, size = 32.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(person.name)
                                        person.phone?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onPersonSelected(person)
                                expanded = false
                            }
                        )
                    }

                    // "新建联系人" 选项（始终显示）
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(
                                        R.string.loan_form_create_person,
                                        query
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            onNewPersonSelected(query)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPersonChip(
    person: PersonEntity?,
    newName: String,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (person != null) {
                AvatarView(name = person.name, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(person.name, style = MaterialTheme.typography.bodyLarge)
                    person.phone?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(newName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.loan_form_new_person_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
            }
        }
    }
}

@Composable
private fun LockedPersonChip(person: PersonEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(name = person.name, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.bodyLarge)
                person.phone?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### B.5 LoanFormViewModel 改造

#### B.5.1 UiState 扩展

```kotlin
data class LoanFormUiState(
    // ...现有字段...
    val amountText: String = "",
    val loanDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val paymentMethodId: Long? = null,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val amountError: Int? = null,
    val dateError: Int? = null,
    val paymentMethodError: Int? = null,
    // === 新增字段 ===
    val isPersonLocked: Boolean = false,          // 从 PersonDetail 进入时为 true
    val selectedPerson: PersonEntity? = null,     // 选中的已有联系人
    val newPersonName: String = "",               // 输入的新联系人名称
    val personQuery: String = "",                 // 搜索输入
    val personSuggestions: List<PersonEntity> = emptyList(),  // 搜索结果
    val personError: Int? = null,                 // 联系人选择验证错误
)
```

#### B.5.2 ViewModel 签名变化

```kotlin
class LoanFormViewModel(
    private val loanRepository: LoanRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val personRepository: PersonRepository,    // 新增
    private val personId: Long?                        // 改为 nullable
) : ViewModel() {

    init {
        if (personId != null) {
            // 从 PersonDetail 进入 -- 加载联系人信息并锁定
            viewModelScope.launch {
                personRepository.getById(personId)?.let { person ->
                    _uiState.value = _uiState.value.copy(
                        selectedPerson = person,
                        isPersonLocked = true
                    )
                }
            }
        }
        // ...现有的 loanId 加载逻辑...
    }

    // === 新增方法 ===

    fun onPersonQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            personQuery = query,
            personError = null
        )
        // 搜索联系人
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce 300ms
            val results = personRepository.searchByName(query)
            _uiState.value = _uiState.value.copy(personSuggestions = results)
        }
    }

    fun onPersonSelected(person: PersonEntity) {
        _uiState.value = _uiState.value.copy(
            selectedPerson = person,
            newPersonName = "",
            personQuery = "",
            personSuggestions = emptyList(),
            personError = null
        )
    }

    fun onNewPersonSelected(name: String) {
        _uiState.value = _uiState.value.copy(
            selectedPerson = null,
            newPersonName = name.trim(),
            personQuery = "",
            personSuggestions = emptyList(),
            personError = null
        )
    }

    fun clearPersonSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPerson = null,
            newPersonName = "",
            personQuery = "",
            personSuggestions = emptyList()
        )
    }

    // === save() 改造 ===

    fun save() {
        val state = _uiState.value

        // 验证联系人
        val resolvedPersonId: Long? = when {
            state.isPersonLocked -> state.selectedPerson?.id
            state.selectedPerson != null -> state.selectedPerson.id
            state.newPersonName.isNotBlank() -> null  // 需要先创建
            else -> {
                _uiState.value = state.copy(
                    personError = R.string.error_select_person
                )
                return
            }
        }

        // ...现有金额/日期/支付方式验证...

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            // 如果是新建联系人，先创建
            val finalPersonId = resolvedPersonId ?: run {
                personRepository.insert(
                    PersonEntity(name = state.newPersonName)
                )
            }

            // 创建/更新借条（现有逻辑，personId 换为 finalPersonId）
            if (loanId != null) {
                // ...现有更新逻辑...
            } else {
                loanRepository.insert(
                    LoanEntity(
                        personId = finalPersonId,
                        amount = amountCents,
                        loanDate = state.loanDate,
                        dueDate = state.dueDate,
                        paymentMethodId = paymentMethodId,
                        note = state.note.trim().ifBlank { null }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }
}
```

### B.6 整体交互流程图

#### B.6.1 用户旅程图（Mermaid）

```mermaid
graph TD
    Start[用户打开首页] --> FAB{点击 FAB}

    FAB --> Expand[FAB 展开<br/>显示两个选项]

    Expand --> AddLoan[点击"新建借条"]
    Expand --> AddPerson[点击"新建联系人"]

    AddPerson --> PersonForm[进入 PersonForm]
    PersonForm --> BackHome[返回首页]

    AddLoan --> LoanFormNew[进入 LoanForm<br/>personId = null]

    LoanFormNew --> SearchPerson[联系人输入框<br/>搜索或输入名称]

    SearchPerson --> TypeQuery{用户输入}

    TypeQuery --> HasMatch[有匹配联系人]
    TypeQuery --> NoMatch[无匹配联系人]

    HasMatch --> SelectExisting[选择已有联系人]
    HasMatch --> CreateNew[选择"新建联系人 'xxx'"]
    NoMatch --> CreateNew

    SelectExisting --> FillForm[填写金额/日期/支付方式]
    CreateNew --> FillForm

    FillForm --> Submit[点击保存]

    Submit --> Validate{验证通过?}

    Validate -->|失败| ShowError[显示错误提示]
    ShowError --> FillForm

    Validate -->|成功| Saving[显示保存中...]

    Saving --> CreatePersonIfNeeded{需要创建联系人?}

    CreatePersonIfNeeded -->|是| AutoCreate[自动创建 PersonEntity]
    AutoCreate --> SaveLoan[保存 LoanEntity]

    CreatePersonIfNeeded -->|否| SaveLoan

    SaveLoan --> Success[保存成功<br/>返回上一页]

    %% 兼容旧路径
    Start --> ClickPerson[点击联系人卡片]
    ClickPerson --> PersonDetail[PersonDetail 页面]
    PersonDetail --> DetailFAB[点击 FAB "新建借条"]
    DetailFAB --> LoanFormLocked[进入 LoanForm<br/>personId = 已知<br/>联系人锁定]
    LoanFormLocked --> FillForm
```

#### B.6.2 两种入口对比

| 维度 | 路径 A：首页快速创建（新） | 路径 B：PersonDetail 创建（保留） |
|------|--------------------------|----------------------------------|
| 入口 | 首页 FAB -> "新建借条" | PersonDetail FAB -> "添加借条" |
| personId | null（需选择/创建） | 非 null（已锁定） |
| 联系人区块 | 搜索+自动补全+新建 | 只读锁定显示 |
| 步骤数 | 2 步（首页 -> 表单 -> 保存） | 3 步（首页 -> 详情 -> 表单 -> 保存） |
| 适用场景 | "快速记一笔账" | "给某人记一笔新的" |

### B.7 AppNavGraph 改造

```kotlin
// AppNavGraph.kt 中 HomeScreen 路由改造
composable(Screen.Home.route) {
    HomeScreen(
        container = container,
        onPersonClick = { personId ->
            navController.navigate(Screen.PersonDetail.createRoute(personId))
        },
        onAddPerson = {
            navController.navigate(Screen.PersonForm.createRoute())
        },
        onAddLoan = {
            // 新增：不传 personId，进入联系人选择模式
            navController.navigate(Screen.LoanForm.createRoute())
        },
    )
}
```

### B.8 新增字符串资源

```xml
<!-- 首页 FAB -->
<string name="home_add_loan">新建借条</string>

<!-- 借条表单 - 联系人选择 -->
<string name="loan_form_label_person">联系人 *</string>
<string name="loan_form_person_hint">搜索联系人或输入新名称...</string>
<string name="loan_form_create_person">新建联系人「%s」</string>
<string name="loan_form_new_person_tag">将自动创建</string>
<string name="error_select_person">请选择或输入联系人</string>
```

### B.9 无障碍访问

| 组件 | A11y 实践 |
|------|-----------|
| SpeedDialFab | 主 FAB `contentDescription = "创建"`；展开后子项各有独立 label |
| Speed Dial 遮罩 | 遮罩层不获取焦点，TalkBack 用户通过返回键关闭 |
| PersonSelector 搜索 | `OutlinedTextField` 带 label，搜索结果使用 `ExposedDropdownMenu` 原生 A11y |
| 锁定状态 | 锁定图标 `contentDescription = "联系人已锁定"` |
| "新建联系人"选项 | `DropdownMenuItem` 的 text 包含完整描述 |

### B.10 开发交付清单 -- 功能 B

- [ ] 新增 `SpeedDialFab` 组件 (`ui/components/SpeedDialFab.kt`)
- [ ] `HomeScreen` 添加 `onAddLoan` 回调，FAB 替换为 SpeedDialFab
- [ ] `Screen.LoanForm` 路由改造（`personId` 变为可选参数）
- [ ] `AppNavGraph` 更新 LoanForm 路由注册 + HomeScreen 新增 `onAddLoan` 导航
- [ ] 新增 `PersonSelector` 组件 (`ui/components/PersonSelector.kt`)
- [ ] `LoanFormUiState` 添加联系人相关字段
- [ ] `LoanFormViewModel` 添加联系人搜索/选择/创建逻辑
- [ ] `LoanFormViewModel.save()` 改造：支持自动创建联系人
- [ ] `LoanFormScreen` 改造：根据 `personId` 是否为 null 显示不同的联系人区块
- [ ] `PersonRepository` 添加 `searchByName(query: String)` 方法（如果不存在）
- [ ] `strings.xml` 添加相关字符串资源
- [ ] 验证两种入口路径均正常工作（首页快速创建 + PersonDetail 创建）

---

## 实施优先级建议

| 优先级 | 任务 | 工作量 | 依赖 |
|--------|------|--------|------|
| P0 | 功能 A - 硬编码颜色修复 | 小 | 无 |
| P0 | 功能 A - ThemeMode 枚举 + DataStore 存储 | 小 | 无 |
| P0 | 功能 A - MainActivity 读取主题偏好 | 小 | ThemeMode + DataStore |
| P1 | 功能 A - 设置页面外观区块 UI | 中 | ThemeMode + SettingsViewModel |
| P1 | 功能 B - SpeedDialFab 组件 | 中 | 无 |
| P1 | 功能 B - HomeScreen FAB 改造 | 小 | SpeedDialFab |
| P2 | 功能 B - Screen.LoanForm 路由改造 | 小 | 无 |
| P2 | 功能 B - PersonSelector 组件 | 中 | PersonRepository.searchByName |
| P2 | 功能 B - LoanFormViewModel 改造 | 中 | PersonSelector |
| P2 | 功能 B - LoanFormScreen 改造 | 中 | PersonSelector + ViewModel |
| P3 | 两个功能全页面回归测试 | 中 | 全部完成 |

---

## 涉及文件索引

### 功能 A 涉及文件

| 文件（绝对路径） | 操作 |
|------------------|------|
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/theme/Theme.kt` | 无需改动（参数化已就绪） |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/MainActivity.kt` | 读取 themeMode 驱动主题 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/settings/SettingsScreen.kt` | 插入外观设置区块 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/home/HomeScreen.kt` | 替换硬编码颜色 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/components/AssetGuardBottomBar.kt` | 替换硬编码颜色引用 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/res/values/strings.xml` | 新增外观相关字符串 |
| 新建: `data/model/ThemeMode.kt` | ThemeMode 枚举 |

### 功能 B 涉及文件

| 文件（绝对路径） | 操作 |
|------------------|------|
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/home/HomeScreen.kt` | FAB 替换为 SpeedDialFab |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/navigation/Screen.kt` | LoanForm 路由 personId 改为可选 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/navigation/AppNavGraph.kt` | 更新路由注册 + HomeScreen 回调 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/loan/LoanFormScreen.kt` | 添加 PersonSelector 区块 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/java/com/zipper/compose/assetguard/ui/loan/LoanFormViewModel.kt` | 联系人搜索/选择/创建逻辑 |
| `/Users/Zipper/Github/AssetGuard/app/src/main/res/values/strings.xml` | 新增借条表单相关字符串 |
| 新建: `ui/components/SpeedDialFab.kt` | Speed Dial FAB 组件 |
| 新建: `ui/components/PersonSelector.kt` | 联系人选择器组件 |
