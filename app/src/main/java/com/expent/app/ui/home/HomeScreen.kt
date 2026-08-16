package com.expent.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expent.app.R
import com.expent.app.core.BudgetPacing
import com.expent.app.core.CategorySpending
import com.expent.app.core.ForecastAccuracy
import com.expent.app.core.Insight
import com.expent.app.core.InsightKind
import com.expent.app.core.MonthlyForecast
import com.expent.app.core.util.DateUtil
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import com.expent.app.ui.components.AnimatedMoneyText
import com.expent.app.ui.components.CategoryAvatar
import com.expent.app.ui.components.EmptyState
import com.expent.app.ui.components.LedgerCard
import com.expent.app.ui.components.TransactionRow
import com.expent.app.ui.theme.DisplayBig
import com.expent.app.ui.theme.LocalCurrencySymbol
import com.expent.app.ui.theme.MoneyDisplay
import com.expent.app.ui.theme.MoneyMedium
import com.expent.app.ui.theme.MoneySmall
import com.expent.app.ui.theme.TitleStrong
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenTransaction: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The stat pages: swipe sideways between them; never scroll for a number.
    val pages = buildList {
        add(HomePage.BALANCE)
        add(HomePage.SPENDING)
        if (state.forecast.hasForecast) add(HomePage.FORECAST)
        if (state.startingBalanceCents > 0 ||
            state.debtPosition.lentCents > 0 ||
            state.debtPosition.borrowedCents > 0
        ) add(HomePage.NET_WORTH)
        if (state.insights.isNotEmpty()) add(HomePage.INSIGHTS)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Switching months crossfades the whole page; the stat pages slide
    // horizontally beneath it.
    AnimatedContent(
        targetState = state.monthLabel,
        transitionSpec = {
            (fadeIn(tween(240, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = "month"
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            MonthHeader(
                monthLabel = state.monthLabel,
                isCurrentMonth = state.isCurrentMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onOpenSettings = onOpenSettings
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) +
                        pagerState.currentPageOffsetFraction).absoluteValue
                    val scale = 0.94f + (1f - pageOffset.coerceIn(0f, 1f)) * 0.06f
                    val alpha = 0.55f + (1f - pageOffset.coerceIn(0f, 1f)) * 0.45f

                    HomePageCard(
                        page = pages[page],
                        state = state,
                        onOpenTransaction = onOpenTransaction,
                        onDismiss = viewModel::dismissInsight,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    )
                }
            }

            PageDots(count = pages.size, current = pagerState.currentPage)

            if (state.monthTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(
                            if (state.isCurrentMonth) R.string.home_empty_body else R.string.home_empty_past_month
                        ),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_recent_activity),
                        style = TitleStrong,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(state.monthTransactions.take(5), key = { it.transaction.id }) { item ->
                            TransactionRow(item, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

private enum class HomePage { BALANCE, SPENDING, FORECAST, NET_WORTH, INSIGHTS }

@Composable
private fun HomePageCard(
    page: HomePage,
    state: HomeUiState,
    onOpenTransaction: (Long) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (page) {
        HomePage.BALANCE -> BalanceCard(state, modifier)
        HomePage.SPENDING -> SpendingCard(state, modifier)
        HomePage.FORECAST -> ForecastCard(state.forecast, state.forecastAccuracy, modifier)
        HomePage.NET_WORTH -> NetWorthCard(state, modifier)
        HomePage.INSIGHTS -> InsightsCard(state.insights, onOpenTransaction, onDismiss, modifier)
    }
}

@Composable
private fun MonthHeader(
    monthLabel: String,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month)
            )
        }
        Text(
            text = if (isCurrentMonth) {
                stringResource(R.string.home_this_month)
            } else {
                monthLabel
            },
            style = DisplayBig,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onNext,
            enabled = !isCurrentMonth
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

@Composable
private fun PageDots(count: Int, current: Int) {
    if (count <= 1) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(
                targetValue = if (selected) 20.dp else 6.dp,
                animationSpec = tween(220),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun BalanceCard(state: HomeUiState, modifier: Modifier = Modifier) {
    // The month's story in one panel: the running total rides a violet wash,
    // with the trajectory of the month drawn beneath it as a sparkline.
    val wash = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
        )
    )
    // Cumulative balance over the month, one point per transaction in time order.
    val series = remember(state.monthTransactions) {
        state.monthTransactions
            .sortedBy { it.transaction.timestamp }
            .runningFold(0L) { acc, item ->
                acc + if (item.transaction.type == TransactionType.INCOME) {
                    item.transaction.amountCents
                } else {
                    -item.transaction.amountCents
                }
            }
    }
    LedgerCard(
        modifier = modifier.background(
            brush = wash,
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.home_balance),
                style = DisplayBig,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            AnimatedMoneyText(
                amountCents = state.balanceCents,
                symbol = LocalCurrencySymbol.current,
                style = MoneyDisplay,
                color = if (state.balanceCents >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Spacer(Modifier.height(16.dp))
            Sparkline(
                points = series,
                lineColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
            Spacer(Modifier.height(18.dp))
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

/** The month's trajectory: cumulative balance as a smooth line with a soft fill. */
@Composable
private fun Sparkline(
    points: List<Long>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val min = points.min()
        val max = points.max()
        val range = (max - min).coerceAtLeast(1).toFloat()
        val inset = 2.dp.toPx()
        val plotHeight = size.height - inset * 2
        val step = size.width / (points.size - 1)
        val line = Path()
        points.forEachIndexed { i, v ->
            val x = i * step
            val y = inset + plotHeight - ((v - min).toFloat() / range) * plotHeight
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, fillColor)
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx()))
        val last = points.last()
        val lastY = inset + plotHeight - ((last - min).toFloat() / range) * plotHeight
        drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(size.width, lastY))
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MoneyMedium
        )
    }
}

@Composable
private fun SpendingCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val spending = state.spendingByCategory
    LedgerCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_spending_by_category),
                style = TitleStrong,
                modifier = Modifier.fillMaxWidth()
            )
            if (spending.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val total = spending.sumOf { it.amountCents }.coerceAtLeast(1)
                DonutChart(spending)
                spending.take(4).forEach { item ->
                    val share = item.amountCents.toFloat() / total
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryAvatar(iconName = item.iconName, colorArgb = item.colorArgb)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name ?: stringResource(R.string.uncategorized),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            // The month's spending, drawn as ruled proportions:
                            // each bar shows that category's share of the total.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(share.coerceIn(0f, 1f))
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(item.colorArgb))
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.home_share_pct, (share * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = MoneyUtil.format(item.amountCents, symbol = LocalCurrencySymbol.current),
                            style = MoneyMedium
                        )
                    }
                    val budget = item.budgetCents
                    if (budget != null && budget > 0) {
                        val pacing = item.categoryId?.let { state.pacingByCategory[it] }
                        val over = item.amountCents > budget
                        val pacingOver = !over && pacing?.isPacingOver == true
                        val barColor = when {
                            over -> MaterialTheme.colorScheme.error
                            pacingOver -> MaterialTheme.colorScheme.tertiary
                            else -> Color(item.colorArgb)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { (item.amountCents.toFloat() / budget.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f).height(4.dp),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.home_budget_used,
                                    (item.amountCents.toFloat() / budget.toFloat() * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = barColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A donut of the month's spending: each category a slice of the coin. */
@Composable
private fun DonutChart(spending: List<CategorySpending>, modifier: Modifier = Modifier) {
    val total = spending.sumOf { it.amountCents }.coerceAtLeast(1)
    // The coin spins up when it first appears instead of snapping into place.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(total) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.size(128.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(128.dp)) {
            val stroke = 18.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            val gapDegrees = 3f
            var start = -90f
            spending.forEach { item ->
                val sweep = 360f * item.amountCents / total
                drawArc(
                    color = Color(item.colorArgb),
                    startAngle = start + gapDegrees / 2,
                    sweepAngle = (sweep * progress.value - gapDegrees).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_spent_total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedMoneyText(
                amountCents = total,
                symbol = LocalCurrencySymbol.current,
                style = MoneySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ForecastCard(forecast: MonthlyForecast, accuracy: ForecastAccuracy, modifier: Modifier = Modifier) {
    LedgerCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_forecast_title),
                style = TitleStrong
            )
            Spacer(Modifier.height(4.dp))
            ForecastBars(forecast.incomeCents, forecast.expenseCents)
            forecast.budgetedExpenseCents?.let { budgeted ->
                ForecastRow(
                    label = stringResource(R.string.home_forecast_budgeted),
                    amountCents = budgeted,
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = stringResource(R.string.home_forecast_net),
                    style = TitleStrong,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MoneyUtil.format(forecast.netCents, symbol = LocalCurrencySymbol.current),
                    style = MoneyMedium,
                    color = if (forecast.netCents >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            if (accuracy.hasData) {
                val parts = buildList {
                    accuracy.averageIncomeDeviationPct?.let {
                        add(stringResource(R.string.forecast_accuracy_income, it))
                    }
                    accuracy.averageExpenseDeviationPct?.let {
                        add(stringResource(R.string.forecast_accuracy_expenses, it))
                    }
                }
                Text(
                    text = stringResource(R.string.forecast_accuracy) + " · " + parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Expected income against expected expenses on one shared scale. */
@Composable
private fun ForecastBars(incomeCents: Long, expenseCents: Long, modifier: Modifier = Modifier) {
    val max = maxOf(incomeCents, expenseCents).coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ForecastBar(
            label = stringResource(R.string.home_forecast_income),
            amountCents = incomeCents,
            fraction = incomeCents.toFloat() / max,
            color = MaterialTheme.colorScheme.primary
        )
        ForecastBar(
            label = stringResource(R.string.home_forecast_expenses),
            amountCents = expenseCents,
            fraction = expenseCents.toFloat() / max,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun ForecastBar(label: String, amountCents: Long, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = MoneyUtil.format(amountCents, symbol = LocalCurrencySymbol.current),
            style = MoneyMedium
        )
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
            style = MoneyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NetWorthCard(state: HomeUiState, modifier: Modifier = Modifier) {
    LedgerCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_net_worth),
                style = TitleStrong
            )
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(8.dp))
            // How the month composes: cash, movement, and debt on one strip.
            NetWorthBar(state)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = stringResource(R.string.home_net_worth),
                    style = TitleStrong,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MoneyUtil.format(state.netWorthCents, symbol = LocalCurrencySymbol.current),
                    style = MoneyMedium,
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

/** How the month composes: starting cash, flow, and debt on one shared strip. */
@Composable
private fun NetWorthBar(state: HomeUiState, modifier: Modifier = Modifier) {
    val segments = buildList {
        if (state.startingBalanceCents > 0) {
            add(MaterialTheme.colorScheme.secondary to state.startingBalanceCents)
        }
        if (state.balanceCents > 0) {
            add(MaterialTheme.colorScheme.primary to state.balanceCents)
        }
        if (state.debtPosition.lentCents > 0) {
            add(MaterialTheme.colorScheme.tertiary to state.debtPosition.lentCents)
        }
        if (state.debtPosition.borrowedCents > 0) {
            add(MaterialTheme.colorScheme.error to state.debtPosition.borrowedCents)
        }
    }
    val total = segments.sumOf { it.second }.coerceAtLeast(1)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (segments.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var x = 0f
                segments.forEach { (color, amount) ->
                    val width = size.width * amount / total
                    drawRect(
                        color = color,
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(width, size.height)
                    )
                    x += width
                }
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
            style = MoneyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InsightsCard(
    insights: List<Insight>,
    onOpenTransaction: (Long) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.insights_title),
                style = TitleStrong
            )
            if (insights.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                insights.take(4).forEach { insight ->
                    val transactionId = insight.transactionId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = transactionId != null) {
                                transactionId?.let(onOpenTransaction)
                            }
                    ) {
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
                        IconButton(onClick = { onDismiss(insight.dismissKey) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.dismiss),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
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
