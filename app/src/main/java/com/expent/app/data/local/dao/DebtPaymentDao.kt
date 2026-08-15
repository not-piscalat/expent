package com.expent.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.expent.app.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {

    @Insert
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Delete
    suspend fun delete(payment: DebtPaymentEntity)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY timestamp DESC")
    fun observeForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>
}
