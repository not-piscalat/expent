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

class ForecastTest {

    /** The forecast's target month: September 2026 (today is 2026-08-15). */
    private val target = java.time.YearMonth.of(2026, 9)
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

    private fun template(
        type: TransactionType,
        amountCents: Long,
        frequency: RecurringFrequency,
        dayOfWeek: Int = 1,
        active: Boolean = true
    ) = RecurringTemplateEntity(
        title = "t",
        amountCents = amountCents,
        type = type,
        categoryId = 1L,
        note = null,
        frequency = frequency,
        dayOfMonth = 1,
        dayOfWeek = dayOfWeek,
        nextDueEpochDay = 0,
        isActive = active
    )

    @Test
    fun `forecast is zero with no templates and no history`() {
        val result = forecast(emptyList(), emptyList(), emptyMap(), target)
        assertFalse(result.hasForecast)
        assertEquals(0L, result.incomeCents)
        assertEquals(0L, result.expenseCents)
    }

    @Test
    fun `recurring monthly fires once and weekly counts weekday occurrences`() {
        val templates = listOf(
            template(TransactionType.INCOME, 30_000, RecurringFrequency.MONTHLY),
            template(TransactionType.EXPENSE, 1_000, RecurringFrequency.WEEKLY)
        )
        // Sep 2026 starts on a Tuesday -> 4 Mondays -> 4 weekly occurrences.
        val result = forecast(templates, emptyList(), emptyMap(), target)
        assertEquals(30_000L, result.incomeCents)
        assertEquals(4_000L, result.expenseCents)
        assertEquals(26_000L, result.netCents)
    }

    @Test
    fun `counts weekday occurrences per month`() {
        // Aug 2026 starts Saturday (31 days): 5 Mondays and 5 Sundays.
        assertEquals(5, countWeekdays(LocalDate.of(2026, 8, 1), 1))
        assertEquals(5, countWeekdays(LocalDate.of(2026, 8, 1), 7))
        // Oct 2026 starts Thursday (31 days): 4 Mondays.
        assertEquals(4, countWeekdays(LocalDate.of(2026, 10, 1), 1))
        // Jun 2026 starts Monday (30 days): 5 Mondays.
        assertEquals(5, countWeekdays(LocalDate.of(2026, 6, 1), 1))
    }

    @Test
    fun `variable baseline averages past months minus their recurring share`() {
        val templates = listOf(template(TransactionType.INCOME, 30_000, RecurringFrequency.MONTHLY))
        // July: 35,000 total income (30,000 recurring salary + 5,000 bonus).
        val past = listOf(txn(35_000, TransactionType.INCOME, 2026, 7))
        val result = forecast(templates, past, emptyMap(), target)
        // Next month recurring 30,000 + variable 5,000.
        assertEquals(35_000L, result.incomeCents)
    }

    @Test
    fun `variable baseline clamps at zero when recurring estimate exceeds history`() {
        val templates = listOf(template(TransactionType.EXPENSE, 1_000, RecurringFrequency.WEEKLY))
        // May 2026 has 4 Mondays -> recurring estimate 4,000, but only 3,000 was actually spent.
        val past = listOf(txn(3_000, TransactionType.EXPENSE, 2026, 5))
        val result = forecast(templates, past, emptyMap(), target)
        assertEquals(4_000L, result.expenseCents) // recurring only; variable clamped to 0
    }

    @Test
    fun `inactive templates are excluded`() {
        val templates = listOf(
            template(TransactionType.INCOME, 30_000, RecurringFrequency.MONTHLY, active = false)
        )
        val result = forecast(templates, emptyList(), emptyMap(), target)
        assertFalse(result.hasForecast)
        assertEquals(0L, result.incomeCents)
    }

    @Test
    fun `sums positive budgets and reports null when none set`() {
        val templates = listOf(template(TransactionType.EXPENSE, 1_000, RecurringFrequency.MONTHLY))
        val withBudgets = forecast(
            templates, emptyList(), mapOf(1L to 5_000, 2L to 3_000, 3L to -1, 4L to null), target
        )
        assertEquals(8_000L, withBudgets.budgetedExpenseCents)

        val without = forecast(templates, emptyList(), emptyMap(), target)
        assertNull(without.budgetedExpenseCents)
    }

    @Test
    fun `months without any transactions do not drag the average down`() {
        val templates = listOf(template(TransactionType.INCOME, 30_000, RecurringFrequency.MONTHLY))
        // Only July had data (35,000 total). June/May are empty and must not count as 0.
        val past = listOf(txn(35_000, TransactionType.INCOME, 2026, 7))
        val result = forecast(templates, past, emptyMap(), target)
        assertEquals(35_000L, result.incomeCents)
        assertTrue(result.hasForecast)
    }
}
