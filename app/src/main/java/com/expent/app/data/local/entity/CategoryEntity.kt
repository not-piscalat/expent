package com.expent.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A transaction category. Categories are typed (expense or income) and carry a
 * stable [iconName] key (mapped to a Compose icon in the UI layer) and an ARGB color.
 */
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
    val sortOrder: Int = 0
)
