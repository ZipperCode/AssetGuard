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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    personId: Long,
    loanId: Long?,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: LoanFormViewModel = viewModel(factory = LoanFormViewModel.factory(container, personId, loanId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    var showLoanDatePicker by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑借条" else "新增借条") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("金额（元）*") },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("¥") }
            )

            Spacer(Modifier.height(12.dp))

            // 借款日期
            OutlinedTextField(
                value = DateUtils.formatDate(uiState.loanDate),
                onValueChange = {},
                label = { Text("借款日期") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = true,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also {
                    LaunchedEffect(it) {
                        it.interactions.collect { interaction ->
                            if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                showLoanDatePicker = true
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // 到期日
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.dueDate?.let { DateUtils.formatDate(it) } ?: "未设置",
                    onValueChange = {},
                    label = { Text("到期日（可选）") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
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
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.onDueDateChanged(null) }) {
                        Text("清除")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 支付方式下拉
            ExposedDropdownMenuBox(
                expanded = paymentMethodExpanded,
                onExpandedChange = { paymentMethodExpanded = it }
            ) {
                val selectedMethod = paymentMethods.find { it.id == uiState.paymentMethodId }
                OutlinedTextField(
                    value = selectedMethod?.name ?: "请选择",
                    onValueChange = {},
                    label = { Text("支付方式 *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) }
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

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "保存中..." else "保存")
            }
        }
    }

    // 借款日期选择器
    if (showLoanDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.loanDate)
        DatePickerDialog(
            onDismissRequest = { showLoanDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onLoanDateChanged(it) }
                    showLoanDatePicker = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showLoanDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
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
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
