package com.expent.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.RecurringTemplateDao
import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        RecurringTemplateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ExpentDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
}

/** v1 -> v2: adds the optional monthly budget to categories. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN budgetCents INTEGER")
    }
}

/** v2 -> v3: adds recurring transaction templates. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amountCents` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `categoryId` INTEGER,
                `note` TEXT,
                `frequency` TEXT NOT NULL,
                `dayOfMonth` INTEGER NOT NULL,
                `dayOfWeek` INTEGER NOT NULL,
                `nextDueEpochDay` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recurring_templates_categoryId ON recurring_templates (categoryId)"
        )
    }
}
