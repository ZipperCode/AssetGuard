package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.ui.theme.extendedColorScheme

@Composable
fun StatusChip(
    status: Int,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when (status) {
        LoanStatus.UNPAID -> Triple(
            "未还",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        LoanStatus.PARTIAL -> Triple(
            "部分归还",
            MaterialTheme.extendedColorScheme.warningContainer,
            MaterialTheme.extendedColorScheme.onWarningContainer
        )
        LoanStatus.PAID -> Triple(
            "已还清",
            MaterialTheme.extendedColorScheme.successContainer,
            MaterialTheme.extendedColorScheme.onSuccessContainer
        )
        LoanStatus.OVERDUE -> Triple(
            "逾期",
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError
        )
        LoanStatus.DISPUTED -> Triple(
            "争议中",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        LoanStatus.BAD_DEBT -> Triple(
            "坏账",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        LoanStatus.ARCHIVED -> Triple(
            "已归档",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Triple(
            "未知",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
