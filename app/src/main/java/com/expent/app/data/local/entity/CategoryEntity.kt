package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A transaction category. Categories are typed (expense or income) and carry a
 * stable [iconName] key (mapped to a Compose icon in the UI layer) and an ARGB color.
 */
@Serializable
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String?,
    val colorArgb: Long,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    /** Optional monthly spending limit in cents; null means no budget. */
    val budgetCents: Long? = null
)
