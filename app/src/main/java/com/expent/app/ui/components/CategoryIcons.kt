package com.expent.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves the stable [com.expent.app.data.local.entity.CategoryEntity.iconName]
 * keys stored in the database to Material icons.
 */
object CategoryIcons {

    private val iconMap: Map<String, ImageVector> = mapOf(
        "Restaurant" to Icons.Filled.Restaurant,
        "DirectionsCar" to Icons.Filled.DirectionsCar,
        "Home" to Icons.Filled.Home,
        "Bolt" to Icons.Filled.Bolt,
        "Movie" to Icons.Filled.Movie,
        "ShoppingCart" to Icons.Filled.ShoppingCart,
        "Favorite" to Icons.Filled.Favorite,
        "School" to Icons.Filled.School,
        "MoreHoriz" to Icons.Filled.MoreHoriz,
        "AttachMoney" to Icons.Filled.AttachMoney,
        "TrendingUp" to Icons.Filled.TrendingUp
    )

    fun resolve(name: String?): ImageVector = iconMap[name] ?: Icons.Filled.MoreHoriz

    fun supports(name: String?): Boolean = name != null && iconMap.containsKey(name)
}
