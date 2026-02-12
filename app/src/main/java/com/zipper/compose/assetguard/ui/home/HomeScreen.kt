package com.zipper.compose.assetguard.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.BatchActionBar
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batchMessage by viewModel.batchMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var personToDelete by remember { mutableStateOf<PersonWithSummary?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    // 批量导出 launcher
    val batchExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.batchExport(context, it) }
    }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    // 可恢复删除 Snackbar
    LaunchedEffect(uiState.pendingDeletePersonName) {
        uiState.pendingDeletePersonName?.let { name ->
            val result = snackbarHostState.showSnackbar(
                message = "「$name」将被删除",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    // 批量操作结果
    LaunchedEffect(batchMessage) {
        batchMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBatchMessage()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                BatchActionBar(
                    selectedCount = uiState.selectedPersonIds.size,
                    onClose = { viewModel.exitSelectionMode() },
                    onRemind = { viewModel.batchRemind(context) },
                    onExport = {
                        batchExportLauncher.launch("assetguard_partial_backup.json")
                    },
                    onArchive = { showArchiveConfirm = true }
                )
            } else {
                TopAppBar(
                    title = { Text("资产守护") },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(onClick = onAddPerson) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "添加联系人")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KPI 统计卡片
            item {
                SummaryCard(
                    totalLent = uiState.totalLent,
                    totalRepaid = uiState.totalRepaid,
                    totalOutstanding = uiState.totalOutstanding,
                    overdueCount = uiState.overdueCount,
                    dueTodayCount = uiState.dueTodayCount,
                    totalPending = uiState.totalPending
                )
            }

            // 到期警示
            if (uiState.dueSoonLoans.isNotEmpty()) {
                item {
                    DueSoonSection(loans = uiState.dueSoonLoans)
                }
            }

            // 搜索栏（非选择模式显示）
            if (!uiState.isSelectionMode) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索联系人...") },
                        singleLine = true
                    )
                }
            }

            // 联系人列表
            if (uiState.persons.isEmpty()) {
                item {
                    EmptyStateView(
                        message = if (uiState.searchQuery.isBlank()) "还没有联系人，点击右下角添加"
                        else "未找到匹配的联系人",
                        icon = Icons.Default.PersonAdd
                    )
                }
            } else {
                items(uiState.persons, key = { it.person.id }) { personWithSummary ->
                    PersonCard(
                        personWithSummary = personWithSummary,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected = personWithSummary.person.id in uiState.selectedPersonIds,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.togglePersonSelection(personWithSummary.person.id)
                            } else {
                                onPersonClick(personWithSummary.person.id)
                            }
                        },
                        onLongClick = {
                            if (!uiState.isSelectionMode) {
                                viewModel.enterSelectionMode(personWithSummary.person.id)
                            }
                        },
                        onDelete = {
                            if (!uiState.isSelectionMode) {
                                personToDelete = personWithSummary
                            }
                        }
                    )
                }
            }
        }
    }

    personToDelete?.let { person ->
        ConfirmDialog(
            title = "删除联系人",
            message = "确定要删除「${person.person.name}」吗？${
                if (person.unpaidLoanCount > 0) "\n\n该联系人还有 ${person.unpaidLoanCount} 笔未结清借条，无法删除。"
                else "相关的已结清借条记录也会被删除。"
            }",
            onConfirm = {
                viewModel.deletePerson(person)
                personToDelete = null
            },
            onDismiss = { personToDelete = null }
        )
    }

    if (showArchiveConfirm) {
        ConfirmDialog(
            title = "批量归档",
            message = "确定要归档选中的 ${uiState.selectedPersonIds.size} 位联系人的所有借条吗？\n归档后借条将不再参与统计。",
            onConfirm = {
                viewModel.batchArchive()
                showArchiveConfirm = false
            },
            onDismiss = { showArchiveConfirm = false }
        )
    }
}

@Composable
private fun SummaryCard(
    totalLent: Long,
    totalRepaid: Long,
    totalOutstanding: Long,
    overdueCount: Int,
    dueTodayCount: Int,
    totalPending: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "总览",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("总借出", totalLent)
                SummaryItem("已收回", totalRepaid)
                SummaryItem("待追回", totalOutstanding)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )

            // KPI 行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiItem(
                    label = "逾期",
                    value = "$overdueCount 笔",
                    isAlert = overdueCount > 0
                )
                KpiItem(
                    label = "今日到期",
                    value = "$dueTodayCount 笔",
                    isAlert = dueTodayCount > 0
                )
                KpiItem(
                    label = "待回款",
                    value = MoneyUtils.formatCents(totalPending),
                    isAlert = false
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = MoneyUtils.formatCents(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun KpiItem(label: String, value: String, isAlert: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isAlert) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DueSoonSection(loans: List<com.zipper.compose.assetguard.data.local.entity.LoanEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "到期提醒 (${loans.size}笔)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            loans.take(3).forEach { loan ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MoneyText(
                        cents = loan.amount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    loan.dueDate?.let {
                        DueDateIndicator(dueDate = it)
                    }
                }
            }
            if (loans.size > 3) {
                Text(
                    text = "还有 ${loans.size - 3} 笔...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonCard(
    personWithSummary: PersonWithSummary,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = personWithSummary.person.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (personWithSummary.person.phone != null) {
                    Text(
                        text = personWithSummary.person.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = "待还: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MoneyText(
                        cents = personWithSummary.outstanding,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (personWithSummary.outstanding > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (personWithSummary.unpaidLoanCount > 0) {
                        Text(
                            text = " · ${personWithSummary.unpaidLoanCount}笔未结清",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
