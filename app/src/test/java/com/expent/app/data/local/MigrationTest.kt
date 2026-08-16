package com.expent.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.DebtStatus
import com.expent.app.data.local.entity.DebtType
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the hand-written migrations: a database created at v1 (with real data)
 * must open at the current version with Room's schema validation passing and
 * the data intact. Any drift between the migration SQL and Room's expected
 * schema fails here instead of crashing every existing user's app on upgrade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    /** The v1 schema, reconstructed from the original entities. */
    private val v1Schema = listOf(
        """
        CREATE TABLE IF NOT EXISTS `categories` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `type` TEXT NOT NULL,
            `iconName` TEXT,
            `colorArgb` INTEGER NOT NULL,
            `isDefault` INTEGER NOT NULL,
            `sortOrder` INTEGER NOT NULL
        )
        """,
        """
        CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name_type` ON `categories` (`name`, `type`)
        """,
        """
        CREATE TABLE IF NOT EXISTS `transactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `amountCents` INTEGER NOT NULL,
            `type` TEXT NOT NULL,
            `categoryId` INTEGER,
            `note` TEXT,
            `timestamp` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """,
        "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)",
        """
        CREATE TABLE IF NOT EXISTS `debts` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `personName` TEXT,
            `type` TEXT NOT NULL,
            `amountCents` INTEGER NOT NULL,
            `note` TEXT,
            `dueTimestamp` INTEGER,
            `createdAt` INTEGER NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS `debt_payments` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `debtId` INTEGER NOT NULL,
            `amountCents` INTEGER NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `note` TEXT,
            FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON DELETE CASCADE
        )
        """,
        "CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)"
    )

    @After
    fun cleanup() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migrating from v1 to v7 keeps data and validates the schema`() {
        context.deleteDatabase(dbName)

        // Create a real v1 database and seed it like a first-launch install.
        context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null).use { db ->
            v1Schema.forEach { db.execSQL(it) }
            // A genuine v1 install carries its schema version in PRAGMA user_version.
            db.execSQL("PRAGMA user_version = 1")
            db.execSQL(
                """
                INSERT INTO categories (name, type, iconName, colorArgb, isDefault, sortOrder)
                VALUES ('Food', 'EXPENSE', 'Restaurant', -4282973, 1, 1)
                """
            )
            db.execSQL(
                """
                INSERT INTO transactions (amountCents, type, categoryId, note, timestamp, createdAt)
                VALUES (1500, 'EXPENSE', 1, 'Lunch', 1000, 1000)
                """
            )
            db.execSQL(
                """
                INSERT INTO debts (title, personName, type, amountCents, note, dueTimestamp, createdAt)
                VALUES ('Loan', 'Ana', 'LENT', 10000, NULL, NULL, 500)
                """
            )
            db.execSQL(
                """
                INSERT INTO debt_payments (debtId, amountCents, timestamp, note)
                VALUES (1, 2000, 2000, 'First payment')
                """
            )
        }

        // Opening at the current version runs the migrations; Room validates the
        // resulting schema and throws if the SQL drifted from its expectations.
        val db = Room.databaseBuilder(context, ExpentDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

        // A write proves the migrated database is fully writable.
        db.openHelper.writableDatabase
        runBlocking {
            assertEquals(1, db.categoryDao().count())
            assertEquals(1, db.transactionDao().getAll().size)
            assertEquals(1, db.debtDao().getAll().size)
            assertEquals(1, db.debtPaymentDao().getAll().size)

            // v3's new structures exist and work: budgets land on categories,
            // recurring templates are insertable.
            db.categoryDao().update(
                db.categoryDao().getById(1)!!.copy(budgetCents = 5_000)
            )
            assertEquals(5_000L, db.categoryDao().getById(1)!!.budgetCents)
            db.recurringTemplateDao().insert(
                RecurringTemplateEntity(
                    title = "Rent",
                    amountCents = 12_000,
                    type = TransactionType.EXPENSE,
                    categoryId = 1,
                    note = null,
                    frequency = com.expent.app.core.RecurringFrequency.MONTHLY,
                    dayOfMonth = 1,
                    dayOfWeek = 1,
                    nextDueEpochDay = 20_000
                )
            )
            assertEquals(1, db.recurringTemplateDao().getAll().size)

            // v4's sync columns land on pre-existing data with their defaults:
            // local-only, open, never synced, alive.
            val migratedDebt = db.debtDao().getAll().single()
            assertEquals(DebtStatus.OPEN, migratedDebt.status)
            assertEquals(0L, migratedDebt.updatedAt)
            assertEquals(0L, migratedDebt.deletedAt)
            assertEquals(null, migratedDebt.remoteId)
            assertEquals(null, migratedDebt.creatorId)
            assertEquals(null, migratedDebt.otherParticipantId)
            assertEquals(null, migratedDebt.shareCode)
            val migratedPayment = db.debtPaymentDao().getAll().single()
            assertEquals(null, migratedPayment.remoteId)
            assertEquals(null, migratedPayment.payerId)
            assertEquals(0L, migratedPayment.updatedAt)
            // v6: payments carry the tombstone column, alive by default.
            assertEquals(0L, migratedPayment.deletedAt)
            // v7: both tables carry the persisted push guard, dirty by default
            // (0) so the next sync pushes each shared row exactly once.
            assertEquals(0L, migratedDebt.lastPushedUpdatedAt)
            assertEquals(0L, migratedPayment.lastPushedUpdatedAt)

            // And a shared debt + payment round-trip through the new columns.
            val sharedId = db.debtDao().insert(
                DebtEntity(
                    title = "Shared",
                    personName = "Kuya",
                    type = DebtType.LENT,
                    amountCents = 50_000,
                    note = null,
                    dueTimestamp = null,
                    createdAt = 700,
                    remoteId = "doc-abc",
                    creatorId = "uid-1",
                    otherParticipantId = "uid-2",
                    shareCode = "K7M2QX",
                    status = DebtStatus.OPEN,
                    updatedAt = 1_700_000_000_000,
                    deletedAt = 0
                )
            )
            val shared = db.debtDao().getAll().first { it.id == sharedId }
            assertEquals("doc-abc", shared.remoteId)
            assertEquals("uid-1", shared.creatorId)
            assertEquals("uid-2", shared.otherParticipantId)
            assertEquals("K7M2QX", shared.shareCode)
            db.debtPaymentDao().insert(
                DebtPaymentEntity(
                    debtId = sharedId,
                    amountCents = 10_000,
                    timestamp = 800,
                    note = null,
                    remoteId = "pay-xyz",
                    payerId = "uid-2",
                    updatedAt = 1_700_000_000_100
                )
            )
            assertEquals(1, db.debtPaymentDao().getAll().count { it.remoteId == "pay-xyz" })
        }
        db.close()
    }
}
