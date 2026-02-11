package com.zipper.compose.assetguard.ui.components

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
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.util.DateUtils

@Composable
fun DueDateIndicator(
    dueDate: Long,
    modifier: Modifier = Modifier
) {
    val days = DateUtils.daysUntilDue(dueDate)
    val description = DateUtils.dueDateDescription(dueDate)

    val color = when {
        days < 0 -> MaterialTheme.colorScheme.error
        days <= 3 -> MaterialTheme.colorScheme.error
        days <= 7 -> MaterialTheme.colorScheme.tertiary
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
