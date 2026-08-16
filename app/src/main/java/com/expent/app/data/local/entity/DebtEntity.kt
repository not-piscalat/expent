package com.expent.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Whether money flowed out of your pocket (LENT) or into it (BORROWED). */
@Serializable
enum class DebtType { LENT, BORROWED }

/**
 * Lifecycle state of a debt. Local-only debts are always OPEN; SETTLED is
 * applied by the sync layer (or a future "mark settled" action) once a shared
 * debt is fully paid or forgiven.
 */
@Serializable
enum class DebtStatus { OPEN, SETTLED }

/**
 * A debt — money you lent to someone, or money you borrowed.
 * The remaining balance is derived from [DebtPaymentEntity] rows, not stored.
 *
 * The `remote*` columns are null for local-only debts and are filled in by the
 * sync layer once a debt is shared with another account (see the mutual-debt
 * milestone). `updatedAt` is the last-writer-wins timestamp used to resolve
 * conflicts; `deletedAt` is the tombstone that propagates deletions to the
 * other participant instead of hard-deleting.
 */
@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val personName: String?,
    val type: DebtType,
    val amountCents: Long,
    val note: String?,
    val dueTimestamp: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    /** Firestore document ID once shared; null while local-only. */
    val remoteId: String? = null,
    /** The uid of the account whose perspective this record is stored in. */
    val creatorId: String? = null,
    /** The uid of the other participant in a shared debt. */
    val otherParticipantId: String? = null,
    /**
     * The short code the creator shares with a partner so they can link the
     * debt (e.g. "K7M2QX"). Stored locally so the creator can re-show it and
     * the sync engine can write it to the Firestore doc atomically.
     */
    val shareCode: String? = null,
    @ColumnInfo(defaultValue = "'OPEN'") val status: DebtStatus = DebtStatus.OPEN,
    /** Server timestamp from Firestore for last-writer-wins conflict resolution. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    /** Tombstone timestamp; 0 means the debt is alive. */
    @ColumnInfo(defaultValue = "0") val deletedAt: Long = 0
)
