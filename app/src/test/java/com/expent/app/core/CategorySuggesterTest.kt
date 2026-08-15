package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySuggesterTest {

    private fun category(id: Long, name: String, type: TransactionType = TransactionType.EXPENSE) =
        CategoryEntity(id = id, name = name, type = type, iconName = null, colorArgb = 0xFF000000)

    private fun history(note: String, categoryId: Long, type: TransactionType = TransactionType.EXPENSE) =
        TransactionWithCategory(
            transaction = TransactionEntity(
                amountCents = 100,
                type = type,
                categoryId = categoryId,
                note = note,
                timestamp = 1
            ),
            categoryName = null,
            categoryIconName = null,
            categoryColorArgb = null
        )

    private val food = category(1, "Food")
    private val utilities = category(2, "Utilities")
    private val transport = category(3, "Transport")
    private val entertainment = category(4, "Entertainment")
    private val coffee = category(5, "Coffee")
    private val salary = category(6, "Salary", TransactionType.INCOME)

    @Test
    fun `blank note suggests nothing`() {
        assertTrue(CategorySuggester.suggest("  ", emptyList(), listOf(food), TransactionType.EXPENSE).isEmpty())
    }

    @Test
    fun `learns from the user's own history`() {
        val past = listOf(history("meralco bill", categoryId = 2))
        val result = CategorySuggester.suggest("meralco", past, listOf(food, utilities), TransactionType.EXPENSE)
        assertEquals(listOf(2L), result.map { it.category.id })
    }

    @Test
    fun `built-in keywords match against category names without history`() {
        val result = CategorySuggester.suggest("grab", emptyList(), listOf(food, transport), TransactionType.EXPENSE)
        assertEquals(listOf(3L), result.map { it.category.id })
    }

    @Test
    fun `note naming a category directly is the strongest match`() {
        val result = CategorySuggester.suggest(
            "coffee", emptyList(), listOf(food, transport, coffee), TransactionType.EXPENSE
        )
        // Coffee (+3 name token, +2 keyword) ranks above Food (+2 keyword hint).
        assertEquals(listOf(5L, 1L), result.map { it.category.id })
    }

    @Test
    fun `income categories are never suggested for expenses`() {
        val result = CategorySuggester.suggest(
            "salary", emptyList(), listOf(food, salary), TransactionType.EXPENSE
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `exact note history outranks a plain keyword match`() {
        val past = listOf(history("netflix", categoryId = 4))
        // Keyword hints put "Entertainment" at +2; history adds the +5 exact-note boost.
        val result = CategorySuggester.suggest("netflix", past, listOf(transport, entertainment), TransactionType.EXPENSE)
        assertEquals(listOf(4L), result.map { it.category.id })
    }

    @Test
    fun `respects the limit`() {
        // "coffee" matches "Coffee" (+3) and "Food" via keyword hints (+2).
        val result = CategorySuggester.suggest(
            "coffee", emptyList(), listOf(coffee, food, transport), TransactionType.EXPENSE, limit = 1
        )
        assertEquals(1, result.size)
        assertEquals(5L, result.single().category.id)
    }

    @Test
    fun `nothing matches when there is no signal`() {
        val result = CategorySuggester.suggest("xyzzy", emptyList(), listOf(food, transport), TransactionType.EXPENSE)
        assertTrue(result.isEmpty())
    }
}
