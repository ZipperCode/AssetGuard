package com.zipper.compose.assetguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.ui.theme.AssetGuardTheme
import com.zipper.compose.assetguard.ui.theme.StatusArchived
import com.zipper.compose.assetguard.ui.theme.StatusBadDebt
import com.zipper.compose.assetguard.ui.theme.StatusDisputed
import com.zipper.compose.assetguard.ui.theme.StatusOverdue
import com.zipper.compose.assetguard.ui.theme.StatusPaid
import com.zipper.compose.assetguard.ui.theme.StatusPartial
import com.zipper.compose.assetguard.ui.theme.StatusUnpaid

@Composable
fun StatusChip(
    status: Int,
    modifier: Modifier = Modifier
) {
    val (text, containerColor) = when (status) {
        LoanStatus.UNPAID -> Pair(
            stringResource(R.string.status_unpaid),
            StatusUnpaid
        )
        LoanStatus.PARTIAL -> Pair(
            stringResource(R.string.status_partial),
            StatusPartial
        )
        LoanStatus.PAID -> Pair(
            stringResource(R.string.status_paid),
            StatusPaid
        )
        LoanStatus.OVERDUE -> Pair(
            stringResource(R.string.status_overdue),
            StatusOverdue
        )
        LoanStatus.DISPUTED -> Pair(
            stringResource(R.string.status_disputed),
            StatusDisputed
        )
        LoanStatus.BAD_DEBT -> Pair(
            stringResource(R.string.status_bad_debt),
            StatusBadDebt
        )
        LoanStatus.ARCHIVED -> Pair(
            stringResource(R.string.status_archived),
            StatusArchived
        )
        else -> Pair(
            stringResource(R.string.status_unknown),
            StatusArchived
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor.copy(alpha = 0.15f),
        contentColor = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    AssetGuardTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            StatusChip(status = LoanStatus.UNPAID)
            StatusChip(status = LoanStatus.PARTIAL)
            StatusChip(status = LoanStatus.PAID)
            StatusChip(status = LoanStatus.OVERDUE)
            StatusChip(status = LoanStatus.DISPUTED)
            StatusChip(status = LoanStatus.BAD_DEBT)
            StatusChip(status = LoanStatus.ARCHIVED)
        }
    }
}
