package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ForecastAccuracyTest {

    private val zone = ZoneId.systemDefault()

    private fun txn(amountCents: Long, type: TransactionType, year: Int, month: Int, day: Int = 15) =
        TransactionWithCategory(
            transaction = TransactionEntity(
                amountCents = amountCents,
                type = type,
                categoryId = 1L,
                note = null,
                timestamp = LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
            ),
            categoryName = "Cat",
            categoryIconName = null,
            categoryColorArgb = null
        )

    private fun template(type: TransactionType, amountCents: Long) = RecurringTemplateEntity(
        title = "t",
        amountCents = amountCents,
        type = type,
        categoryId = 1L,
        note = null,
        frequency = RecurringFrequency.MONTHLY,
        dayOfMonth = 1,
        dayOfWeek = 1,
        nextDueEpochDay = 0,
        isActive = true
    )

    /** Today is 2026-08-15, so the scored months are July, June, May 2026. */
    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun `perfect prediction scores zero deviation`() {
        val templates = listOf(template(TransactionType.INCOME, 30_000))
        // July actual matches the recurring prediction exactly.
        val past = listOf(txn(30_000, TransactionType.INCOME, 2026, 7))
        val result = forecastAccuracy(templates, past, emptyMap(), today)
        assertEquals(0, result.averageIncomeDeviationPct)
    }

    @Test
    fun `measures absolute deviation from actual`() {
        val templates = listOf(template(TransactionType.INCOME, 30_000))
        // July predicted 30,000 (recurring only); actual 36,000 -> 6,000/36,000 = 16.67% -> 16 (truncated).
        val past = listOf(txn(36_000, TransactionType.INCOME, 2026, 7))
        val result = forecastAccuracy(templates, past, emptyMap(), today)
        assertEquals(16, result.averageIncomeDeviationPct)
    }

    @Test
    fun `averages over several months`() {
        val templates = listOf(template(TransactionType.INCOME, 30_000))
        val past = listOf(
            txn(33_000, TransactionType.INCOME, 2026, 7), // 3,000/33,000 = 9.09% -> 9
            txn(36_000, TransactionType.INCOME, 2026, 6)  // 6,000/36,000 = 16.67% -> 16
        )
        val result = forecastAccuracy(templates, past, emptyMap(), today)
        // (9 + 16) / 2 = 12.5 -> rounds to 13
        assertEquals(13, result.averageIncomeDeviationPct)
    }

    @Test
    fun `months without actual activity are skipped per side`() {
        // No income at all in the window -> no income accuracy, but expenses still scored.
        val past = listOf(txn(10_000, TransactionType.EXPENSE, 2026, 7))
        val result = forecastAccuracy(emptyList(), past, emptyMap(), today)
        assertNull(result.averageIncomeDeviationPct)
        assertTrue(result.averageExpenseDeviationPct != null)
    }

    @Test
    fun `no data at all reports no accuracy`() {
        val result = forecastAccuracy(emptyList(), emptyList(), emptyMap(), today)
        assertFalse(result.hasData)
        assertNull(result.averageIncomeDeviationPct)
        assertNull(result.averageExpenseDeviationPct)
    }
}
