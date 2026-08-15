package com.expent.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    ExpentTheme(darkTheme = themeOption.resolvesToDark(isSystemInDarkTheme())) {
    CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                ExpentDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
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
