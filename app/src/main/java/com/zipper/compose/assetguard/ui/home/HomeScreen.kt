package com.zipper.compose.assetguard.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.AvatarView
import com.zipper.compose.assetguard.ui.components.BatchActionBar
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.GradientCard
import com.zipper.compose.assetguard.ui.components.KpiIndicator
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.SectionHeader
import com.zipper.compose.assetguard.ui.components.SpeedDialFab
import com.zipper.compose.assetguard.ui.components.SpeedDialItem
import com.zipper.compose.assetguard.ui.theme.StatusOverdue
import com.zipper.compose.assetguard.ui.theme.StatusPartial
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onAddLoan: () -> Unit,
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
                message = context.getString(R.string.home_person_will_delete, name),
                actionLabel = context.getString(R.string.action_undo),
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    title = { Text(stringResource(R.string.home_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { /* notification click */ }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.settings_section_notification)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                SpeedDialFab(
                    items = listOf(
                        SpeedDialItem(
                            icon = Icons.Default.NoteAdd,
                            label = stringResource(R.string.home_add_loan),
                            onClick = onAddLoan
                        ),
                        SpeedDialItem(
                            icon = Icons.Default.PersonAdd,
                            label = stringResource(R.string.home_add_person),
                            onClick = onAddPerson
                        ),
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // 总览渐变卡片
            item {
                OverviewGradientCard(
                    totalLent = uiState.totalLent,
                    totalRepaid = uiState.totalRepaid,
                    totalOutstanding = uiState.totalOutstanding
                )
            }

            // KPI 指标行
            item {
                KpiRow(
                    overdueCount = uiState.overdueCount,
                    dueTodayCount = uiState.dueTodayCount,
                    totalPending = uiState.totalPending
                )
            }

            // 到期警示 — 横向滚动卡片
            if (uiState.dueSoonLoans.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.summary_due_soon, uiState.dueSoonLoans.size)
                    )
                }
                item {
                    DueSoonRow(loans = uiState.dueSoonLoans)
                }
            }

            // 联系人区块标题
            item {
                SectionHeader(title = stringResource(R.string.label_person))
            }

            // 联系人列表
            if (uiState.persons.isEmpty()) {
                item {
                    EmptyStateView(
                        message = stringResource(R.string.home_empty_no_person),
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
            title = stringResource(R.string.home_delete_person_title),
            message = stringResource(R.string.home_delete_person_msg, person.person.name) + (if (person.unpaidLoanCount > 0) "\n\n" + stringResource(R.string.home_delete_person_has_unpaid, person.unpaidLoanCount) else stringResource(R.string.home_delete_person_impact)),
            onConfirm = {
                viewModel.deletePerson(person)
                personToDelete = null
            },
            onDismiss = { personToDelete = null }
        )
    }

    if (showArchiveConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.home_batch_archive_title),
            message = stringResource(R.string.home_batch_archive_msg, uiState.selectedPersonIds.size),
            onConfirm = {
                viewModel.batchArchive()
                showArchiveConfirm = false
            },
            onDismiss = { showArchiveConfirm = false }
        )
    }
}

@Composable
private fun OverviewGradientCard(
    totalLent: Long,
    totalRepaid: Long,
    totalOutstanding: Long
) {
    GradientCard {
        Column {
            Text(
                text = stringResource(R.string.summary_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = stringResource(R.string.summary_total_lent),
                    amount = totalLent
                )
                SummaryItem(
                    label = stringResource(R.string.summary_repaid),
                    amount = totalRepaid
                )
                SummaryItem(
                    label = stringResource(R.string.summary_outstanding),
                    amount = totalOutstanding
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
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = MoneyUtils.formatCents(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun KpiRow(
    overdueCount: Int,
    dueTodayCount: Int,
    totalPending: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        KpiIndicator(
            value = stringResource(R.string.unit_count_loans, overdueCount),
            label = stringResource(R.string.summary_overdue),
            modifier = Modifier.weight(1f),
            valueColor = if (overdueCount > 0) StatusOverdue else MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )
        KpiIndicator(
            value = stringResource(R.string.unit_count_loans, dueTodayCount),
            label = stringResource(R.string.summary_due_today),
            modifier = Modifier.weight(1f),
            valueColor = if (dueTodayCount > 0) StatusPartial else MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )
        KpiIndicator(
            value = MoneyUtils.formatCents(totalPending),
            label = stringResource(R.string.summary_pending),
            modifier = Modifier.weight(1f),
            valueColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun DueSoonRow(loans: List<LoanEntity>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.xxs)
    ) {
        items(loans, key = { it.id }) { loan ->
            DueSoonCard(loan = loan)
        }
    }
}

@Composable
private fun DueSoonCard(loan: LoanEntity) {
    Card(
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusOverdue,
                    modifier = Modifier.padding(end = MaterialTheme.spacing.xs)
                )
                loan.dueDate?.let {
                    DueDateIndicator(dueDate = it)
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            MoneyText(
                cents = loan.amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
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
        ) else CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.lg)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(Modifier.width(MaterialTheme.spacing.sm))
            } else {
                AvatarView(
                    name = personWithSummary.person.name,
                    size = 40.dp
                )
                Spacer(Modifier.width(MaterialTheme.spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = personWithSummary.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (personWithSummary.person.phone != null) {
                    Text(
                        text = personWithSummary.person.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.person_card_outstanding),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MoneyText(
                        cents = personWithSummary.outstanding,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (personWithSummary.outstanding > 0) StatusOverdue
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (personWithSummary.unpaidLoanCount > 0) {
                        Text(
                            text = stringResource(R.string.person_card_unpaid_count, personWithSummary.unpaidLoanCount),
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
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
