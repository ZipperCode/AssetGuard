package com.zipper.compose.assetguard.ui.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.AvatarView
import com.zipper.compose.assetguard.ui.components.EmptyStateView
import com.zipper.compose.assetguard.ui.components.MoneyText
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onPersonClick: (Long) -> Unit,
    onLoanClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(container))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // 搜索栏 — filled style, rounded corners 20dp, background surfaceVariant (#1A1A1D)
            item {
                TextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.action_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            // 筛选标签 — 始终显示 FlowRow，无展开/折叠
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.search_status_filter),
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(onClick = viewModel::clearFilters) {
                            Text(stringResource(R.string.search_clear_filter))
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        val statuses = listOf(
                            LoanStatus.UNPAID to stringResource(R.string.status_unpaid),
                            LoanStatus.PARTIAL to stringResource(R.string.status_partial),
                            LoanStatus.OVERDUE to stringResource(R.string.status_overdue),
                            LoanStatus.DISPUTED to stringResource(R.string.status_disputed),
                            LoanStatus.BAD_DEBT to stringResource(R.string.status_bad_debt)
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

            // 联系人结果
            if (uiState.persons.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.search_persons_count, uiState.persons.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.persons, key = { "person_${it.person.id}" }) { person ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonClick(person.person.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(MaterialTheme.spacing.lg)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(name = person.person.name)
                            Column(modifier = Modifier.weight(1f)) {
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
                                    text = stringResource(R.string.search_unpaid_count, person.unpaidLoanCount),
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
                        text = stringResource(R.string.search_loans_count, uiState.loans.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(uiState.loans, key = { "loan_${it.id}" }) { loan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoanClick(loan.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(MaterialTheme.spacing.lg)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                MoneyText(
                                    cents = loan.amount,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                                Row {
                                    Text(
                                        text = stringResource(R.string.search_loan_date, DateUtils.formatDate(loan.loanDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    loan.dueDate?.let {
                                        Spacer(Modifier.width(MaterialTheme.spacing.sm))
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
                        message = stringResource(R.string.search_empty_no_match),
                        icon = Icons.Default.Search
                    )
                }
            }

            if (uiState.query.isBlank() && uiState.filter == SearchFilter()) {
                item {
                    EmptyStateView(
                        message = stringResource(R.string.search_empty_hint),
                        icon = Icons.Default.Search
                    )
                }
            }
        }
    }
}
