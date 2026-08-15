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
}
