package com.expent.app.data.seed

import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType

/**
 * Out-of-the-box categories, seeded once on first launch.
 * [iconName] keys map to Material icons via [com.expent.app.ui.components.CategoryIcons].
 */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        expense("Food", "Restaurant", 0xFFFF7043L, 1),
        expense("Transport", "DirectionsCar", 0xFF42A5F5L, 2),
        expense("Housing", "Home", 0xFF8D6E63L, 3),
        expense("Utilities", "Bolt", 0xFFFFCA28L, 4),
        expense("Entertainment", "Movie", 0xFFAB47BCL, 5),
        expense("Shopping", "ShoppingCart", 0xFFEC407AL, 6),
        expense("Health", "Favorite", 0xFFEF5350L, 7),
        expense("Education", "School", 0xFF26A69AL, 8),
        expense("Other", "MoreHoriz", 0xFF78909CL, 9),
        income("Salary", "AttachMoney", 0xFF66BB6AL),
        income("Other Income", "TrendingUp", 0xFF29B6F6L)
    )

    private fun expense(name: String, icon: String, color: Long, sortOrder: Int) =
        CategoryEntity(
            name = name,
            type = TransactionType.EXPENSE,
            iconName = icon,
            colorArgb = color,
            isDefault = true,
            sortOrder = sortOrder
        )

    private fun income(name: String, icon: String, color: Long) =
        CategoryEntity(
            name = name,
            type = TransactionType.INCOME,
            iconName = icon,
            colorArgb = color,
            isDefault = true,
            sortOrder = 100
        )
}
