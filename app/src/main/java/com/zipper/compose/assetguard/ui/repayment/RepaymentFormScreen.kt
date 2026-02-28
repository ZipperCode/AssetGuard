package com.zipper.compose.assetguard.ui.repayment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.DatePickerField
import com.zipper.compose.assetguard.ui.components.GradientCard
import com.zipper.compose.assetguard.ui.components.StatusChip
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepaymentFormScreen(
    loanId: Long,
    repaymentId: Long?,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: RepaymentFormViewModel = viewModel(
        factory = RepaymentFormViewModel.factory(container, loanId, repaymentId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    LaunchedEffect(paymentMethods) {
        if (uiState.paymentMethodId == null && paymentMethods.isNotEmpty()) {
            viewModel.onPaymentMethodChanged(paymentMethods.first().id)
        }
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isEditing) R.string.repayment_form_title_edit else R.string.repayment_form_title_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = MaterialTheme.spacing.xs,
                shadowElevation = MaterialTheme.spacing.sm,
                color = MaterialTheme.colorScheme.background,
            ) {
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md)
                ) {
                    Text(stringResource(if (uiState.isSaving) R.string.action_saving else R.string.action_save))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(MaterialTheme.spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // 借条余额参考 - GradientCard
            if (uiState.loanAmount > 0) {
                val repaidAmount = uiState.loanAmount - uiState.remainingAmount
                val loanStatus = LoanStatus.fromRepaid(repaidAmount, uiState.loanAmount)

                GradientCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.repayment_form_remaining, MoneyUtils.formatCents(uiState.remainingAmount)),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            StatusChip(status = loanStatus)
                        }

                        Spacer(Modifier.height(MaterialTheme.spacing.sm))

                        Text(
                            text = stringResource(R.string.repayment_form_loan_amount, MoneyUtils.formatCents(uiState.loanAmount)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.lg))
            }

            // 表单区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.lg)
                ) {
                    OutlinedTextField(
                        value = uiState.amountText,
                        onValueChange = viewModel::onAmountChanged,
                        label = { Text(stringResource(R.string.repayment_form_label_amount)) },
                        isError = uiState.amountError != null,
                        supportingText = when {
                            uiState.amountError != null -> {{ Text(stringResource(uiState.amountError!!)) }}
                            uiState.overpayWarning != null -> {{
                                Text(
                                    text = uiState.overpayWarning!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }}
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("\u00A5") },
                        colors = inputColors,
                    )

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    // 还款日期
                    DatePickerField(
                        label = stringResource(R.string.repayment_form_label_date),
                        value = uiState.repayDate,
                        onValueChange = viewModel::onRepayDateChanged,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.dateError != null,
                        supportingText = uiState.dateError?.let { resId -> { Text(stringResource(resId)) } }
                    )

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    // 支付方式下拉
                    ExposedDropdownMenuBox(
                        expanded = paymentMethodExpanded,
                        onExpandedChange = { paymentMethodExpanded = it }
                    ) {
                        val selectedMethod = paymentMethods.find { it.id == uiState.paymentMethodId }
                        OutlinedTextField(
                            value = selectedMethod?.name ?: stringResource(R.string.label_select),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.repayment_form_label_payment_method)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            isError = uiState.paymentMethodError != null,
                            supportingText = uiState.paymentMethodError?.let { resId -> { Text(stringResource(resId)) } },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                            colors = inputColors,
                        )
                        ExposedDropdownMenu(
                            expanded = paymentMethodExpanded,
                            onDismissRequest = { paymentMethodExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.name) },
                                    onClick = {
                                        viewModel.onPaymentMethodChanged(method.id)
                                        paymentMethodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChanged,
                        label = { Text(stringResource(R.string.label_note)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = inputColors,
                    )
                }
            }
        }
    }
}
