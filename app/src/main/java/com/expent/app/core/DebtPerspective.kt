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

    /**
     * Whether [myUid] may see this debt on a shared device. A user sees a debt
     * when they are a participant — the creator or the other side of a shared
     * record — or when they created an unshared one. Unshared debts created
     * before ownership was stamped (creatorId null, never shared) stay visible
     * to anyone on the device rather than vanishing for their original owner.
     */
    fun visibleTo(debt: DebtEntity, myUid: String?): Boolean {
        if (myUid == null) return false
        return debt.creatorId == myUid ||
            debt.otherParticipantId == myUid ||
            (debt.creatorId == null && debt.remoteId == null)
    }
}
