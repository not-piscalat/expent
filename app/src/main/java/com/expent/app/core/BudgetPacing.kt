package com.expent.app.core

import java.time.LocalDate

/**
 * Running-rate projection for one budgeted category: how the month will end
 * if spending continues at the current daily rate.
 */
data class BudgetPacing(
    val spentCents: Long,
    val budgetCents: Long,
    val projectedCents: Long
) {
    /** Already spent more than the budget. */
    val isOverBudget: Boolean get() = spentCents > budgetCents

    /** Projected to finish over budget, even if not over yet. */
    val isPacingOver: Boolean get() = projectedCents > budgetCents

    val projectedOverCents: Long get() = (projectedCents - budgetCents).coerceAtLeast(0)
}

/**
 * Projects month-end spend from the running rate: spent so far scaled by
 * (days in month / days elapsed). On the first of the month there is no track
 * record yet, so the projection equals the spend so far.
 */
fun budgetPacing(spentCents: Long, budgetCents: Long, today: LocalDate): BudgetPacing {
    val daysElapsed = (today.dayOfMonth - 1).coerceAtLeast(0)
    val daysInMonth = today.lengthOfMonth()
    val projected = if (daysElapsed == 0) {
        spentCents
    } else {
        spentCents * daysInMonth / daysElapsed
    }
    return BudgetPacing(spentCents = spentCents, budgetCents = budgetCents, projectedCents = projected)
}
