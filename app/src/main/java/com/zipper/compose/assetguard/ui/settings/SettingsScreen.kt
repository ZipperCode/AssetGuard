package com.zipper.compose.assetguard.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.data.backup.BackupManager
import com.zipper.compose.assetguard.data.backup.DatabaseBackupManager
import com.zipper.compose.assetguard.di.AppContainer
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
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(container) }
    val dbBackupManager = remember { DatabaseBackupManager(context, container.getDatabase()) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
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

    // JSON 导入
    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = backupManager.importJson(context, it)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) "JSON 数据导入成功" else "导入失败: ${result.exceptionOrNull()?.message}"
                )
            }
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
        ) {
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
                supportingContent = { Text("从 JSON 文件恢复数据") },
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

            // 关于
            ListItem(
                headlineContent = { Text("关于") },
                supportingContent = { Text("AssetGuard v1.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }
}
