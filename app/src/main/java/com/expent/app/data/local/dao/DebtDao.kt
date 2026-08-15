package com.expent.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expent.app.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

/** A debt joined with the sum of its payments, so lists can show a remaining balance. */
data class DebtWithPaid(
    @Embedded val debt: DebtEntity,
    @ColumnInfo(name = "totalPaidCents") val totalPaidCents: Long = 0
)

@Dao
interface DebtDao {

    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeById(id: Long): Flow<DebtEntity?>

    @Query(
        """
        SELECT d.*, COALESCE(SUM(p.amountCents), 0) AS totalPaidCents
        FROM debts d
        LEFT JOIN debt_payments p ON p.debtId = d.id
        GROUP BY d.id
        ORDER BY d.createdAt DESC
        """
    )
    fun observeDebtsWithPaid(): Flow<List<DebtWithPaid>>

    @Query(
        """
        SELECT d.*, COALESCE(SUM(p.amountCents), 0) AS totalPaidCents
        FROM debts d
        LEFT JOIN debt_payments p ON p.debtId = d.id
        WHERE d.id = :id
        GROUP BY d.id
        """
    )
    fun observeDebtWithPaid(id: Long): Flow<DebtWithPaid?>

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM debts")
    suspend fun getAll(): List<DebtEntity>

    @Insert
    suspend fun insertAll(debts: List<DebtEntity>)

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE remoteId IS NOT NULL")
    fun observeSynced(): Flow<List<DebtEntity>>

    @Query("DELETE FROM debts WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)

    @Query("DELETE FROM debts")
    suspend fun clearAll()
}
