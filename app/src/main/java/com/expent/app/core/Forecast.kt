package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import java.time.LocalDate

/**
 * Projected next month, from three signals:
 *  - recurring templates (deterministic: each active template fires once per month,
 *    or once per weekday occurrence for weekly schedules);
 *  - a variable baseline: the average of the last three complete months, with each
 *    month's recurring contribution removed so nothing is double counted;
 *  - category budgets, shown as a comparison rather than a prediction.
 */
data class MonthlyForecast(
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    /** Sum of positive category budgets; null when no budgets are set. */
    val budgetedExpenseCents: Long? = null
) {
    val netCents: Long get() = incomeCents - expenseCents
    val hasForecast: Boolean get() = incomeCents > 0 || expenseCents > 0
}

fun forecast(
    templates: List<RecurringTemplateEntity>,
    pastTransactions: List<TransactionWithCategory>,
    budgets: Map<Long, Long?>,
    today: LocalDate
): MonthlyForecast {
    val active = templates.filter { it.isActive }
    val nextMonth = today.plusMonths(1).withDayOfMonth(1)

    val recurringIncome = active
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amountCents * occurrences(nextMonth, it).toLong() }
    val recurringExpense = active
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountCents * occurrences(nextMonth, it).toLong() }

    // The three complete months before the current one.
    val completeMonths = (1..3).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }

    fun variable(type: TransactionType): Long {
        val samples = completeMonths.map { month ->
            val total = pastTransactions
                .filter { it.transaction.type == type && inMonth(it, month) }
                .sumOf { it.transaction.amountCents }
            val recurringInMonth = active
                .filter { it.type == type }
                .sumOf { it.amountCents * occurrences(month, it).toLong() }
            total to recurringInMonth
        }
        // Only months where the user was active (had any transactions) count.
        val usable = samples.filter { (total, _) -> total > 0 }
        if (usable.isEmpty()) return 0
        return usable
            .map { (total, recurringInMonth) -> (total - recurringInMonth).coerceAtLeast(0) }
            .average()
            .toLong()
    }

    val budgeted = budgets.values
        .filterNotNull()
        .filter { it > 0 }
        .sum()
        .takeIf { it > 0 }

    return MonthlyForecast(
        incomeCents = recurringIncome + variable(TransactionType.INCOME),
        expenseCents = recurringExpense + variable(TransactionType.EXPENSE),
        budgetedExpenseCents = budgeted
    )
}

private fun inMonth(item: TransactionWithCategory, month: LocalDate): Boolean {
    val millis = item.transaction.timestamp
    val start = month.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    return millis >= start && millis < end
}

/** How many times a template fires in the given month. */
private fun occurrences(month: LocalDate, template: RecurringTemplateEntity): Int =
    when (template.frequency) {
        RecurringFrequency.MONTHLY -> 1
        RecurringFrequency.WEEKLY -> countWeekdays(month, template.dayOfWeek)
    }

/** Counts how many times a weekday (1=Mon..7=Sun) appears in a month. */
internal fun countWeekdays(monthStart: LocalDate, dayOfWeek: Int): Int {
    val first = monthStart.dayOfWeek.value
    val days = monthStart.lengthOfMonth()
    val offset = (dayOfWeek - first + 7) % 7
    return if (offset < days) 1 + (days - offset - 1) / 7 else 0
}
