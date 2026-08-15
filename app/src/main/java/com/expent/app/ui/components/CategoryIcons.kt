package com.expent.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves the stable [com.expent.app.data.local.entity.CategoryEntity.iconName]
 * keys stored in the database to Material icons. [all] is also the picker
 * catalog shown when creating or editing a category.
 */
object CategoryIcons {

    val all: List<Pair<String, ImageVector>> = listOf(
        "Restaurant" to Icons.Filled.Restaurant,
        "LocalCafe" to Icons.Filled.LocalCafe,
        "DirectionsCar" to Icons.Filled.DirectionsCar,
        "DirectionsWalk" to Icons.Filled.DirectionsWalk,
        "Home" to Icons.Filled.Home,
        "Bolt" to Icons.Filled.Bolt,
        "Movie" to Icons.Filled.Movie,
        "ShoppingCart" to Icons.Filled.ShoppingCart,
        "Favorite" to Icons.Filled.Favorite,
        "FitnessCenter" to Icons.Filled.FitnessCenter,
        "School" to Icons.Filled.School,
        "Medication" to Icons.Filled.Medication,
        "Pets" to Icons.Filled.Pets,
        "Flight" to Icons.Filled.Flight,
        "CreditCard" to Icons.Filled.CreditCard,
        "Payments" to Icons.Filled.Payments,
        "Wallet" to Icons.Filled.Wallet,
        "Savings" to Icons.Filled.Savings,
        "AttachMoney" to Icons.Filled.AttachMoney,
        "BarChart" to Icons.Filled.BarChart,
        "TrendingUp" to Icons.Filled.TrendingUp,
        "TrendingDown" to Icons.Filled.TrendingDown,
        "MoreHoriz" to Icons.Filled.MoreHoriz
    )

    private val iconMap: Map<String, ImageVector> = all.toMap()

    fun resolve(name: String?): ImageVector = iconMap[name] ?: Icons.Filled.MoreHoriz

    fun supports(name: String?): Boolean = name != null && iconMap.containsKey(name)
}
