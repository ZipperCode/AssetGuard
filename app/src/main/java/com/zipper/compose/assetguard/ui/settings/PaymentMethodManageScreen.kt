package com.zipper.compose.assetguard.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import com.zipper.compose.assetguard.ui.theme.spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodManageScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val paymentMethods by container.paymentMethodRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var newMethodName by remember { mutableStateOf("") }
    var methodToDelete by remember { mutableStateOf<PaymentMethodEntity?>(null) }

    val builtinMethods = paymentMethods.filter { it.isBuiltin }
    val customMethods = paymentMethods.filter { !it.isBuiltin }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payment_method_manage_title)) },
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
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "+ ${stringResource(R.string.payment_method_add)}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg)
        ) {
            // ---- 内置方式 ----
            if (builtinMethods.isNotEmpty()) {
                Text(
                    text = "\u5185\u7f6e\u65b9\u5f0f",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = MaterialTheme.spacing.lg,
                        bottom = MaterialTheme.spacing.sm
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    builtinMethods.forEachIndexed { index, method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = stringResource(R.string.label_builtin),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        if (index < builtinMethods.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg)
                            )
                        }
                    }
                }
            }

            // ---- 自定义方式 ----
            Text(
                text = "\u81ea\u5b9a\u4e49\u65b9\u5f0f",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = MaterialTheme.spacing.lg,
                    bottom = MaterialTheme.spacing.sm
                )
            )

            if (customMethods.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    customMethods.forEachIndexed { index, method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.xs, top = MaterialTheme.spacing.xs, bottom = MaterialTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { methodToDelete = method }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (index < customMethods.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "\u6682\u65e0\u81ea\u5b9a\u4e49\u652f\u4ed8\u65b9\u5f0f",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
        }
    }

    // 添加对话框
    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddDialog = false; newMethodName = "" },
            title = { Text(stringResource(R.string.payment_method_add_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newMethodName,
                    onValueChange = { newMethodName = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMethodName.isNotBlank()) {
                            scope.launch {
                                container.paymentMethodRepository.insert(
                                    PaymentMethodEntity(name = newMethodName.trim())
                                )
                                newMethodName = ""
                                showAddDialog = false
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newMethodName = "" }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // 删除确认
    methodToDelete?.let { method ->
        ConfirmDialog(
            title = stringResource(R.string.payment_method_delete_title),
            message = stringResource(R.string.payment_method_delete_msg, method.name),
            onConfirm = {
                scope.launch {
                    val result = container.paymentMethodRepository.delete(method)
                    result.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: context.getString(R.string.payment_method_delete_failed))
                    }
                }
                methodToDelete = null
            },
            onDismiss = { methodToDelete = null }
        )
    }
}
