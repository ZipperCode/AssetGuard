package com.zipper.compose.assetguard.util

import java.text.DecimalFormat

object MoneyUtils {

    private val formatter = DecimalFormat("#,##0.00")

    /** 分转元字符串，如 12345 -> "123.45" */
    fun centsToYuanString(cents: Long): String {
        return formatter.format(cents / 100.0)
    }

    /** 分转元显示（带 ¥ 前缀），如 12345 -> "¥123.45" */
    fun formatCents(cents: Long): String {
        return "¥${centsToYuanString(cents)}"
    }

    /** 元字符串转分，如 "123.45" -> 12345L */
    fun yuanStringToCents(yuan: String): Long {
        val cleaned = yuan.replace(",", "").trim()
        if (cleaned.isEmpty()) return 0L
        return try {
            (cleaned.toDouble() * 100).toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /** 验证金额输入是否合法（允许最多两位小数） */
    fun isValidAmountInput(input: String): Boolean {
        if (input.isEmpty()) return true
        return input.matches(Regex("^\\d{0,10}(\\.\\d{0,2})?$"))
    }
}
