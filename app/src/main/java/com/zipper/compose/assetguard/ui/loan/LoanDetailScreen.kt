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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.PaymentMethodChip
import com.zipper.compose.assetguard.ui.components.StatusChip
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
    val snackbarHostState = remember { SnackbarHostState() }

    val loanWithRepayments = uiState.loanWithRepayments

    // 可恢复删除 Snackbar
    LaunchedEffect(uiState.pendingDeleteRepaymentId) {
        uiState.pendingDeleteRepaymentId?.let {
            val result = snackbarHostState.showSnackbar(
                message = "还款记录将被删除",
                actionLabel = "撤销",
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
                title = { Text("借条详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (loanWithRepayments != null) {
                        IconButton(onClick = { onEditLoan(loanWithRepayments.loan.personId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑借条")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (loanWithRepayments != null && loanWithRepayments.remaining > 0) {
                FloatingActionButton(onClick = onAddRepayment) {
                    Icon(Icons.Default.Add, contentDescription = "添加还款")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (loanWithRepayments == null) {
            EmptyStateView(
                message = "借条不存在",
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 借条信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoneyText(
                                cents = loan.amount,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            StatusChip(status = loan.status)
                        }

                        Spacer(Modifier.height(12.dp))

                        // 进度条
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "已还 ${MoneyUtils.formatCents(loanWithRepayments.totalRepaid)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "剩余 ${MoneyUtils.formatCents(loanWithRepayments.remaining)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("借款日期", style = MaterialTheme.typography.labelSmall)
                                Text(DateUtils.formatDate(loan.loanDate), style = MaterialTheme.typography.bodyMedium)
                            }
                            loan.dueDate?.let {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("到期日", style = MaterialTheme.typography.labelSmall)
                                    DueDateIndicator(dueDate = it)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val methodName = uiState.paymentMethods[loan.paymentMethodId]?.name
                        if (methodName != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("支付方式: ", style = MaterialTheme.typography.labelSmall)
                                PaymentMethodChip(name = methodName)
                            }
                        }

                        if (!loan.note.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "备注: ${loan.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 还款记录标题
            item {
                Text(
                    text = "还款记录 (${loanWithRepayments.repayments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (loanWithRepayments.repayments.isEmpty()) {
                item { EmptyStateView(message = "暂无还款记录") }
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
            title = "删除还款记录",
            message = "确定要删除这笔 ${MoneyUtils.formatCents(repayment.amount)} 的还款记录吗？借条状态将自动重算。",
            onConfirm = {
                viewModel.deleteRepayment(repayment)
                repaymentToDelete = null
            },
            onDismiss = { repaymentToDelete = null }
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
                .padding(12.dp)
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
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DateUtils.formatDate(repayment.repayDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (paymentMethodName != null) {
                        Spacer(Modifier.width(8.dp))
                        PaymentMethodChip(name = paymentMethodName)
                    }
                }
                if (!repayment.note.isNullOrBlank()) {
                    Text(
                        text = repayment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}
