package com.zipper.compose.assetguard.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class LoanStatusTest {

    @Test
    fun fromRepaid_zeroRepaidReturnsUnpaid() {
        assertEquals(LoanStatus.UNPAID, LoanStatus.fromRepaid(0L, 10000L))
    }

    @Test
    fun fromRepaid_partialRepaidReturnsPartial() {
        assertEquals(LoanStatus.PARTIAL, LoanStatus.fromRepaid(5000L, 10000L))
    }

    @Test
    fun fromRepaid_fullRepaidReturnsPaid() {
        assertEquals(LoanStatus.PAID, LoanStatus.fromRepaid(10000L, 10000L))
    }

    @Test
    fun fromRepaid_overRepaidReturnsPaid() {
        assertEquals(LoanStatus.PAID, LoanStatus.fromRepaid(15000L, 10000L))
    }

    @Test
    fun fromRepaid_singleCentRepaidReturnsPartial() {
        assertEquals(LoanStatus.PARTIAL, LoanStatus.fromRepaid(1L, 10000L))
    }

    @Test
    fun toDisplayString_unpaid() {
        assertEquals("未还", LoanStatus.toDisplayString(LoanStatus.UNPAID))
    }

    @Test
    fun toDisplayString_partial() {
        assertEquals("部分归还", LoanStatus.toDisplayString(LoanStatus.PARTIAL))
    }

    @Test
    fun toDisplayString_paid() {
        assertEquals("已还清", LoanStatus.toDisplayString(LoanStatus.PAID))
    }

    @Test
    fun toDisplayString_overdue() {
        assertEquals("逾期", LoanStatus.toDisplayString(LoanStatus.OVERDUE))
    }

    @Test
    fun toDisplayString_disputed() {
        assertEquals("争议中", LoanStatus.toDisplayString(LoanStatus.DISPUTED))
    }

    @Test
    fun toDisplayString_badDebt() {
        assertEquals("坏账", LoanStatus.toDisplayString(LoanStatus.BAD_DEBT))
    }

    @Test
    fun toDisplayString_archived() {
        assertEquals("已归档", LoanStatus.toDisplayString(LoanStatus.ARCHIVED))
    }

    @Test
    fun toDisplayString_unknownStatus() {
        assertEquals("未知", LoanStatus.toDisplayString(99))
    }

    @Test
    fun excludedFromActive_containsPaidAndArchived() {
        assertEquals(setOf(LoanStatus.PAID, LoanStatus.ARCHIVED), LoanStatus.EXCLUDED_FROM_ACTIVE)
    }
}
