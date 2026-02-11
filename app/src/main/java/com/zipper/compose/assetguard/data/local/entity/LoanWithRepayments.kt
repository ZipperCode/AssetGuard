package com.zipper.compose.assetguard.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LoanWithRepayments(
    @Embedded
    val loan: LoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    val repayments: List<RepaymentEntity>
) {
    val totalRepaid: Long get() = repayments.sumOf { it.amount }
    val remaining: Long get() = loan.amount - totalRepaid
}
