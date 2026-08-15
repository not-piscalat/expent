package com.expent.app.core

import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtType
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtPerspectiveTest {

    private fun debt(type: DebtType, remoteId: String? = "doc", creatorId: String? = "creator") =
        DebtEntity(
            id = 1,
            title = "t",
            personName = null,
            type = type,
            amountCents = 100,
            note = null,
            dueTimestamp = null,
            createdAt = 0,
            remoteId = remoteId,
            creatorId = creatorId,
            otherParticipantId = "other"
        )

    @Test
    fun `local debts are shown as stored regardless of user`() {
        assertEquals(DebtType.LENT, DebtPerspective.displayedType(debt(DebtType.LENT, remoteId = null), "anyone"))
        assertEquals(DebtType.BORROWED, DebtPerspective.displayedType(debt(DebtType.BORROWED, remoteId = null), "anyone"))
    }

    @Test
    fun `creator sees the canonical direction`() {
        assertEquals(DebtType.LENT, DebtPerspective.displayedType(debt(DebtType.LENT), "creator"))
        assertEquals(DebtType.BORROWED, DebtPerspective.displayedType(debt(DebtType.BORROWED), "creator"))
    }

    @Test
    fun `counterparty sees the flipped direction`() {
        assertEquals(DebtType.BORROWED, DebtPerspective.displayedType(debt(DebtType.LENT), "other"))
        assertEquals(DebtType.LENT, DebtPerspective.displayedType(debt(DebtType.BORROWED), "other"))
    }

    @Test
    fun `signed-out users see the canonical direction`() {
        assertEquals(DebtType.LENT, DebtPerspective.displayedType(debt(DebtType.LENT), null))
    }
}
