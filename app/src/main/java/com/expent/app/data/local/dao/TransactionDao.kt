package com.expent.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.expent.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** A transaction joined with its category's display info for list screens. */
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "categoryIconName") val categoryIconName: String?,
    @ColumnInfo(name = "categoryColorArgb") val categoryColorArgb: Long?
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query(
        """
        SELECT t.*, c.name AS categoryName, c.iconName AS categoryIconName, c.colorArgb AS categoryColorArgb
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        ORDER BY t.timestamp DESC
        """
    )
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.*, c.name AS categoryName, c.iconName AS categoryIconName, c.colorArgb AS categoryColorArgb
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.timestamp >= :startInclusive AND t.timestamp < :endExclusive
        ORDER BY t.timestamp DESC
        """
    )
    fun observeBetweenWithCategory(
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
