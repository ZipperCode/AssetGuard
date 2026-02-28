package com.zipper.compose.assetguard.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.backup.BackupManager
import com.zipper.compose.assetguard.data.backup.DatabaseBackupManager
import com.zipper.compose.assetguard.data.model.ThemeMode
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.PermissionRationaleDialog
import com.zipper.compose.assetguard.ui.theme.spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onPaymentMethodManage: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val integrityReport by viewModel.integrityReport.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(container) }
    val dbBackupManager = remember { DatabaseBackupManager(context, container.getDatabase()) }

    var showRationaleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // 通知权限请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationPermissionAsked()
        if (!granted) {
            showRationaleDialog = true
        }
    }

    // JSON 导出
    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = backupManager.exportJson(context, it)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) context.getString(R.string.settings_json_export_success) else context.getString(R.string.settings_export_failed, result.exceptionOrNull()?.message ?: "")
                )
            }
        }
    }

    // JSON 导入（三步流程：选择文件 → 解析预览）
    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.parseBackup(context, it)
        }
    }

    // SQLite 导出
    val dbExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = dbBackupManager.exportDatabase(context, it)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) context.getString(R.string.settings_db_export_success) else context.getString(R.string.settings_export_failed, result.exceptionOrNull()?.message ?: "")
                )
            }
        }
    }

    // SQLite 导入
    val dbImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = dbBackupManager.importDatabase(context, it)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) context.getString(R.string.settings_db_import_success) else context.getString(R.string.settings_import_failed, result.exceptionOrNull()?.message ?: "")
                )
            }
        }
    }

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
    val listItemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            // ── 外观设置 Card ──
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(MaterialTheme.spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_theme),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.settings_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.md))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            Triple(ThemeMode.LIGHT, R.string.theme_light, Icons.Default.LightMode),
                            Triple(ThemeMode.DARK, R.string.theme_dark, Icons.Default.DarkMode),
                            Triple(ThemeMode.SYSTEM, R.string.theme_system, Icons.Default.SettingsSuggest),
                        )
                        options.forEachIndexed { index, (mode, labelRes, icon) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { viewModel.updateThemeMode(mode) },
                                selected = prefs.themeMode == mode,
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = prefs.themeMode == mode) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.height(SegmentedButtonDefaults.IconSize)
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

            // ── 通知设置 Card ──
            Text(
                text = stringResource(R.string.settings_section_notification),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                Column {
                    // 通知权限
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_notification_perm)) },
                        supportingContent = { Text(stringResource(if (prefs.notificationPermissionAsked) R.string.settings_perm_asked else R.string.settings_perm_grant)) },
                        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 提醒时间设置
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                        Text(stringResource(R.string.settings_reminder_time), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(R.string.settings_daily_reminder, String.format("%02d:%02d", prefs.reminderHour, prefs.reminderMinute)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.md))

                        Text(stringResource(R.string.settings_advance_days, prefs.reminderAdvanceDays), style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = prefs.reminderAdvanceDays.toFloat(),
                            onValueChange = { viewModel.updateReminderAdvanceDays(it.toInt()) },
                            valueRange = 0f..7f,
                            steps = 6
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.settings_only_overdue),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = prefs.onlyOverdue,
                                onCheckedChange = viewModel::updateOnlyOverdue
                            )
                        }

                        Spacer(Modifier.height(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(R.string.settings_silent_hours, prefs.silentStartHour, prefs.silentEndHour),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 支付方式管理
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_payment_method)) },
                        supportingContent = { Text(stringResource(R.string.settings_payment_method_desc)) },
                        leadingContent = { Icon(Icons.Default.Payment, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable(onClick = onPaymentMethodManage)
                    )
                }
            }

            // ── 数据管理 Card ──
            Text(
                text = stringResource(R.string.settings_section_backup),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                Column {
                    // JSON 导出
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_export_json)) },
                        supportingContent = { Text(stringResource(R.string.settings_export_json_desc)) },
                        leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable {
                            jsonExportLauncher.launch("assetguard_backup.json")
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // JSON 导入
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_import_json)) },
                        supportingContent = { Text(stringResource(R.string.settings_import_json_desc)) },
                        leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable {
                            jsonImportLauncher.launch(arrayOf("application/json"))
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // SQLite 导出
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_export_db)) },
                        supportingContent = { Text(stringResource(R.string.settings_export_db_desc)) },
                        leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable {
                            dbExportLauncher.launch("assetguard_backup.db")
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // SQLite 导入
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_import_db)) },
                        supportingContent = { Text(stringResource(R.string.settings_import_db_desc)) },
                        leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable {
                            dbImportLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3"))
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 数据完整性检查
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_integrity_check)) },
                        supportingContent = { Text(stringResource(R.string.settings_integrity_check_desc)) },
                        leadingContent = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
                        colors = listItemColors,
                        modifier = Modifier.clickable { viewModel.runIntegrityCheck() }
                    )
                }
            }

            // 关于
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    supportingContent = { Text(stringResource(R.string.settings_version)) },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    colors = listItemColors
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.md))
        }
    }

    // 权限说明对话框
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            onDismiss = { showRationaleDialog = false },
            onGoToSettings = {
                showRationaleDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    // 导入预览对话框
    when (val state = importState) {
        is ImportState.Previewing -> {
            ImportPreviewDialog(
                preview = state.preview,
                onConfirm = { strategy ->
                    viewModel.confirmImport(state.data, strategy)
                },
                onDismiss = { viewModel.resetImportState() }
            )
        }
        is ImportState.Importing -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text(stringResource(R.string.import_title_importing)) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.import_msg_importing))
                    }
                }
            )
        }
        is ImportState.Done -> {
            ImportResultDialog(
                result = state.result,
                onDismiss = { viewModel.resetImportState() }
            )
        }
        is ImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetImportState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetImportState() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                title = { Text(stringResource(R.string.import_title_failed)) },
                text = { Text(state.message) }
            )
        }
        ImportState.Idle -> {}
    }

    // 完整性检查报告对话框
    integrityReport?.let { report ->
        IntegrityReportDialog(
            report = report,
            onDismiss = { viewModel.clearIntegrityReport() }
        )
    }
}
