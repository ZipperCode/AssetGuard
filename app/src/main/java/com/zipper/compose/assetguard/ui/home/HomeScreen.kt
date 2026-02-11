package com.zipper.compose.assetguard.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.components.DueDateIndicator
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onPersonClick: (Long) -> Unit,
    onAddPerson: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var personToDelete by remember { mutableStateOf<PersonWithSummary?>(null) }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产守护") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPerson) {
                Icon(Icons.Default.PersonAdd, contentDescription = "添加联系人")
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
            // 统计卡片
            item {
                SummaryCard(
                    totalLent = uiState.totalLent,
                    totalRepaid = uiState.totalRepaid,
                    totalOutstanding = uiState.totalOutstanding
                )
            }

            // 到期警示
            if (uiState.dueSoonLoans.isNotEmpty()) {
                item {
                    DueSoonSection(loans = uiState.dueSoonLoans)
                }
            }

            // 搜索栏
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索联系人...") },
                    singleLine = true
                )
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
                        onClick = { onPersonClick(personWithSummary.person.id) },
                        onDelete = { personToDelete = personWithSummary }
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
}

@Composable
private fun SummaryCard(
    totalLent: Long,
    totalRepaid: Long,
    totalOutstanding: Long
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

@Composable
private fun PersonCard(
    personWithSummary: PersonWithSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
