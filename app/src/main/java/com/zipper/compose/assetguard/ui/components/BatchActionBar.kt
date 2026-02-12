package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchActionBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onRemind: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text("已选择 $selectedCount 项")
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "取消选择")
            }
        },
        actions = {
            if (onRemind != null) {
                IconButton(onClick = onRemind) {
                    Icon(Icons.Default.Notifications, contentDescription = "发送提醒")
                }
            }
            if (onExport != null) {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.IosShare, contentDescription = "导出")
                }
            }
            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, contentDescription = "归档")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
