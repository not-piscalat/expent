package com.expent.app.core

import com.expent.app.data.local.entity.TransactionEntity

/**
 * Whether [myUid] may see this transaction on a shared device.
 *
 * Rules:
 *  - Signed out: the device is in single-user mode, show everything
 *    (transactions have no sign-in gate, so hiding them would blank the app).
 *  - Signed in: a transaction is visible when it is the user's own, or when
 *    it predates ownership tracking (ownerId null) — hiding those would
 *    delete the original owner's view of their own spending.
 */
fun TransactionEntity.visibleTo(myUid: String?): Boolean = visibleToOwner(ownerId, myUid)
