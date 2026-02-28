package com.zipper.compose.assetguard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zipper.compose.assetguard.R

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_notification_title)) },
        text = {
            Text(stringResource(R.string.permission_notification_desc))
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(stringResource(R.string.permission_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_later))
            }
        }
    )
}
