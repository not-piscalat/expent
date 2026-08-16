package com.expent.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expent.app.R
import com.expent.app.core.ThemeOption
import com.expent.app.ui.categories.AddCategoryScreen
import com.expent.app.ui.categories.CategoriesScreen
import com.expent.app.ui.debts.AddDebtScreen
import com.expent.app.ui.debts.DebtDetailScreen
import com.expent.app.ui.debts.DebtsScreen
import com.expent.app.ui.home.HomeScreen
import com.expent.app.ui.recurring.AddRecurringScreen
import com.expent.app.ui.recurring.RecurringScreen
import com.expent.app.ui.settings.SettingsScreen
import com.expent.app.ui.settings.SettingsViewModel
import com.expent.app.ui.theme.ExpentTheme
import com.expent.app.ui.theme.LocalCurrencySymbol
import com.expent.app.ui.transactions.AddTransactionScreen
import com.expent.app.ui.transactions.TransactionsScreen
import kotlinx.coroutines.flow.map

private const val ADD_TRANSACTION_ROUTE = "add_transaction?transactionId={transactionId}"
private const val ADD_DEBT_ROUTE = "add_debt?debtId={debtId}"
private const val DEBT_DETAIL_ROUTE = "debt_detail/{debtId}"
private const val CATEGORIES_ROUTE = "categories"
private const val ADD_CATEGORY_ROUTE = "add_category?categoryId={categoryId}"
private const val SETTINGS_ROUTE = "settings"
private const val RECURRING_ROUTE = "recurring"
private const val ADD_RECURRING_ROUTE = "add_recurring?templateId={templateId}"

enum class ExpentDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    TRANSACTIONS("transactions", R.string.nav_transactions, Icons.AutoMirrored.Filled.List),
    DEBTS("debts", R.string.nav_debts, Icons.Filled.AccountCircle)
}

@Composable
fun ExpentApp(viewModel: SettingsViewModel = hiltViewModel()) {
    val currencySymbol by viewModel.currency
        .map { it.symbol }
        .collectAsStateWithLifecycle(initialValue = "₱")
    val themeOption by viewModel.theme.collectAsStateWithLifecycle(initialValue = ThemeOption.SYSTEM)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Switching themes dissolves instead of snapping, so the whole palette
    // fades between light and dark.
    Crossfade(
        targetState = themeOption.resolvesToDark(isSystemInDarkTheme()),
        animationSpec = tween(durationMillis = 350),
        label = "theme"
    ) { darkTheme ->
    ExpentTheme(darkTheme = darkTheme) {
    CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
    Scaffold(
        bottomBar = {
            // The coin dock: each tab is a circular slot, and the page you're
            // on is a raised violet coin — the same token as the FAB.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpentDestination.entries.forEach { destination ->
                            NavCoin(
                                destination = destination,
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ExpentDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ExpentDestination.HOME.route) {
                HomeScreen(
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenTransaction = { id -> navController.navigate("add_transaction?transactionId=$id") }
                )
            }
            composable(ExpentDestination.TRANSACTIONS.route) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate("add_transaction") },
                    onEditTransaction = { id -> navController.navigate("add_transaction?transactionId=$id") }
                )
            }
            composable(ExpentDestination.DEBTS.route) {
                DebtsScreen(
                    onAddDebt = { navController.navigate("add_debt") },
                    onOpenDebt = { debtId -> navController.navigate("debt_detail/$debtId") }
                )
            }
            composable(
                route = ADD_TRANSACTION_ROUTE,
                arguments = listOf(
                    navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddTransactionScreen(
                    onDone = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(CATEGORIES_ROUTE) }
                )
            }
            composable(CATEGORIES_ROUTE) {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onAddCategory = { navController.navigate("add_category") },
                    onEditCategory = { id -> navController.navigate("add_category?categoryId=$id") }
                )
            }
            composable(
                route = ADD_CATEGORY_ROUTE,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddCategoryScreen(onDone = { navController.popBackStack() })
            }
            composable(RECURRING_ROUTE) {
                RecurringScreen(
                    onBack = { navController.popBackStack() },
                    onAddRecurring = { navController.navigate("add_recurring") },
                    onEditRecurring = { id -> navController.navigate("add_recurring?templateId=$id") }
                )
            }
            composable(
                route = ADD_RECURRING_ROUTE,
                arguments = listOf(
                    navArgument("templateId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddRecurringScreen(
                    onDone = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(CATEGORIES_ROUTE) }
                )
            }
            composable(
                route = ADD_DEBT_ROUTE,
                arguments = listOf(navArgument("debtId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddDebtScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = DEBT_DETAIL_ROUTE,
                arguments = listOf(navArgument("debtId") { type = NavType.LongType })
            ) { entry ->
                val debtId = entry.arguments?.getLong("debtId") ?: 0L
                DebtDetailScreen(
                    debtId = debtId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("add_debt?debtId=$debtId") },
                    onDeleted = { navController.popBackStack() }
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecurring = { navController.navigate(RECURRING_ROUTE) }
                )
            }
        }
    }
    }
    }
    }
}

@Composable
private fun NavCoin(
    destination: ExpentDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    // The active tab is a minted coin: the gradient disc rises above the dock
    // with a shadow, while the rest stay flat in their slots. Pressing any
    // coin sinks it briefly before the spring returns.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val coinScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.82f
            selected -> 1.06f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navCoinScale"
    )
    val shape = CircleShape
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    val label = stringResource(destination.labelRes)

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(coinScale)
            .then(
                if (selected) {
                    Modifier
                        .shadow(6.dp, shape)
                        .background(gradient, shape)
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
