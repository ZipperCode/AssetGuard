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
import androidx.compose.ui.res.stringResource
import com.zipper.compose.assetguard.R

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
            Text(stringResource(R.string.batch_selected_count, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.batch_cancel_selection))
            }
        },
        actions = {
            if (onRemind != null) {
                IconButton(onClick = onRemind) {
                    Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.batch_send_remind))
                }
            }
            if (onExport != null) {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.batch_export))
                }
            }
            if (onArchive != null) {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.batch_archive))
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
