package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single money movement — an expense or an income ("money log").
 *
 * Amounts are stored as integer minor units (cents) to avoid floating point drift.
 */
@Serializable
enum class TransactionType { EXPENSE, INCOME }

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("timestamp")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val note: String?,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)
