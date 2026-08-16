package com.expent.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expent.app.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {

    @Insert
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Delete
    suspend fun delete(payment: DebtPaymentEntity)

    @Update
    suspend fun update(payment: DebtPaymentEntity)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND deletedAt = 0 ORDER BY timestamp DESC")
    fun observeForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    /** All payments of a debt, including tombstoned ones — used when soft-deleting a debt. */
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId")
    suspend fun getByDebtId(debtId: Long): List<DebtPaymentEntity>

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM debt_payments")
    suspend fun getAll(): List<DebtPaymentEntity>

    @Insert
    suspend fun insertAll(payments: List<DebtPaymentEntity>)

    @Query("SELECT * FROM debt_payments WHERE id = :id")
    suspend fun getById(id: Long): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payments WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): DebtPaymentEntity?

    @Query("SELECT * FROM debt_payments WHERE remoteId IS NOT NULL")
    fun observeSynced(): Flow<List<DebtPaymentEntity>>

    @Query("DELETE FROM debt_payments WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("DELETE FROM debt_payments")
    suspend fun clearAll()
}
