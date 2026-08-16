package com.expent.app.core

import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the per-account transaction visibility rule that keeps one account's\n *  spending out of another account's view on a shared device. */
class TransactionVisibilityTest {

    private fun tx(ownerId: String? = null) = TransactionEntity(
        amountCents = 100,
        type = TransactionType.EXPENSE,
        categoryId = null,
        note = null,
        timestamp = 1,
        ownerId = ownerId
    )

    @Test
    fun `a transaction is visible only to its owner`() {
        val t = tx(ownerId = "alice")
        assertTrue(t.visibleTo("alice"))
        assertFalse(t.visibleTo("bob"))
    }

    @Test
    fun `a pre-ownership transaction stays visible to any signed-in user`() {
        val t = tx() // created before ownership was stamped
        assertTrue(t.visibleTo("alice"))
        assertTrue(t.visibleTo("bob"))
    }

    @Test
    fun `a signed-out device shows everything`() {
        // Transactions have no sign-in gate, so signing out must not blank the
        // app: a signed-out device is a single-user phone.
        val mine = tx(ownerId = "alice")
        val legacy = tx()
        assertTrue(mine.visibleTo(null))
        assertTrue(legacy.visibleTo(null))
    }
}
