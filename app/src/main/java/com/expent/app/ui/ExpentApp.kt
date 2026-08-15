package com.expent.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expent.app.R
import com.expent.app.ui.debts.DebtsScreen
import com.expent.app.ui.home.HomeScreen
import com.expent.app.ui.transactions.AddTransactionScreen
import com.expent.app.ui.transactions.TransactionsScreen

private const val ADD_TRANSACTION_ROUTE = "add_transaction"

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
fun ExpentApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

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
            composable(ExpentDestination.HOME.route) { HomeScreen() }
            composable(ExpentDestination.TRANSACTIONS.route) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION_ROUTE) }
                )
            }
            composable(ExpentDestination.DEBTS.route) { DebtsScreen() }
            composable(ADD_TRANSACTION_ROUTE) {
                AddTransactionScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
