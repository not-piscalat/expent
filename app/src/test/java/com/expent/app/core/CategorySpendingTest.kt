package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySpendingTest {

    private fun txc(
        categoryId: Long?,
        name: String?,
        amountCents: Long,
        type: TransactionType = TransactionType.EXPENSE,
        colorArgb: Long? = 0xFFFF7043L
    ) = TransactionWithCategory(
        transaction = TransactionEntity(
            amountCents = amountCents,
            type = type,
            categoryId = categoryId,
            note = null,
            timestamp = 0
        ),
        categoryName = name,
        categoryIconName = "Restaurant",
        categoryColorArgb = colorArgb
    )

    @Test
    fun `groups expenses by category and sums amounts`() {
        val items = listOf(txc(1L, "Food", 100), txc(1L, "Food", 250), txc(2L, "Transport", 75))
        val result = items.spendingByCategory()
        assertEquals(2, result.size)
        assertEquals(350L, result.first { it.categoryId == 1L }.amountCents)
        assertEquals(75L, result.first { it.categoryId == 2L }.amountCents)
    }

    @Test
    fun `sorts categories by amount descending`() {
        val items = listOf(txc(1L, "A", 100), txc(2L, "B", 500), txc(3L, "C", 300))
        assertEquals(listOf(2L, 3L, 1L), items.spendingByCategory().map { it.categoryId })
    }

    @Test
    fun `ignores income transactions`() {
        val items = listOf(
            txc(1L, "Food", 100),
            txc(1L, "Salary", 1000, type = TransactionType.INCOME)
        )
        assertEquals(1, items.spendingByCategory().size)
    }

    @Test
    fun `groups uncategorized expenses under one row`() {
        val items = listOf(txc(null, null, 50), txc(null, null, 30))
        val result = items.spendingByCategory()
        assertEquals(1, result.size)
        assertNull(result.first().name)
        assertEquals(80L, result.first().amountCents)
    }

    @Test
    fun `falls back to grey when color is missing`() {
        val result = listOf(txc(1L, "Food", 10, colorArgb = null)).spendingByCategory()
        assertEquals(0xFF9E9E9EL, result.first().colorArgb)
    }
}
