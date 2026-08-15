package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import java.time.LocalDate

enum class InsightKind { UNUSUAL_EXPENSE, DUPLICATE, MISSED_RECURRING }

/** One explainable flag, ready to render. Text lives in the UI layer. */
data class Insight(
    val kind: InsightKind,
    val amountCents: Long,
    val categoryName: String? = null,
    val note: String? = null,
    /** Epoch day of the involved transaction or due occurrence. */
    val dateEpochDay: Long? = null,
    val title: String? = null,
    /** The transaction this insight points at, for UNUSUAL_EXPENSE and DUPLICATE. */
    val transactionId: Long? = null,
    /** The recurring template behind a MISSED_RECURRING insight. */
    val templateId: Long? = null
) {
    /** Stable identity for dismissal; a missed flag returns next month with a new key. */
    val dismissKey: String = when (kind) {
        InsightKind.UNUSUAL_EXPENSE, InsightKind.DUPLICATE -> "txn:${transactionId ?: 0}"
        InsightKind.MISSED_RECURRING -> "tpl:${templateId ?: 0}:${dateEpochDay ?: 0}"
    }
}

/** Duplicates are flagged when the same amount and note recur within this many days. */
private const val DUPLICATE_WINDOW_DAYS = 3L

/** A transaction is unusual when it exceeds this multiple of its category's median. */
private const val UNUSUAL_MULTIPLE = 3

/**
 * Flags worth surfacing on Home:
 *  - expenses far larger than the category's typical transaction (median-based,
 *    so a few big bills don't distort the baseline);
 *  - likely duplicate entries (same amount + note within a few days);
 *  - active recurring occurrences whose due date has passed without being logged.
 *
 * Ordered by time-sensitivity: missed occurrences first, then unusual, then duplicates.
 */
fun insights(
    transactions: List<TransactionWithCategory>,
    templates: List<RecurringTemplateEntity>,
    today: LocalDate
): List<Insight> {
    val result = mutableListOf<Insight>()

    // Missed recurring occurrences (active templates whose next due date has passed).
    templates
        .filter { it.isActive && LocalDate.ofEpochDay(it.nextDueEpochDay) <= today }
        .forEach { template ->
            result += Insight(
                kind = InsightKind.MISSED_RECURRING,
                amountCents = template.amountCents,
                dateEpochDay = template.nextDueEpochDay,
                title = template.title,
                templateId = template.id
            )
        }

    // Unusually large expenses, judged against the category's own median.
    val expenses = transactions.filter { it.transaction.type == TransactionType.EXPENSE }
    expenses
        .groupBy { it.transaction.categoryId }
        .forEach { (_, items) ->
            val median = medianOf(items.map { it.transaction.amountCents }) ?: return@forEach
            items.forEach { item ->
                if (item.transaction.amountCents > UNUSUAL_MULTIPLE * median) {
                    result += Insight(
                        kind = InsightKind.UNUSUAL_EXPENSE,
                        amountCents = item.transaction.amountCents,
                        categoryName = item.categoryName,
                        dateEpochDay = epochDayOf(item.transaction.timestamp),
                        transactionId = item.transaction.id
                    )
                }
            }
        }

    // Likely duplicates: same type, amount, and written note within a few days of each
    // other. Transactions without a note are ignored (two same-priced rides are common).
    transactions
        .filter { !it.transaction.note.isNullOrBlank() }
        .groupBy {
            Triple(
                it.transaction.type,
                it.transaction.amountCents,
                it.transaction.note.orEmpty().trim().lowercase()
            )
        }
        .values
        .forEach { group ->
            if (group.size < 2) return@forEach
            val sorted = group.sortedBy { it.transaction.timestamp }
            for (i in 0 until sorted.size - 1) {
                val a = epochDayOf(sorted[i].transaction.timestamp)
                val b = epochDayOf(sorted[i + 1].transaction.timestamp)
                if (b - a <= DUPLICATE_WINDOW_DAYS) {
                    result += Insight(
                        kind = InsightKind.DUPLICATE,
                        amountCents = sorted[i].transaction.amountCents,
                        note = sorted[i].transaction.note,
                        dateEpochDay = a,
                        transactionId = sorted[i].transaction.id
                    )
                    break // one flag per group
                }
            }
        }

    return result
}

/** Median of a non-empty list; null when empty. */
internal fun medianOf(values: List<Long>): Long? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) {
        sorted[middle]
    } else {
        (sorted[middle - 1] + sorted[middle]) / 2
    }
}

private fun epochDayOf(epochMillis: Long): Long =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toEpochDay()
