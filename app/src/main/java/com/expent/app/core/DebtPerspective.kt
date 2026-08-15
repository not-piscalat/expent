package com.expent.app.core

import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtType

/**
 * Shared debts are stored canonically from the creator's perspective: a record
 * with type LENT always means "the creator lent to the other participant". The
 * counterparty must see the debt from their own side — "Borrowed from X"
 * instead of "Lent to X" — so their UI flips the direction.
 *
 * Local-only debts (never shared) are always shown exactly as stored, since
 * their creator is the only viewer.
 */
object DebtPerspective {

    /** The debt type as [myUid] experiences it; null means the user is signed out. */
    fun displayedType(debt: DebtEntity, myUid: String?): DebtType {
        if (debt.remoteId == null || myUid == null || debt.creatorId == myUid) return debt.type
        return when (debt.type) {
            DebtType.LENT -> DebtType.BORROWED
            DebtType.BORROWED -> DebtType.LENT
        }
    }
}
