package com.expent.app.core.util

import org.junit.Assert.assertEquals
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
}
