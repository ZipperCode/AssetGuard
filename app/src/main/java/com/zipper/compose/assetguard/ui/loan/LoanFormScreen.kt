package com.zipper.compose.assetguard.ui.loan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.DatePickerField
import com.zipper.compose.assetguard.ui.components.PersonSelector
import com.zipper.compose.assetguard.ui.theme.spacing
import com.zipper.compose.assetguard.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    personId: Long?,
    loanId: Long?,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: LoanFormViewModel = viewModel(factory = LoanFormViewModel.factory(container, personId, loanId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    var showDueDatePicker by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // 首次加载时如果有支付方式且未选择，默认选第一个
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
                title = { Text(stringResource(if (uiState.isEditing) R.string.loan_form_title_edit else R.string.loan_form_title_add)) },
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
                    // Person selector
                    PersonSelector(
                        selectedPerson = uiState.selectedPerson,
                        newPersonName = uiState.newPersonName,
                        query = uiState.personQuery,
                        suggestions = uiState.personSuggestions,
                        isLocked = uiState.isPersonLocked,
                        error = uiState.personError,
                        onQueryChanged = viewModel::onPersonQueryChanged,
                        onPersonSelected = viewModel::onPersonSelected,
                        onNewPersonSelected = viewModel::onNewPersonSelected,
                        onClear = viewModel::clearPersonSelection,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    OutlinedTextField(
                        value = uiState.amountText,
                        onValueChange = viewModel::onAmountChanged,
                        label = { Text(stringResource(R.string.loan_form_label_amount)) },
                        isError = uiState.amountError != null,
                        supportingText = uiState.amountError?.let { resId -> { Text(stringResource(resId)) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("\u00A5") },
                        colors = inputColors,
                    )

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    // 借款日期
                    DatePickerField(
                        label = stringResource(R.string.loan_form_label_loan_date),
                        value = uiState.loanDate,
                        onValueChange = viewModel::onLoanDateChanged,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(MaterialTheme.spacing.md))

                    // 到期日
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.dueDate?.let { DateUtils.formatDate(it) } ?: stringResource(R.string.label_not_set),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.loan_form_label_due_date)) },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            isError = uiState.dateError != null,
                            supportingText = uiState.dateError?.let { resId -> { Text(stringResource(resId)) } },
                            colors = inputColors,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                                LaunchedEffect(it) {
                                    it.interactions.collect { interaction ->
                                        if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showDueDatePicker = true
                                        }
                                    }
                                }
                            }
                        )
                        if (uiState.dueDate != null) {
                            Spacer(Modifier.width(MaterialTheme.spacing.sm))
                            TextButton(onClick = { viewModel.onDueDateChanged(null) }) {
                                Text(stringResource(R.string.action_clear))
                            }
                        }
                    }

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
                            label = { Text(stringResource(R.string.loan_form_label_payment_method)) },
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

    // 到期日选择器
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDueDateChanged(it) }
                    showDueDatePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
