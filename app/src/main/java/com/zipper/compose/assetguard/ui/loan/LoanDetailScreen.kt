package com.zipper.compose.assetguard.ui.loan

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.GradientCard
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.PaymentMethodChip
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.ui.theme.StatusPaid
import com.zipper.compose.assetguard.ui.theme.StatusPartial
import com.zipper.compose.assetguard.ui.theme.StatusUnpaid
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    container: AppContainer,
    onBack: () -> Unit,
    onEditLoan: (personId: Long) -> Unit,
    onAddRepayment: () -> Unit,
    onEditRepayment: (repaymentId: Long) -> Unit,
    viewModel: LoanDetailViewModel = viewModel(factory = LoanDetailViewModel.factory(container, loanId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var repaymentToDelete by remember { mutableStateOf<RepaymentEntity?>(null) }
    var showDeleteLoanConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val loanWithRepayments = uiState.loanWithRepayments

    // 可恢复删除 Snackbar
    LaunchedEffect(uiState.pendingDeleteRepaymentId) {
        uiState.pendingDeleteRepaymentId?.let {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.loan_detail_repayment_will_delete),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteRepayment()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (loanWithRepayments != null) {
                        IconButton(onClick = { onEditLoan(loanWithRepayments.loan.personId) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.loan_detail_edit_loan))
                        }
                        IconButton(onClick = { showDeleteLoanConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = StatusUnpaid
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            if (loanWithRepayments != null && loanWithRepayments.remaining > 0) {
                FloatingActionButton(onClick = onAddRepayment) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.loan_detail_add_repayment))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (loanWithRepayments == null) {
            EmptyStateView(
                message = stringResource(R.string.loan_detail_empty),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            return@Scaffold
        }

        val loan = loanWithRepayments.loan
        val progress = if (loan.amount > 0) {
            (loanWithRepayments.totalRepaid.toFloat() / loan.amount).coerceIn(0f, 1f)
        } else 0f

        val progressColor = when {
            progress >= 1f -> StatusPaid
            progress > 0f -> StatusPartial
            else -> StatusUnpaid
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // 金额渐变卡片
            item {
                GradientCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoneyText(
                                cents = loan.amount,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            StatusChip(status = loan.status)
                        }

                        Spacer(Modifier.height(MaterialTheme.spacing.md))

                        // 进度条
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.2f),
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.loan_detail_repaid_amount, MoneyUtils.formatCents(loanWithRepayments.totalRepaid)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                stringResource(R.string.loan_detail_remaining_amount, MoneyUtils.formatCents(loanWithRepayments.remaining)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 借条详情信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                        // 借款日期
                        InfoRow(
                            label = stringResource(R.string.loan_detail_loan_date),
                            value = DateUtils.formatDate(loan.loanDate)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 到期日
                        loan.dueDate?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MaterialTheme.spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.loan_detail_due_date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                DueDateIndicator(dueDate = it)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // 支付方式
                        val methodName = uiState.paymentMethods[loan.paymentMethodId]?.name
                        if (methodName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MaterialTheme.spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.loan_detail_payment_method),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PaymentMethodChip(name = methodName)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // 备注
                        if (!loan.note.isNullOrBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MaterialTheme.spacing.md)
                            ) {
                                Text(
                                    text = stringResource(R.string.label_note),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                                Text(
                                    text = loan.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 还款记录标题
            item {
                Text(
                    text = stringResource(R.string.loan_detail_repayment_records, loanWithRepayments.repayments.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs)
                )
            }

            if (loanWithRepayments.repayments.isEmpty()) {
                item {
                    EmptyStateView(message = stringResource(R.string.loan_detail_empty_repayment))
                }
            } else {
                items(loanWithRepayments.repayments, key = { it.id }) { repayment ->
                    RepaymentCard(
                        repayment = repayment,
                        paymentMethodName = uiState.paymentMethods[repayment.paymentMethodId]?.name,
                        onEdit = { onEditRepayment(repayment.id) },
                        onDelete = { repaymentToDelete = repayment }
                    )
                }
            }
        }
    }

    repaymentToDelete?.let { repayment ->
        ConfirmDialog(
            title = stringResource(R.string.loan_detail_delete_repayment_title),
            message = stringResource(R.string.loan_detail_delete_repayment_msg, MoneyUtils.formatCents(repayment.amount)),
            onConfirm = {
                viewModel.deleteRepayment(repayment)
                repaymentToDelete = null
            },
            onDismiss = { repaymentToDelete = null }
        )
    }

    if (showDeleteLoanConfirm && loanWithRepayments != null) {
        val repaymentCount = loanWithRepayments.repayments.size
        ConfirmDialog(
            title = stringResource(R.string.person_detail_delete_loan_title),
            message = stringResource(R.string.person_detail_delete_loan_msg, MoneyUtils.formatCents(loanWithRepayments.loan.amount)),
            impactDescription = if (repaymentCount > 0) stringResource(R.string.person_detail_delete_loan_impact, repaymentCount) else null,
            onConfirm = {
                viewModel.deleteLoan()
                showDeleteLoanConfirm = false
                onBack()
            },
            onDismiss = { showDeleteLoanConfirm = false }
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RepaymentCard(
    repayment: RepaymentEntity,
    paymentMethodName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.md)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MoneyText(
                    cents = repayment.amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DateUtils.formatDate(repayment.repayDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (paymentMethodName != null) {
                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
                        PaymentMethodChip(name = paymentMethodName)
                    }
                }
                if (!repayment.note.isNullOrBlank()) {
                    Text(
                        text = repayment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}
