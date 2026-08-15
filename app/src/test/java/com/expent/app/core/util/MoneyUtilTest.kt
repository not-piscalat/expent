package com.expent.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyUtilTest {

    @Test
    fun `formats cents with thousands separator and two decimals`() {
        assertEquals("1,234.56", MoneyUtil.format(123_456, Locale.US))
    }

    @Test
    fun `formats zero`() {
        assertEquals("0.00", MoneyUtil.format(0, Locale.US))
    }

    @Test
    fun `formats small amounts`() {
        assertEquals("0.99", MoneyUtil.format(99, Locale.US))
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("-12.50", MoneyUtil.format(-1_250, Locale.US))
    }

    @Test
    fun `parses whole amounts to cents`() {
        assertEquals(123_456L, MoneyUtil.parse("1234.56"))
        assertEquals(500L, MoneyUtil.parse("5"))
        assertEquals(5L, MoneyUtil.parse("0.05"))
    }

    @Test
    fun `parses amounts with thousands separators`() {
        assertEquals(123_456L, MoneyUtil.parse("1,234.56"))
        assertEquals(123_400L, MoneyUtil.parse("1,234"))
    }

    @Test
    fun `parses negative amounts`() {
        assertEquals(-1_250L, MoneyUtil.parse("-12.50"))
    }

    @Test
    fun `rejects invalid amounts`() {
        assertNull(MoneyUtil.parse(""))
        assertNull(MoneyUtil.parse("abc"))
        assertNull(MoneyUtil.parse("1.234"))
        assertNull(MoneyUtil.parse("1.2.3"))
    }

    @Test
    fun `rounds single digit fractions`() {
        assertEquals(10L, MoneyUtil.parse("0.1"))
        assertEquals(1L, MoneyUtil.parse("0.01"))
    }
}
