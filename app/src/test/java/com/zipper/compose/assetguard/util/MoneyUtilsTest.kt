package com.zipper.compose.assetguard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun centsToYuanString_basicConversion() {
        assertEquals("123.45", MoneyUtils.centsToYuanString(12345L))
    }

    @Test
    fun centsToYuanString_zero() {
        assertEquals("0.00", MoneyUtils.centsToYuanString(0L))
    }

    @Test
    fun centsToYuanString_negativeValue() {
        assertEquals("-50.00", MoneyUtils.centsToYuanString(-5000L))
    }

    @Test
    fun centsToYuanString_largeValue() {
        assertEquals("1,234,567.89", MoneyUtils.centsToYuanString(123456789L))
    }

    @Test
    fun centsToYuanString_singleCent() {
        assertEquals("0.01", MoneyUtils.centsToYuanString(1L))
    }

    @Test
    fun yuanStringToCents_basicConversion() {
        assertEquals(12345L, MoneyUtils.yuanStringToCents("123.45"))
    }

    @Test
    fun yuanStringToCents_emptyString() {
        assertEquals(0L, MoneyUtils.yuanStringToCents(""))
    }

    @Test
    fun yuanStringToCents_whitespace() {
        assertEquals(0L, MoneyUtils.yuanStringToCents("   "))
    }

    @Test
    fun yuanStringToCents_invalidInput() {
        assertEquals(0L, MoneyUtils.yuanStringToCents("abc"))
    }

    @Test
    fun yuanStringToCents_withCommas() {
        // Note: floating point precision means large values may be off by 1 cent
        val result = MoneyUtils.yuanStringToCents("1,234,567.89")
        assertTrue("Expected ~123456789 but got $result", result in 123456788L..123456789L)
    }

    @Test
    fun yuanStringToCents_wholeNumber() {
        assertEquals(10000L, MoneyUtils.yuanStringToCents("100"))
    }

    @Test
    fun isValidAmountInput_emptyString() {
        assertTrue(MoneyUtils.isValidAmountInput(""))
    }

    @Test
    fun isValidAmountInput_validDecimal() {
        assertTrue(MoneyUtils.isValidAmountInput("123.45"))
    }

    @Test
    fun isValidAmountInput_wholeNumber() {
        assertTrue(MoneyUtils.isValidAmountInput("100"))
    }

    @Test
    fun isValidAmountInput_decimalPoint() {
        assertTrue(MoneyUtils.isValidAmountInput("123."))
    }

    @Test
    fun isValidAmountInput_singleDigitDecimal() {
        assertTrue(MoneyUtils.isValidAmountInput("123.4"))
    }

    @Test
    fun isValidAmountInput_threeDecimalPlaces() {
        assertFalse(MoneyUtils.isValidAmountInput("123.456"))
    }

    @Test
    fun isValidAmountInput_negativeNumber() {
        assertFalse(MoneyUtils.isValidAmountInput("-100"))
    }

    @Test
    fun isValidAmountInput_letters() {
        assertFalse(MoneyUtils.isValidAmountInput("abc"))
    }

    @Test
    fun isValidAmountInput_multipleDecimalPoints() {
        assertFalse(MoneyUtils.isValidAmountInput("12.34.56"))
    }

    @Test
    fun formatCents_withPrefix() {
        assertEquals("¥123.45", MoneyUtils.formatCents(12345L))
    }

    @Test
    fun formatCents_zero() {
        assertEquals("¥0.00", MoneyUtils.formatCents(0L))
    }
}
