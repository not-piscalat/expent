package com.expent.app.core

import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.RecurringTemplateEntity

/**
 * Per-account visibility for owner-stamped rows on a shared device:
 *  - Signed out: single-user mode, show everything.
 *  - Signed in: the row is visible when it belongs to the user, or when it
 *    predates ownership tracking (ownerId null) — hiding those would delete
 *    the original owner's view of their own data.
 */
fun visibleToOwner(ownerId: String?, myUid: String?): Boolean =
    myUid == null || ownerId == null || ownerId == myUid

fun CategoryEntity.visibleTo(myUid: String?): Boolean = visibleToOwner(ownerId, myUid)

fun RecurringTemplateEntity.visibleTo(myUid: String?): Boolean = visibleToOwner(ownerId, myUid)
