package com.zipper.compose.assetguard.data.backup

import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val persons: List<PersonEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val repayments: List<RepaymentEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList()
)
