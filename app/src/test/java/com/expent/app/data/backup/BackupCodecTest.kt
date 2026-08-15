package com.expent.app.data.backup

import com.expent.app.core.RecurringFrequency
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtType
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {

    private fun sampleData() = BackupData(
        exportedAt = 1_234L,
        categories = listOf(
            CategoryEntity(
                id = 1,
                name = "Food",
                type = TransactionType.EXPENSE,
                iconName = "Restaurant",
                colorArgb = 0xFFFF7043L,
                isDefault = true,
                sortOrder = 1
            )
        ),
        transactions = listOf(
            TransactionEntity(
                id = 2,
                amountCents = 1_500,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                note = "Lunch",
                timestamp = 1_000
            )
        ),
        debts = listOf(
            DebtEntity(
                id = 3,
                title = "Loan",
                personName = "Ana",
                type = DebtType.LENT,
                amountCents = 10_000,
                note = null,
                dueTimestamp = null,
                createdAt = 500
            )
        ),
        payments = listOf(
            DebtPaymentEntity(
                id = 4,
                debtId = 3,
                amountCents = 2_000,
                timestamp = 2_000,
                note = "First payment"
            )
        ),
        recurringTemplates = listOf(
            RecurringTemplateEntity(
                id = 5,
                title = "Rent",
                amountCents = 15_000,
                type = TransactionType.EXPENSE,
                categoryId = 1,
                note = null,
                frequency = RecurringFrequency.MONTHLY,
                dayOfMonth = 1,
                dayOfWeek = 1,
                nextDueEpochDay = 20_000,
                isActive = true
            )
        )
    )

    @Test
    fun `round trips a full backup`() {
        val data = sampleData()
        val decoded = BackupCodec.decode(BackupCodec.encode(data))
        assertEquals(data, decoded)
    }

    @Test
    fun `decode fills missing top-level fields with defaults`() {
        val decoded = BackupCodec.decode("""{"version":1}""")
        assertEquals(1, decoded.version)
        assertEquals(emptyList<CategoryEntity>(), decoded.categories)
        assertEquals(emptyList<TransactionEntity>(), decoded.transactions)
        assertEquals(emptyList<RecurringTemplateEntity>(), decoded.recurringTemplates)
    }

    @Test
    fun `decode ignores unknown keys`() {
        val decoded = BackupCodec.decode(
            """{"version":1,"futureField":123,"categories":[],"exportedAt":42}"""
        )
        assertEquals(42L, decoded.exportedAt)
        assertEquals(emptyList<CategoryEntity>(), decoded.categories)
    }
}
