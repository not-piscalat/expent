package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionType

/** One row of the monthly breakdown: a category and its total spent. */
data class CategorySpending(
    val categoryId: Long?,
    val name: String?,
    val iconName: String?,
    val colorArgb: Long,
    val amountCents: Long,
    /** Optional monthly spending limit; null means no budget. */
    val budgetCents: Long? = null
)

/** Groups expenses by category, summing amounts and sorting by total descending. */
fun List<TransactionWithCategory>.spendingByCategory(): List<CategorySpending> =
    filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.transaction.categoryId }
        .map { (categoryId, items) ->
            CategorySpending(
                categoryId = categoryId,
                name = items.firstNotNullOfOrNull { it.categoryName },
                iconName = items.firstNotNullOfOrNull { it.categoryIconName },
                colorArgb = items.firstNotNullOfOrNull { it.categoryColorArgb } ?: 0xFF9E9E9E,
                amountCents = items.sumOf { it.transaction.amountCents }
            )
        }
        .sortedByDescending { it.amountCents }

/** Attaches each category's monthly budget to its breakdown row. */
fun List<CategorySpending>.withBudgets(budgetByCategoryId: Map<Long, Long?>): List<CategorySpending> =
    map { it.copy(budgetCents = it.categoryId?.let { id -> budgetByCategoryId[id] }) }
