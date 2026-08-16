package com.expent.app.core

import com.expent.app.core.RecurringFrequency
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the per-account visibility rule for categories (and their budgets)\n *  and recurring templates on a shared device. */
class EntityVisibilityTest {

    private fun category(ownerId: String? = null) = CategoryEntity(
        name = "C",
        type = TransactionType.EXPENSE,
        iconName = null,
        colorArgb = 0,
        ownerId = ownerId
    )

    private fun template(ownerId: String? = null) = RecurringTemplateEntity(
        title = "T",
        amountCents = 100,
        type = TransactionType.EXPENSE,
        categoryId = null,
        note = null,
        frequency = RecurringFrequency.MONTHLY,
        dayOfMonth = 1,
        dayOfWeek = 1,
        nextDueEpochDay = 1,
        ownerId = ownerId
    )

    @Test
    fun `a category is visible only to its owner`() {
        val c = category(ownerId = "alice")
        assertTrue(c.visibleTo("alice"))
        assertFalse(c.visibleTo("bob"))
    }

    @Test
    fun `a template is visible only to its owner`() {
        val t = template(ownerId = "alice")
        assertTrue(t.visibleTo("alice"))
        assertFalse(t.visibleTo("bob"))
    }

    @Test
    fun `pre-ownership rows stay visible to any signed-in user`() {
        assertTrue(category().visibleTo("alice"))
        assertTrue(category().visibleTo("bob"))
        assertTrue(template().visibleTo("alice"))
        assertTrue(template().visibleTo("bob"))
    }

    @Test
    fun `a signed-out device shows everything`() {
        assertTrue(category(ownerId = "alice").visibleTo(null))
        assertTrue(template(ownerId = "alice").visibleTo(null))
    }
}
