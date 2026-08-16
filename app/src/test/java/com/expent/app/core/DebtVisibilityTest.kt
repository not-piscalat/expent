package com.expent.app.core

import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtStatus
import com.expent.app.data.local.entity.DebtType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the per-account debt visibility rule that keeps one account's\n *  debts out of another account's list on a shared device. */
class DebtVisibilityTest {

    private fun debt(
        remoteId: String? = null,
        creatorId: String? = null,
        otherParticipantId: String? = null
    ) = DebtEntity(
        title = "T",
        personName = null,
        type = DebtType.LENT,
        amountCents = 100,
        note = null,
        dueTimestamp = null,
        remoteId = remoteId,
        creatorId = creatorId,
        otherParticipantId = otherParticipantId,
        status = DebtStatus.OPEN
    )

    @Test
    fun `a local debt is visible only to its creator`() {
        val d = debt(creatorId = "alice")
        assertTrue(DebtPerspective.visibleTo(d, "alice"))
        assertFalse(DebtPerspective.visibleTo(d, "bob"))
        assertFalse(DebtPerspective.visibleTo(d, null))
    }

    @Test
    fun `a shared debt is visible to both participants but not strangers`() {
        val d = debt(remoteId = "doc", creatorId = "alice", otherParticipantId = "bob")
        assertTrue(DebtPerspective.visibleTo(d, "alice"))
        assertTrue(DebtPerspective.visibleTo(d, "bob"))
        assertFalse(DebtPerspective.visibleTo(d, "carol"))
    }

    @Test
    fun `a pre-ownership local debt stays visible rather than vanishing`() {
        // Created before debts were stamped with their owner: no creator, never
        // shared. Hiding it would delete its original owner's view, so it is
        // shown to whoever signs in.
        val d = debt()
        assertTrue(DebtPerspective.visibleTo(d, "alice"))
        assertTrue(DebtPerspective.visibleTo(d, "bob"))
    }

    @Test
    fun `a signed-out user sees nothing`() {
        val d = debt(remoteId = "doc", creatorId = "alice", otherParticipantId = "bob")
        assertFalse(DebtPerspective.visibleTo(d, null))
    }
}
