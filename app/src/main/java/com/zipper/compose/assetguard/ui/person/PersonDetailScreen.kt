package com.zipper.compose.assetguard.ui.person

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.AvatarView
import com.zipper.compose.assetguard.ui.components.BatchActionBar
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.ui.theme.StatusPaid
import com.zipper.compose.assetguard.ui.theme.StatusPartial
import com.zipper.compose.assetguard.ui.theme.StatusUnpaid
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    container: AppContainer,
    onBack: () -> Unit,
    onEditPerson: () -> Unit,
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    viewModel: PersonDetailViewModel = viewModel(factory = PersonDetailViewModel.factory(container, personId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batchMessage by viewModel.batchMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    // 可恢复删除 Snackbar
    LaunchedEffect(uiState.pendingDeleteLoanName) {
        uiState.pendingDeleteLoanName?.let {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.person_detail_loan_will_delete),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteLoan()
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
            if (uiState.isLoanSelectionMode) {
                BatchActionBar(
                    selectedCount = uiState.selectedLoanIds.size,
                    onClose = { viewModel.exitLoanSelectionMode() },
                    onArchive = { showArchiveConfirm = true }
                )
            } else {
                TopAppBar(
                    title = { Text(uiState.person?.name ?: stringResource(R.string.person_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditPerson) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    )
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isLoanSelectionMode) {
                FloatingActionButton(onClick = onAddLoan) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.person_detail_add_loan))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // 人员信息卡片
            uiState.person?.let { person ->
                item {
                    PersonInfoCard(
                        name = person.name,
                        phone = person.phone,
                        note = person.note
                    )
                }

                // 财务统计卡片
                item {
                    FinancialStatsRow(
                        totalLent = uiState.totalLent,
                        totalRepaid = uiState.totalRepaid,
                        totalOutstanding = uiState.totalOutstanding
                    )
                }
            }

            // 借条列表
            if (uiState.loans.isEmpty()) {
                item {
                    EmptyStateView(message = stringResource(R.string.person_detail_empty_no_loan))
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.person_detail_loan_records, uiState.loans.size),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs)
                    )
                }
                items(uiState.loans, key = { it.loan.id }) { loanWithRepayments ->
                    LoanCard(
                        loanWithRepayments = loanWithRepayments,
                        isSelectionMode = uiState.isLoanSelectionMode,
                        isSelected = loanWithRepayments.loan.id in uiState.selectedLoanIds,
                        onClick = {
                            if (uiState.isLoanSelectionMode) {
                                viewModel.toggleLoanSelection(loanWithRepayments.loan.id)
                            } else {
                                onLoanClick(loanWithRepayments.loan.id)
                            }
                        },
                        onLongClick = {
                            if (!uiState.isLoanSelectionMode) {
                                viewModel.enterLoanSelectionMode(loanWithRepayments.loan.id)
                            }
                        },
                        onDelete = {
                            if (!uiState.isLoanSelectionMode) {
                                loanToDelete = loanWithRepayments.loan
                            }
                        }
                    )
                }
            }
        }
    }

    loanToDelete?.let { loan ->
        val repaymentCount = uiState.loans.find { it.loan.id == loan.id }?.repayments?.size ?: 0
        ConfirmDialog(
            title = stringResource(R.string.person_detail_delete_loan_title),
            message = stringResource(R.string.person_detail_delete_loan_msg, MoneyUtils.formatCents(loan.amount)),
            impactDescription = if (repaymentCount > 0) stringResource(R.string.person_detail_delete_loan_impact, repaymentCount) else null,
            onConfirm = {
                viewModel.deleteLoan(loan)
                loanToDelete = null
            },
            onDismiss = { loanToDelete = null }
        )
    }

    if (showArchiveConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.person_detail_batch_archive_title),
            message = stringResource(R.string.person_detail_batch_archive_msg, uiState.selectedLoanIds.size),
            onConfirm = {
                viewModel.batchArchiveLoans()
                showArchiveConfirm = false
            },
            onDismiss = { showArchiveConfirm = false }
        )
    }
}

@Composable
private fun PersonInfoCard(
    name: String,
    phone: String?,
    note: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                name = name,
                size = androidx.compose.ui.unit.Dp(48f)
            )
            Spacer(Modifier.width(MaterialTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!phone.isNullOrBlank()) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialStatsRow(
    totalLent: Long,
    totalRepaid: Long,
    totalOutstanding: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        StatCard(
            label = stringResource(R.string.person_detail_total_lent),
            cents = totalLent,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.person_detail_repaid),
            cents = totalRepaid,
            color = StatusPaid,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.person_detail_outstanding),
            cents = totalOutstanding,
            color = if (totalOutstanding > 0) StatusUnpaid else StatusPaid,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    cents: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            MoneyText(
                cents = cents,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoanCard(
    loanWithRepayments: LoanWithRepayments,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val loan = loanWithRepayments.loan
    val progress = if (loan.amount > 0) {
        (loanWithRepayments.totalRepaid.toFloat() / loan.amount).coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        progress >= 1f -> StatusPaid
        progress > 0f -> StatusPartial
        else -> StatusUnpaid
    }

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
        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(Modifier.width(MaterialTheme.spacing.sm))
                }
                MoneyText(
                    cents = loan.amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = loan.status)
                    if (!isSelectionMode) {
                        Spacer(Modifier.width(MaterialTheme.spacing.xs))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier)
                        }
                    }
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            // 还款进度
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(MaterialTheme.spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.person_detail_repaid_amount, MoneyUtils.formatCents(loanWithRepayments.totalRepaid)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.person_detail_remaining_amount, MoneyUtils.formatCents(loanWithRepayments.remaining)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.person_detail_loan_date, DateUtils.formatDate(loan.loanDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                loan.dueDate?.let { DueDateIndicator(dueDate = it) }
            }
        }
    }
}
