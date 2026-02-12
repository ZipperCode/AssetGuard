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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.data.backup.BackupManager
import com.zipper.compose.assetguard.data.backup.ConflictStrategy
import com.zipper.compose.assetguard.data.backup.DatabaseBackupManager
import com.zipper.compose.assetguard.data.backup.DataIntegrityChecker
import com.zipper.compose.assetguard.data.backup.ImportPreview
import com.zipper.compose.assetguard.data.backup.ImportResult
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.PermissionRationaleDialog
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
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
                    if (result.isSuccess) "JSON 备份导出成功" else "导出失败: ${result.exceptionOrNull()?.message}"
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
                    if (result.isSuccess) "数据库备份导出成功" else "导出失败: ${result.exceptionOrNull()?.message}"
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
                    if (result.isSuccess) "数据库恢复成功，请重启应用" else "导入失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 通知设置
            Text(
                text = "通知提醒",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("通知权限") },
                supportingContent = { Text(if (prefs.notificationPermissionAsked) "已请求过权限" else "点击授权通知") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // 提醒时间
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("提醒时间", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "每日 ${String.format("%02d:%02d", prefs.reminderHour, prefs.reminderMinute)} 推送提醒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("提前提醒天数: ${prefs.reminderAdvanceDays} 天", style = MaterialTheme.typography.bodySmall)
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
                            text = "仅提醒已逾期",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = prefs.onlyOverdue,
                            onCheckedChange = viewModel::updateOnlyOverdue
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "静音时段: ${prefs.silentStartHour}:00 - ${prefs.silentEndHour}:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 支付方式管理
            ListItem(
                headlineContent = { Text("支付方式管理") },
                supportingContent = { Text("管理微信、支付宝等支付方式") },
                leadingContent = { Icon(Icons.Default.Payment, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onPaymentMethodManage)
            )
            HorizontalDivider()

            // 数据备份
            Text(
                text = "数据备份",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("导出 JSON 备份") },
                supportingContent = { Text("跨平台可读的 JSON 格式") },
                leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                modifier = Modifier.clickable {
                    jsonExportLauncher.launch("assetguard_backup.json")
                }
            )

            ListItem(
                headlineContent = { Text("导入 JSON 备份") },
                supportingContent = { Text("从 JSON 文件恢复数据（支持预览）") },
                leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                modifier = Modifier.clickable {
                    jsonImportLauncher.launch(arrayOf("application/json"))
                }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("导出数据库备份") },
                supportingContent = { Text("SQLite 数据库文件，快速恢复") },
                leadingContent = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                modifier = Modifier.clickable {
                    dbExportLauncher.launch("assetguard_backup.db")
                }
            )

            ListItem(
                headlineContent = { Text("导入数据库备份") },
                supportingContent = { Text("从 SQLite 文件恢复（需重启应用）") },
                leadingContent = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                modifier = Modifier.clickable {
                    dbImportLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3"))
                }
            )
            HorizontalDivider()

            // 数据完整性检查
            Text(
                text = "数据维护",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("数据完整性检查") },
                supportingContent = { Text("检测孤儿记录、状态不一致等问题") },
                leadingContent = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
                modifier = Modifier.clickable { viewModel.runIntegrityCheck() }
            )
            HorizontalDivider()

            // 关于
            ListItem(
                headlineContent = { Text("关于") },
                supportingContent = { Text("AssetGuard v1.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
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
                title = { Text("正在导入") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("请稍候，正在导入数据...")
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
                        Text("确定")
                    }
                },
                title = { Text("导入失败") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportPreviewDialog(
    preview: ImportPreview,
    onConfirm: (ConflictStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ConflictStrategy.SKIP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedStrategy) }) {
                Text("开始导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("导入预览") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("即将导入以下数据：", style = MaterialTheme.typography.bodyMedium)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        PreviewRow("联系人", "${preview.newPersonCount} 条")
                        PreviewRow("借条", "${preview.newLoanCount} 条")
                        PreviewRow("还款记录", "${preview.newRepaymentCount} 条")
                        PreviewRow("支付方式", "${preview.newPaymentMethodCount} 条")
                        if (preview.totalAmount > 0) {
                            PreviewRow("涉及金额", MoneyUtils.formatCents(preview.totalAmount))
                        }
                        if (preview.dateRange.isNotEmpty()) {
                            PreviewRow("时间跨度", preview.dateRange)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("冲突处理策略：", style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.SKIP,
                        onClick = { selectedStrategy = ConflictStrategy.SKIP },
                        label = { Text("跳过") }
                    )
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.OVERWRITE,
                        onClick = { selectedStrategy = ConflictStrategy.OVERWRITE },
                        label = { Text("覆盖") }
                    )
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.MERGE,
                        onClick = { selectedStrategy = ConflictStrategy.MERGE },
                        label = { Text("合并") }
                    )
                }
            }
        }
    )
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        icon = {
            if (result.isSuccess) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text(if (result.isSuccess) "导入成功" else "导入完成（有错误）") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        PreviewRow("联系人", "${result.personsImported} 条")
                        PreviewRow("借条", "${result.loansImported} 条")
                        PreviewRow("还款记录", "${result.repaymentsImported} 条")
                        PreviewRow("支付方式", "${result.paymentMethodsImported} 条")
                    }
                }

                if (result.errors.isNotEmpty()) {
                    Text(
                        "错误信息 (${result.errors.size} 条)：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Column {
                        result.errors.take(5).forEach { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (result.errors.size > 5) {
                            Text(
                                text = "...还有 ${result.errors.size - 5} 条错误",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun IntegrityReportDialog(
    report: DataIntegrityChecker.IntegrityReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        icon = {
            if (report.isHealthy) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text(if (report.isHealthy) "数据完整性检查通过" else "发现数据问题") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (report.isHealthy) {
                    Text(
                        "所有数据记录关联完整，状态一致。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (report.orphanLoans > 0) {
                                PreviewRow("孤儿借条", "${report.orphanLoans} 条")
                            }
                            if (report.orphanRepayments > 0) {
                                PreviewRow("孤儿还款", "${report.orphanRepayments} 条")
                            }
                            if (report.statusMismatches > 0) {
                                PreviewRow("状态不一致", "${report.statusMismatches} 条")
                            }
                        }
                    }

                    Column {
                        report.issues.take(5).forEach { issue ->
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (report.issues.size > 5) {
                            Text(
                                text = "...还有 ${report.issues.size - 5} 个问题",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
