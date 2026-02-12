package com.zipper.compose.assetguard.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
    onLoanClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFilter) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 搜索栏
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索联系人或借条...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // 筛选面板
            item {
                AnimatedVisibility(visible = uiState.isFilterExpanded) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "状态筛选",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                TextButton(onClick = viewModel::clearFilters) {
                                    Text("清除筛选")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val statuses = listOf(
                                    LoanStatus.UNPAID to "未还",
                                    LoanStatus.PARTIAL to "部分归还",
                                    LoanStatus.OVERDUE to "逾期",
                                    LoanStatus.DISPUTED to "争议中",
                                    LoanStatus.BAD_DEBT to "坏账"
                                )
                                statuses.forEach { (status, label) ->
                                    FilterChip(
                                        selected = uiState.filter.statusFilter == status,
                                        onClick = {
                                            viewModel.onFilterChanged(
                                                uiState.filter.copy(
                                                    statusFilter = if (uiState.filter.statusFilter == status) null else status
                                                )
                                            )
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 联系人结果
            if (uiState.persons.isNotEmpty()) {
                item {
                    Text(
                        text = "联系人 (${uiState.persons.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.persons, key = { "person_${it.person.id}" }) { person ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonClick(person.person.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = person.person.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                person.person.phone?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                MoneyText(
                                    cents = person.outstanding,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (person.outstanding > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${person.unpaidLoanCount} 笔未结清",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 借条结果
            if (uiState.loans.isNotEmpty()) {
                item {
                    Text(
                        text = "借条 (${uiState.loans.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.loans, key = { "loan_${it.id}" }) { loan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoanClick(loan.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                MoneyText(
                                    cents = loan.amount,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Text(
                                        text = "借款日: ${DateUtils.formatDate(loan.loanDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    loan.dueDate?.let {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = DateUtils.dueDateDescription(it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (DateUtils.daysUntilDue(it) < 0) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            StatusChip(status = loan.status)
                        }
                    }
                }
            }

            // 空状态
            if (uiState.query.isNotBlank() && uiState.persons.isEmpty() && uiState.loans.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "未找到匹配结果",
                        icon = Icons.Default.Search
                    )
                }
            }

            if (uiState.query.isBlank() && uiState.filter == SearchFilter()) {
                item {
                    EmptyStateView(
                        message = "输入关键词或使用筛选条件搜索",
                        icon = Icons.Default.Search
                    )
                }
            }
        }
    }
}
