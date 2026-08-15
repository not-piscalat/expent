package com.expent.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A payment that reduces the remaining balance of a [DebtEntity]. */
@Serializable
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
    val note: String?,
    /** Firestore document ID once the payment belongs to a shared debt. */
    val remoteId: String? = null,
    /** The uid of the participant who made the payment (needed to derive the balance from either perspective). */
    val payerId: String? = null,
    /** Server timestamp from Firestore for last-writer-wins conflict resolution. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0
)
