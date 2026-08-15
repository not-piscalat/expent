package com.expent.app.di

import android.content.Context
import androidx.room.Room
import com.expent.app.data.local.ExpentDatabase
import com.expent.app.data.local.dao.CategoryDao
import com.expent.app.data.local.dao.DebtDao
import com.expent.app.data.local.dao.DebtPaymentDao
import com.expent.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpentDatabase =
        Room.databaseBuilder(context, ExpentDatabase::class.java, "expent.db")
            .build()

    @Provides
    fun provideTransactionDao(db: ExpentDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: ExpentDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideDebtDao(db: ExpentDatabase): DebtDao = db.debtDao()

    @Provides
    fun provideDebtPaymentDao(db: ExpentDatabase): DebtPaymentDao = db.debtPaymentDao()
}
