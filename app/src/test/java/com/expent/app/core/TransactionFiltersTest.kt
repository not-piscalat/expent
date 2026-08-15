package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TransactionFiltersTest {

    private fun tx(
        id: Long,
        amountCents: Long = 100,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: Long? = 1,
        note: String? = null,
        timestamp: Long = 0
    ) = TransactionWithCategory(
        transaction = TransactionEntity(
            id = id,
            amountCents = amountCents,
            type = type,
            categoryId = categoryId,
            note = note,
            timestamp = timestamp
        ),
        categoryName = if (categoryId == 1L) "Food" else null,
        categoryIconName = null,
        categoryColorArgb = null
    )

    private fun monthStart(year: Int, month: Int): Long =
        LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `returns everything when no filters are applied`() {
        val items = listOf(tx(1), tx(2), tx(3))
        assertEquals(items, TransactionFilters.filter(items, null, null, ""))
        assertEquals(items, TransactionFilters.filter(items, null, null, "   "))
    }

    @Test
    fun `filters by month window`() {
        val jan = monthStart(2026, 1)
        val feb = monthStart(2026, 2)
        val mar = monthStart(2026, 3)
        val items = listOf(
            tx(1, timestamp = jan + 1000),
            tx(2, timestamp = feb + 1000),
            tx(3, timestamp = mar + 1000)
        )
        val result = TransactionFilters.filter(items, feb, mar, "")
        assertEquals(listOf(2L), result.map { it.transaction.id })
    }

    @Test
    fun `matches category names case-insensitively`() {
        val items = listOf(
            tx(1, categoryId = 1, note = "Lunch"),
            tx(2, categoryId = null, note = "Bus")
        )
        val result = TransactionFilters.filter(items, null, null, "FOOD")
        assertEquals(listOf(1L), result.map { it.transaction.id })
    }

    @Test
    fun `matches notes`() {
        val items = listOf(tx(1, categoryId = null, note = "Lunch with Mom"))
        val result = TransactionFilters.filter(items, null, null, "mom")
        assertEquals(listOf(1L), result.map { it.transaction.id })
    }

    @Test
    fun `combines month and query filters`() {
        val jan = monthStart(2026, 1)
        val feb = monthStart(2026, 2)
        val items = listOf(
            tx(1, note = "Food", timestamp = jan + 1),
            tx(2, note = "Food", timestamp = feb + 1),
            tx(3, categoryId = null, note = "Other", timestamp = jan + 1)
        )
        val result = TransactionFilters.filter(items, jan, feb, "food")
        assertEquals(listOf(1L), result.map { it.transaction.id })
    }

    @Test
    fun `recentMonths produces contiguous descending months`() {
        val months = TransactionFilters.recentMonths(count = 3, now = LocalDate.of(2026, 8, 15))
        assertEquals(3, months.size)
        assertEquals(listOf("Aug 2026", "Jul 2026", "Jun 2026"), months.map { it.label })
        for (i in 0 until months.size - 1) {
            assertEquals("months must be contiguous", months[i].startMillis, months[i + 1].endMillis)
        }
    }

    @Test
    fun `recentMonths starts at the first day of the current month`() {
        val now = LocalDate.of(2026, 8, 15)
        val augStart = now.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(augStart, TransactionFilters.recentMonths(count = 1, now = now).first().startMillis)
    }
}
