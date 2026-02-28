package com.zipper.compose.assetguard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.backup.ConflictStrategy
import com.zipper.compose.assetguard.data.backup.DataIntegrityChecker
import com.zipper.compose.assetguard.data.backup.ImportPreview
import com.zipper.compose.assetguard.data.backup.ImportResult
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportPreviewDialog(
    preview: ImportPreview,
    onConfirm: (ConflictStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(ConflictStrategy.SKIP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedStrategy) }) {
                Text(stringResource(R.string.import_action_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = { Text(stringResource(R.string.import_title_preview)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Text(stringResource(R.string.import_msg_will_import), style = MaterialTheme.typography.bodyMedium)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                        PreviewRow(stringResource(R.string.label_person), "${preview.newPersonCount} 条")
                        PreviewRow(stringResource(R.string.label_loan), "${preview.newLoanCount} 条")
                        PreviewRow(stringResource(R.string.label_repayment), "${preview.newRepaymentCount} 条")
                        PreviewRow(stringResource(R.string.label_payment_method), "${preview.newPaymentMethodCount} 条")
                        if (preview.totalAmount > 0) {
                            PreviewRow(stringResource(R.string.label_total_amount), MoneyUtils.formatCents(preview.totalAmount))
                        }
                        if (preview.dateRange.isNotEmpty()) {
                            PreviewRow(stringResource(R.string.label_date_range), preview.dateRange)
                        }
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Text(stringResource(R.string.import_conflict_strategy), style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.SKIP,
                        onClick = { selectedStrategy = ConflictStrategy.SKIP },
                        label = { Text(stringResource(R.string.import_strategy_skip)) }
                    )
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.OVERWRITE,
                        onClick = { selectedStrategy = ConflictStrategy.OVERWRITE },
                        label = { Text(stringResource(R.string.import_strategy_overwrite)) }
                    )
                    FilterChip(
                        selected = selectedStrategy == ConflictStrategy.MERGE,
                        onClick = { selectedStrategy = ConflictStrategy.MERGE },
                        label = { Text(stringResource(R.string.import_strategy_merge)) }
                    )
                }
            }
        }
    )
}

@Composable
internal fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
        icon = {
            if (result.isSuccess) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text(stringResource(if (result.isSuccess) R.string.import_title_success else R.string.import_title_with_errors)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                        PreviewRow(stringResource(R.string.label_person), "${result.personsImported} 条")
                        PreviewRow(stringResource(R.string.label_loan), "${result.loansImported} 条")
                        PreviewRow(stringResource(R.string.label_repayment), "${result.repaymentsImported} 条")
                        PreviewRow(stringResource(R.string.label_payment_method), "${result.paymentMethodsImported} 条")
                    }
                }

                if (result.errors.isNotEmpty()) {
                    Text(
                        stringResource(R.string.import_error_count, result.errors.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Column {
                        result.errors.take(5).forEach { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (result.errors.size > 5) {
                            Text(
                                text = stringResource(R.string.import_more_errors, result.errors.size - 5),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
internal fun IntegrityReportDialog(
    report: DataIntegrityChecker.IntegrityReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
        icon = {
            if (report.isHealthy) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = { Text(stringResource(if (report.isHealthy) R.string.integrity_title_pass else R.string.integrity_title_fail)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                if (report.isHealthy) {
                    Text(
                        stringResource(R.string.integrity_healthy),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                            if (report.orphanLoans > 0) {
                                PreviewRow(stringResource(R.string.integrity_orphan_loans), "${report.orphanLoans} 条")
                            }
                            if (report.orphanRepayments > 0) {
                                PreviewRow(stringResource(R.string.integrity_orphan_repayments), "${report.orphanRepayments} 条")
                            }
                            if (report.statusMismatches > 0) {
                                PreviewRow(stringResource(R.string.integrity_status_mismatch), "${report.statusMismatches} 条")
                            }
                        }
                    }

                    Column {
                        report.issues.take(5).forEach { issue ->
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (report.issues.size > 5) {
                            Text(
                                text = stringResource(R.string.integrity_more_issues, report.issues.size - 5),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
