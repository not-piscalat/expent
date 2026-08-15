package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Whether money flowed out of your pocket (LENT) or into it (BORROWED). */
enum class DebtType { LENT, BORROWED }

/**
 * A debt — money you lent to someone, or money you borrowed.
 * The remaining balance is derived from [DebtPaymentEntity] rows, not stored.
 */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val personName: String?,
    val type: DebtType,
    val amountCents: Long,
    val note: String?,
    val dueTimestamp: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
