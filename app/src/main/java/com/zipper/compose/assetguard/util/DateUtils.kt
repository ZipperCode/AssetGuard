package com.zipper.compose.assetguard.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val displayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 时间戳转显示字符串 yyyy-MM-dd */
    fun formatDate(timestamp: Long): String {
        return displayFormat.format(Date(timestamp))
    }

    /** 获取今天的开始时间戳（0点） */
    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 计算到期天数（正数=剩余天数，负数=已过期天数，0=今天到期） */
    fun daysUntilDue(dueDate: Long): Long {
        val today = todayStart()
        val dueDayStart = getDayStart(dueDate)
        return TimeUnit.MILLISECONDS.toDays(dueDayStart - today)
    }

    /** 获取某天的开始时间戳 */
    private fun getDayStart(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 获取 N 天后的时间戳 */
    fun daysFromNow(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    /** 到期状态描述 */
    fun dueDateDescription(dueDate: Long): String {
        val days = daysUntilDue(dueDate)
        return when {
            days < 0 -> "已过期${-days}天"
            days == 0L -> "今天到期"
            days <= 7 -> "${days}天后到期"
            else -> formatDate(dueDate)
        }
    }
}
