package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expent.app.core.RecurringFrequency
import kotlinx.serialization.Serializable

/**
 * A template that generates transactions automatically. The engine materializes
 * one transaction per due occurrence (advancing [nextDueEpochDay]) whenever the
 * app starts. Generated transactions are ordinary transactions; deleting a
 * template only stops future generation.
 */
@Serializable
@Entity(
    tableName = "recurring_templates",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class RecurringTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val note: String?,
    val frequency: RecurringFrequency,
    /** 1..31, the month day a MONTHLY schedule fires on. */
    val dayOfMonth: Int,
    /** DayOfWeek.value (1=Mon..7=Sun), the weekday a WEEKLY schedule fires on. */
    val dayOfWeek: Int,
    /** Epoch day of the next occurrence to materialize; advanced as transactions are generated. */
    val nextDueEpochDay: Long,
    val isActive: Boolean = true,
    /**
     * The uid of the account that owns this template, so a different account on
     * the same device neither sees it nor has its occurrences materialized.
     * Null on pre-ownership rows (kept visible to everyone).
     */
    val ownerId: String? = null
)
