package com.zipper.compose.assetguard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.components.ConfirmDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodManageScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val paymentMethods by container.paymentMethodRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var newMethodName by remember { mutableStateOf("") }
    var methodToDelete by remember { mutableStateOf<PaymentMethodEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("支付方式管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加支付方式")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(paymentMethods, key = { it.id }) { method ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (method.isBuiltin) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "内置",
                                    modifier = Modifier.padding(start = 8.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        if (!method.isBuiltin) {
                            IconButton(onClick = { methodToDelete = method }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddDialog = false; newMethodName = "" },
            title = { Text("添加支付方式") },
            text = {
                OutlinedTextField(
                    value = newMethodName,
                    onValueChange = { newMethodName = it },
                    label = { Text("名称") },
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
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newMethodName = "" }) { Text("取消") }
            }
        )
    }

    // 删除确认
    methodToDelete?.let { method ->
        ConfirmDialog(
            title = "删除支付方式",
            message = "确定要删除「${method.name}」吗？",
            onConfirm = {
                scope.launch {
                    val result = container.paymentMethodRepository.delete(method)
                    result.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: "删除失败")
                    }
                }
                methodToDelete = null
            },
            onDismiss = { methodToDelete = null }
        )
    }
}
