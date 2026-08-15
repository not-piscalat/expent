package com.expent.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetPacingTest {

    @Test
    fun `first of the month projects exactly the spend so far`() {
        val today = LocalDate.of(2026, 8, 1)
        val pacing = budgetPacing(spentCents = 500, budgetCents = 5_000, today = today)
        assertEquals(500L, pacing.projectedCents)
        assertFalse(pacing.isOverBudget)
        assertFalse(pacing.isPacingOver)
    }

    @Test
    fun `mid-month projects the running rate forward`() {
        val today = LocalDate.of(2026, 8, 15) // 31-day month, 14 days elapsed
        val pacing = budgetPacing(spentCents = 4_000, budgetCents = 5_000, today = today)
        // 4000 * 31 / 14 = 8857 (integer division)
        assertEquals(8_857L, pacing.projectedCents)
        assertFalse(pacing.isOverBudget)
        assertTrue(pacing.isPacingOver)
        assertEquals(3_857L, pacing.projectedOverCents)
    }

    @Test
    fun `flags on track when the projection stays under budget`() {
        val today = LocalDate.of(2026, 8, 15)
        val pacing = budgetPacing(spentCents = 1_000, budgetCents = 5_000, today = today)
        // 1000 * 31 / 14 = 2214
        assertEquals(2_214L, pacing.projectedCents)
        assertFalse(pacing.isPacingOver)
        assertEquals(0L, pacing.projectedOverCents)
    }

    @Test
    fun `over budget stays over regardless of pacing`() {
        val today = LocalDate.of(2026, 8, 15)
        val pacing = budgetPacing(spentCents = 6_000, budgetCents = 5_000, today = today)
        assertTrue(pacing.isOverBudget)
        assertTrue(pacing.isPacingOver)
        // projected = 6000 * 31 / 14 = 13285; overage 13285 - 5000 = 8285
        assertEquals(8_285L, pacing.projectedOverCents)
    }

    @Test
    fun `spent exactly at budget can still be pacing over`() {
        val today = LocalDate.of(2026, 8, 15)
        val pacing = budgetPacing(spentCents = 5_000, budgetCents = 5_000, today = today)
        assertFalse(pacing.isOverBudget)
        assertTrue(pacing.isPacingOver)
    }

    @Test
    fun `uses the real month length`() {
        val today = LocalDate.of(2026, 2, 15) // 28-day February, 14 days elapsed
        val pacing = budgetPacing(spentCents = 1_400, budgetCents = 3_000, today = today)
        assertEquals(2_800L, pacing.projectedCents)
        assertFalse(pacing.isPacingOver)
    }
}
