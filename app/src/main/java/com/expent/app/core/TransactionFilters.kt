package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A selectable month: label plus the exact millisecond range it covers. */
data class MonthOption(
    val label: String,
    val startMillis: Long,
    val endMillis: Long
)

/** Pure month-window and text-search filtering for the transactions list. */
object TransactionFilters {

    /** Builds the last [count] months (including the current one), newest first. */
    fun recentMonths(count: Int = 12, now: LocalDate = LocalDate.now()): List<MonthOption> {
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
        val current = now.withDayOfMonth(1)
        return (0 until count).map { offset ->
            val startDate = current.minusMonths(offset.toLong())
            val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = startDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            MonthOption(startDate.format(formatter), start, end)
        }
    }

    /**
     * Filters transactions by an optional month window (half-open
     * [start, end)) and a case-insensitive search across category names
     * and notes. With no filters, returns the input unchanged.
     */
    fun filter(
        transactions: List<TransactionWithCategory>,
        monthStart: Long?,
        monthEnd: Long?,
        query: String
    ): List<TransactionWithCategory> {
        if (monthStart == null && query.isBlank()) return transactions
        val effectiveStart = monthStart ?: Long.MIN_VALUE
        val q = query.trim()
        return transactions.filter { tx ->
            val inMonth = monthEnd == null ||
                (tx.transaction.timestamp >= effectiveStart && tx.transaction.timestamp < monthEnd)
            val matchesQuery = q.isEmpty() ||
                tx.categoryName?.contains(q, ignoreCase = true) == true ||
                tx.transaction.note?.contains(q, ignoreCase = true) == true
            inMonth && matchesQuery
        }
    }
}
