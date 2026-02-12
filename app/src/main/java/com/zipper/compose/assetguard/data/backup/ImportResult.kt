package com.zipper.compose.assetguard.data.backup

data class ImportResult(
    val personsImported: Int = 0,
    val loansImported: Int = 0,
    val repaymentsImported: Int = 0,
    val paymentMethodsImported: Int = 0,
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = errors.isEmpty()

    val summary: String
        get() = "导入完成: $personsImported 个联系人, $loansImported 条借条, $repaymentsImported 条还款" +
                if (errors.isNotEmpty()) "\n${errors.size} 条错误" else ""
}
