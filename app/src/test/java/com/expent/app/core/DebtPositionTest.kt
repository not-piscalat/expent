package com.expent.app.core

import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtType
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtPositionTest {

    private fun debt(
        id: Long,
        type: DebtType,
        amountCents: Long,
        paidCents: Long = 0
    ) = DebtWithPaid(
        debt = DebtEntity(
            id = id,
            title = "Debt $id",
            personName = null,
            type = type,
            amountCents = amountCents,
            note = null,
            dueTimestamp = null
        ),
        totalPaidCents = paidCents
    )

    @Test
    fun `sums active lent and borrowed separately`() {
        val items = listOf(
            debt(1, DebtType.LENT, 1_000, paidCents = 200),
            debt(2, DebtType.BORROWED, 500)
        )
        val position = items.debtPosition()
        assertEquals(800L, position.lentCents)
        assertEquals(500L, position.borrowedCents)
        assertEquals(300L, position.netCents)
    }

    @Test
    fun `ignores fully settled debts`() {
        val items = listOf(
            debt(1, DebtType.LENT, 500, paidCents = 500),
            debt(2, DebtType.BORROWED, 300, paidCents = 400)
        )
        val position = items.debtPosition()
        assertEquals(0L, position.lentCents)
        assertEquals(0L, position.borrowedCents)
        assertEquals(0L, position.netCents)
    }

    @Test
    fun `empty list yields a zero position`() {
        assertEquals(DebtPosition(), emptyList<DebtWithPaid>().debtPosition())
    }

    @Test
    fun `negative net when owing more than owed`() {
        val items = listOf(
            debt(1, DebtType.LENT, 200),
            debt(2, DebtType.BORROWED, 1_000)
        )
        assertEquals(-800L, items.debtPosition().netCents)
    }
}
