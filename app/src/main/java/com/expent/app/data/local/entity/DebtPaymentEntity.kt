package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A payment that reduces the remaining balance of a [DebtEntity]. */
@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId")]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountCents: Long,
    val timestamp: Long,
    val note: String?
)
