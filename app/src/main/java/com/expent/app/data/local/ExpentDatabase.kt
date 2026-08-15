package com.expent.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.TransactionDao
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExpentDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
}
