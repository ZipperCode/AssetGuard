package com.zipper.compose.assetguard.data.backup

data class ImportPreview(
    val newPersonCount: Int = 0,
    val newLoanCount: Int = 0,
    val newRepaymentCount: Int = 0,
    val newPaymentMethodCount: Int = 0,
    val totalAmount: Long = 0L,
    val dateRange: String = ""
)
