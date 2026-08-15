package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightsTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 8, 15)

    private fun txn(
        amountCents: Long,
        type: TransactionType = TransactionType.EXPENSE,
        note: String? = null,
        categoryId: Long = 1L,
        categoryName: String = "Food",
        day: Int
    ) = TransactionWithCategory(
        transaction = TransactionEntity(
            amountCents = amountCents,
            type = type,
            categoryId = categoryId,
            note = note,
            timestamp = LocalDate.of(2026, 8, day).atStartOfDay(zone).toInstant().toEpochMilli()
        ),
        categoryName = categoryName,
        categoryIconName = null,
        categoryColorArgb = null
    )

    private fun template(
        amountCents: Long,
        type: TransactionType = TransactionType.INCOME,
        dueDay: Int,
        active: Boolean = true
    ) = RecurringTemplateEntity(
        title = if (type == TransactionType.INCOME) "Salary" else "Rent",
        amountCents = amountCents,
        type = type,
        categoryId = 1L,
        note = null,
        frequency = RecurringFrequency.MONTHLY,
        dayOfMonth = 1,
        dayOfWeek = 1,
        nextDueEpochDay = LocalDate.of(2026, 8, dueDay).toEpochDay(),
        isActive = active
    )

    // --- unusual expenses ---

    @Test
    fun `flags an expense far above its category median`() {
        val list = listOf(
            txn(100, day = 1), txn(150, day = 2), txn(120, day = 3), txn(15_000, day = 10)
        )
        val result = insights(list, emptyList(), today)
        val unusual = result.filter { it.kind == InsightKind.UNUSUAL_EXPENSE }
        assertEquals(1, unusual.size)
        assertEquals(15_000L, unusual.single().amountCents)
        assertEquals("Food", unusual.single().categoryName)
    }

    @Test
    fun `does not flag when the category has no track record`() {
        val list = listOf(txn(15_000, day = 10))
        val result = insights(list, emptyList(), today)
        assertTrue(result.none { it.kind == InsightKind.UNUSUAL_EXPENSE })
    }

    @Test
    fun `ignores income for the unusual rule`() {
        val list = listOf(
            txn(1_000, day = 1),
            txn(50_000, type = TransactionType.INCOME, day = 5)
        )
        val result = insights(list, emptyList(), today)
        assertTrue(result.none { it.kind == InsightKind.UNUSUAL_EXPENSE })
    }

    // --- duplicates ---

    @Test
    fun `flags same amount and note within the window`() {
        val list = listOf(
            txn(1_200, note = "Grab", day = 3),
            txn(1_200, note = "grab", day = 5) // case-insensitive
        )
        val result = insights(list, emptyList(), today)
        val dup = result.filter { it.kind == InsightKind.DUPLICATE }
        assertEquals(1, dup.size)
        assertEquals(1_200L, dup.single().amountCents)
    }

    @Test
    fun `does not flag same amount and note far apart`() {
        val list = listOf(
            txn(1_200, note = "Rent", day = 1),
            txn(1_200, note = "Rent", day = 20)
        )
        assertTrue(insights(list, emptyList(), today).none { it.kind == InsightKind.DUPLICATE })
    }

    @Test
    fun `does not flag blank-note pairs`() {
        val list = listOf(
            txn(150, note = null, day = 3),
            txn(150, note = null, day = 4)
        )
        assertTrue(insights(list, emptyList(), today).none { it.kind == InsightKind.DUPLICATE })
    }

    @Test
    fun `does not flag same amount different note`() {
        val list = listOf(
            txn(1_200, note = "Grab", day = 3),
            txn(1_200, note = "Lunch", day = 4)
        )
        assertTrue(insights(list, emptyList(), today).none { it.kind == InsightKind.DUPLICATE })
    }

    // --- missed recurring ---

    @Test
    fun `flags an active template whose due date has passed`() {
        val result = insights(emptyList(), listOf(template(30_000, dueDay = 10)), today)
        val missed = result.filter { it.kind == InsightKind.MISSED_RECURRING }
        assertEquals(1, missed.size)
        assertEquals("Salary", missed.single().title)
        assertEquals(LocalDate.of(2026, 8, 10).toEpochDay(), missed.single().dateEpochDay)
    }

    @Test
    fun `does not flag templates not yet due or paused`() {
        val future = template(30_000, dueDay = 20)
        val paused = template(30_000, dueDay = 10, active = false)
        assertTrue(insights(emptyList(), listOf(future), today).isEmpty())
        assertTrue(insights(emptyList(), listOf(paused), today).isEmpty())
    }

    @Test
    fun `orders insights with missed occurrences first`() {
        val templates = listOf(template(30_000, dueDay = 10))
        val list = listOf(
            txn(100, day = 1), txn(150, day = 2), txn(15_000, day = 10),
            txn(1_200, note = "Grab", day = 3), txn(1_200, note = "Grab", day = 4)
        )
        val result = insights(list, templates, today)
        assertEquals(InsightKind.MISSED_RECURRING, result.first().kind)
        assertEquals(3, result.size)
    }
}
