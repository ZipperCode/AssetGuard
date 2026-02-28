package com.zipper.compose.assetguard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun formatDate_returnsCorrectFormat() {
        // 2024-01-15 00:00:00 UTC
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val result = DateUtils.formatDate(cal.timeInMillis)
        assertEquals("2024-01-15", result)
    }

    @Test
    fun todayStart_returnsStartOfDay() {
        val todayStart = DateUtils.todayStart()
        val cal = Calendar.getInstance().apply { timeInMillis = todayStart }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun daysUntilDue_todayReturnsZero() {
        val today = DateUtils.todayStart()
        assertEquals(0L, DateUtils.daysUntilDue(today))
    }

    @Test
    fun daysUntilDue_futureReturnsPositive() {
        val tomorrow = DateUtils.todayStart() + 86400000L
        assertEquals(1L, DateUtils.daysUntilDue(tomorrow))
    }

    @Test
    fun daysUntilDue_pastReturnsNegative() {
        val yesterday = DateUtils.todayStart() - 86400000L
        assertEquals(-1L, DateUtils.daysUntilDue(yesterday))
    }

    @Test
    fun daysFromNow_positiveDays() {
        val now = System.currentTimeMillis()
        val result = DateUtils.daysFromNow(7)
        val diff = result - now
        // Should be approximately 7 days (within 1 second tolerance)
        assertTrue(diff in 604799000L..604801000L)
    }

    @Test
    fun daysFromNow_negativeDays() {
        val now = System.currentTimeMillis()
        val result = DateUtils.daysFromNow(-1)
        assertTrue(result < now)
    }

    @Test
    fun dueDateStatus_overdueReturnsOverdue() {
        val pastDate = DateUtils.todayStart() - 3 * 86400000L
        val status = DateUtils.dueDateStatus(pastDate)
        assertTrue(status is DateUtils.DueDateStatus.Overdue)
        assertEquals(3L, (status as DateUtils.DueDateStatus.Overdue).days)
    }

    @Test
    fun dueDateStatus_todayReturnsDueToday() {
        val today = DateUtils.todayStart()
        val status = DateUtils.dueDateStatus(today)
        assertTrue(status is DateUtils.DueDateStatus.DueToday)
    }

    @Test
    fun dueDateStatus_withinWeekReturnsDueSoon() {
        val threeDaysLater = DateUtils.todayStart() + 3 * 86400000L
        val status = DateUtils.dueDateStatus(threeDaysLater)
        assertTrue(status is DateUtils.DueDateStatus.DueSoon)
        assertEquals(3L, (status as DateUtils.DueDateStatus.DueSoon).days)
    }

    @Test
    fun dueDateStatus_farFutureReturnsNormal() {
        val thirtyDaysLater = DateUtils.todayStart() + 30 * 86400000L
        val status = DateUtils.dueDateStatus(thirtyDaysLater)
        assertTrue(status is DateUtils.DueDateStatus.Normal)
    }

    @Test
    fun dueDateDescription_overdue() {
        val pastDate = DateUtils.todayStart() - 2 * 86400000L
        val desc = DateUtils.dueDateDescription(pastDate)
        assertEquals("已过期2天", desc)
    }

    @Test
    fun dueDateDescription_dueToday() {
        val today = DateUtils.todayStart()
        val desc = DateUtils.dueDateDescription(today)
        assertEquals("今天到期", desc)
    }

    @Test
    fun dueDateDescription_dueSoon() {
        val fiveDaysLater = DateUtils.todayStart() + 5 * 86400000L
        val desc = DateUtils.dueDateDescription(fiveDaysLater)
        assertEquals("5天后到期", desc)
    }
}
