package com.zipper.compose.assetguard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要通知权限") },
        text = {
            Text("AssetGuard 需要通知权限来发送借条到期提醒，帮助您及时追回欠款。\n\n您可以在系统设置中手动开启通知权限。")
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("前往设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        }
    )
}
