package com.expent.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.BudgetPacing
import com.expent.app.core.CategorySpending
import com.expent.app.core.Insight
import com.expent.app.core.InsightKind
import com.expent.app.core.MonthlyForecast
import com.expent.app.core.util.DateUtil
import com.expent.app.core.util.MoneyUtil
import java.time.LocalDate
import java.time.ZoneId
import com.expent.app.ui.components.CategoryAvatar
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.components.TransactionRow
import com.expent.app.ui.theme.LocalCurrencySymbol

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_month)
                    )
                }
                Text(
                    text = if (state.isCurrentMonth) {
                        stringResource(R.string.home_this_month)
                    } else {
                        state.monthLabel
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = viewModel::nextMonth,
                    enabled = !state.isCurrentMonth
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }

        item {
            BalanceCard(state)
        }

        if (state.forecast.hasForecast) {
            item {
                ForecastCard(state.forecast)
            }
        }

        if (state.startingBalanceCents > 0 ||
            state.debtPosition.lentCents > 0 ||
            state.debtPosition.borrowedCents > 0
        ) {
            item {
                NetPositionCard(state)
            }
        }

        if (state.insights.isNotEmpty()) {
            item {
                InsightsCard(state.insights.take(4))
            }
        }

        if (state.spendingByCategory.isNotEmpty()) {
            item {
                SpendingBreakdownCard(
                    spending = state.spendingByCategory,
                    pacingByCategory = state.pacingByCategory
                )
            }
        }

        if (state.monthTransactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(
                        if (state.isCurrentMonth) R.string.home_empty_body else R.string.home_empty_past_month
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(R.string.home_recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(state.monthTransactions.take(5)) { item ->
                TransactionRow(item)
            }
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = MoneyUtil.format(state.balanceCents, symbol = LocalCurrencySymbol.current),
                style = MaterialTheme.typography.displaySmall,
                color = if (state.balanceCents >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    label = stringResource(R.string.home_income),
                    value = MoneyUtil.format(state.incomeCents, symbol = LocalCurrencySymbol.current),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.home_expenses),
                    value = MoneyUtil.format(state.expenseCents, symbol = LocalCurrencySymbol.current),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(insights: List<Insight>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.titleMedium
            )
            insights.forEach { insight ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (insight.kind) {
                            InsightKind.UNUSUAL_EXPENSE -> Icons.Filled.Warning
                            InsightKind.DUPLICATE -> Icons.Filled.Info
                            InsightKind.MISSED_RECURRING -> Icons.Filled.DateRange
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = insightText(insight),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun insightText(insight: Insight): String {
    val symbol = LocalCurrencySymbol.current
    return when (insight.kind) {
        InsightKind.UNUSUAL_EXPENSE -> stringResource(
            R.string.insight_unusual,
            MoneyUtil.format(insight.amountCents, symbol = symbol),
            insight.categoryName ?: stringResource(R.string.uncategorized)
        )
        InsightKind.DUPLICATE -> stringResource(
            R.string.insight_duplicate,
            MoneyUtil.format(insight.amountCents, symbol = symbol),
            insight.note.orEmpty()
        )
        InsightKind.MISSED_RECURRING -> stringResource(
            R.string.insight_missed,
            insight.title.orEmpty(),
            insight.dateEpochDay?.let { day ->
                DateUtil.format(
                    LocalDate.ofEpochDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
            }.orEmpty(),
            MoneyUtil.format(insight.amountCents, symbol = symbol)
        )
    }
}

@Composable
private fun ForecastCard(forecast: MonthlyForecast) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_forecast_title),
                style = MaterialTheme.typography.titleMedium
            )
            ForecastRow(
                label = stringResource(R.string.home_forecast_income),
                amountCents = forecast.incomeCents,
                valueColor = MaterialTheme.colorScheme.primary
            )
            ForecastRow(
                label = stringResource(R.string.home_forecast_expenses),
                amountCents = forecast.expenseCents
            )
            forecast.budgetedExpenseCents?.let { budgeted ->
                ForecastRow(
                    label = stringResource(R.string.home_forecast_budgeted),
                    amountCents = budgeted,
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_forecast_net),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MoneyUtil.format(forecast.netCents, symbol = LocalCurrencySymbol.current),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (forecast.netCents >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(label: String, amountCents: Long, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = MoneyUtil.format(amountCents, symbol = LocalCurrencySymbol.current),
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NetPositionCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_net_worth),
                style = MaterialTheme.typography.titleMedium
            )
            NetPositionRow(
                label = stringResource(R.string.home_starting_cash),
                amountCents = state.startingBalanceCents
            )
            NetPositionRow(
                label = stringResource(R.string.home_balance),
                amountCents = state.balanceCents
            )
            NetPositionRow(
                label = stringResource(R.string.home_lent_out),
                amountCents = state.debtPosition.lentCents
            )
            NetPositionRow(
                label = stringResource(R.string.home_borrowed),
                amountCents = -state.debtPosition.borrowedCents,
                valueColor = MaterialTheme.colorScheme.error
            )
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_net_worth),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MoneyUtil.format(state.netWorthCents, symbol = LocalCurrencySymbol.current),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.netWorthCents >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun NetPositionRow(label: String, amountCents: Long, valueColor: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = MoneyUtil.format(amountCents, symbol = LocalCurrencySymbol.current),
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpendingBreakdownCard(
    spending: List<CategorySpending>,
    pacingByCategory: Map<Long, BudgetPacing>
) {
    val maxAmount = spending.maxOf { it.amountCents }.coerceAtLeast(1)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_spending_by_category),
                style = MaterialTheme.typography.titleMedium
            )
            spending.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(iconName = item.iconName, colorArgb = item.colorArgb)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item.name ?: stringResource(R.string.uncategorized),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = MoneyUtil.format(item.amountCents, symbol = LocalCurrencySymbol.current),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                val budget = item.budgetCents
                if (budget != null && budget > 0) {
                    val pacing = item.categoryId?.let { pacingByCategory[it] }
                    val over = item.amountCents > budget
                    val pacingOver = !over && pacing?.isPacingOver == true
                    val barColor = when {
                        over -> MaterialTheme.colorScheme.error
                        pacingOver -> MaterialTheme.colorScheme.tertiary
                        else -> Color(item.colorArgb)
                    }
                    LinearProgressIndicator(
                        progress = { (item.amountCents.toFloat() / budget.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = barColor
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = MoneyUtil.format(item.amountCents, symbol = LocalCurrencySymbol.current) +
                                " " + stringResource(R.string.budget_of) + " " +
                                MoneyUtil.format(budget, symbol = LocalCurrencySymbol.current),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        when {
                            over -> {
                                Text(
                                    text = stringResource(R.string.over_by) + " " +
                                        MoneyUtil.format(
                                            item.amountCents - budget,
                                            symbol = LocalCurrencySymbol.current
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            pacingOver -> {
                                Text(
                                    text = stringResource(
                                        R.string.pacing_over_by,
                                        MoneyUtil.format(
                                            pacing?.projectedOverCents ?: 0,
                                            symbol = LocalCurrencySymbol.current
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.pacing_on_track),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { item.amountCents.toFloat() / maxAmount.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(item.colorArgb)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
