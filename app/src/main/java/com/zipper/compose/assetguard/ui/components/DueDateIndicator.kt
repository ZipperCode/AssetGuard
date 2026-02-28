package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.ui.theme.AssetGuardTheme
import com.zipper.compose.assetguard.ui.theme.extendedColorScheme
import com.zipper.compose.assetguard.util.DateUtils

@Composable
fun DueDateIndicator(
    dueDate: Long,
    modifier: Modifier = Modifier
) {
    val days = DateUtils.daysUntilDue(dueDate)
    val status = DateUtils.dueDateStatus(dueDate)
    val description = when (status) {
        is DateUtils.DueDateStatus.Overdue -> stringResource(R.string.due_date_overdue, status.days.toInt())
        is DateUtils.DueDateStatus.DueToday -> stringResource(R.string.due_date_today)
        is DateUtils.DueDateStatus.DueSoon -> stringResource(R.string.due_date_soon, status.days.toInt())
        is DateUtils.DueDateStatus.Normal -> status.formattedDate
    }

    val color = when {
        days < 0 -> MaterialTheme.colorScheme.error
        days <= 3 -> MaterialTheme.colorScheme.error
        days <= 7 -> MaterialTheme.extendedColorScheme.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (days <= 3) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DueDateIndicatorPreview() {
    AssetGuardTheme {
        Column {
            // 过期
            DueDateIndicator(dueDate = System.currentTimeMillis() - 3 * 86400000L)
            // 今天到期
            DueDateIndicator(dueDate = System.currentTimeMillis())
            // 3天后到期
            DueDateIndicator(dueDate = System.currentTimeMillis() + 3 * 86400000L)
            // 30天后到期
            DueDateIndicator(dueDate = System.currentTimeMillis() + 30 * 86400000L)
        }
    }
}
