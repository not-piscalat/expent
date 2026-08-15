package com.expent.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidationTest {

    @Test
    fun `accepts valid amounts`() {
        assertTrue(FormValidation.isValidAmount("5"))
        assertTrue(FormValidation.isValidAmount("0.01"))
        assertTrue(FormValidation.isValidAmount("1,234.56"))
    }

    @Test
    fun `rejects invalid amounts`() {
        assertFalse(FormValidation.isValidAmount(""))
        assertFalse(FormValidation.isValidAmount("0"))
        assertFalse(FormValidation.isValidAmount("0.00"))
        assertFalse(FormValidation.isValidAmount("abc"))
        assertFalse(FormValidation.isValidAmount("1.234"))
    }

    @Test
    fun `transaction save requires only a valid amount`() {
        assertTrue(FormValidation.canSaveTransaction("5"))
        assertFalse(FormValidation.canSaveTransaction(""))
    }

    @Test
    fun `debt save requires a title and a valid amount`() {
        assertFalse(FormValidation.canSaveDebt("", "5"))
        assertFalse(FormValidation.canSaveDebt("   ", "5"))
        assertFalse(FormValidation.canSaveDebt("Rent", ""))
        assertTrue(FormValidation.canSaveDebt("Rent", "5"))
    }

    @Test
    fun `category save requires a name`() {
        assertFalse(FormValidation.canSaveCategory(""))
        assertFalse(FormValidation.canSaveCategory("   "))
        assertTrue(FormValidation.canSaveCategory("Coffee"))
    }

    @Test
    fun `recurring save requires a title and a valid amount`() {
        assertFalse(FormValidation.canSaveRecurring("", "5"))
        assertFalse(FormValidation.canSaveRecurring("   ", "5"))
        assertFalse(FormValidation.canSaveRecurring("Rent", ""))
        assertFalse(FormValidation.canSaveRecurring("Rent", "0"))
        assertTrue(FormValidation.canSaveRecurring("Rent", "15,000"))
    }
}
