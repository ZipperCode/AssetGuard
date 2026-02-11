package com.zipper.compose.assetguard.ui.person

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    var loanToDelete by remember { mutableStateOf<LoanEntity?>(null) }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.person?.name ?: "联系人详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEditPerson) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLoan) {
                Icon(Icons.Default.Add, contentDescription = "添加借条")
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
            // 人员信息卡片
            uiState.person?.let { person ->
                item {
                    PersonInfoCard(
                        name = person.name,
                        phone = person.phone,
                        note = person.note,
                        totalLent = uiState.totalLent,
                        totalRepaid = uiState.totalRepaid,
                        totalOutstanding = uiState.totalOutstanding
                    )
                }
            }

            // 借条列表
            if (uiState.loans.isEmpty()) {
                item {
                    EmptyStateView(message = "暂无借条记录，点击右下角添加")
                }
            } else {
                item {
                    Text(
                        text = "借条记录 (${uiState.loans.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(uiState.loans, key = { it.loan.id }) { loanWithRepayments ->
                    LoanCard(
                        loanWithRepayments = loanWithRepayments,
                        onClick = { onLoanClick(loanWithRepayments.loan.id) },
                        onDelete = { loanToDelete = loanWithRepayments.loan }
                    )
                }
            }
        }
    }

    loanToDelete?.let { loan ->
        ConfirmDialog(
            title = "删除借条",
            message = "确定要删除这笔 ${MoneyUtils.formatCents(loan.amount)} 的借条吗？相关还款记录也会被删除。",
            onConfirm = {
                viewModel.deleteLoan(loan)
                loanToDelete = null
            },
            onDismiss = { loanToDelete = null }
        )
    }
}

@Composable
private fun PersonInfoCard(
    name: String,
    phone: String?,
    note: String?,
    totalLent: Long,
    totalRepaid: Long,
    totalOutstanding: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (!phone.isNullOrBlank()) {
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总借出", style = MaterialTheme.typography.labelSmall)
                    MoneyText(cents = totalLent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("已还", style = MaterialTheme.typography.labelSmall)
                    MoneyText(cents = totalRepaid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("待还", style = MaterialTheme.typography.labelSmall)
                    MoneyText(
                        cents = totalOutstanding,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalOutstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    loanWithRepayments: LoanWithRepayments,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val loan = loanWithRepayments.loan
    val progress = if (loan.amount > 0) {
        (loanWithRepayments.totalRepaid.toFloat() / loan.amount).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoneyText(
                    cents = loan.amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = loan.status)
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 还款进度
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已还 ${MoneyUtils.formatCents(loanWithRepayments.totalRepaid)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "剩余 ${MoneyUtils.formatCents(loanWithRepayments.remaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "借款日: ${DateUtils.formatDate(loan.loanDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                loan.dueDate?.let { DueDateIndicator(dueDate = it) }
            }
        }
    }
}
