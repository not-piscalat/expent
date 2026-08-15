package com.expent.app.core

import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtType

/** Current outstanding positions for active (not fully paid) debts. */
data class DebtPosition(
    val lentCents: Long = 0,
    val borrowedCents: Long = 0
) {
    /** Positive means you're owed more than you owe. */
    val netCents: Long get() = lentCents - borrowedCents
}

/** Sums remaining balances of active debts, split by lent/borrowed type. */
fun List<DebtWithPaid>.debtPosition(): DebtPosition =
    fold(DebtPosition()) { acc, item ->
        val remaining = item.debt.amountCents - item.totalPaidCents
        if (remaining <= 0) {
            acc
        } else {
            when (item.debt.type) {
                DebtType.LENT -> acc.copy(lentCents = acc.lentCents + remaining)
                DebtType.BORROWED -> acc.copy(borrowedCents = acc.borrowedCents + remaining)
            }
        }
    }
