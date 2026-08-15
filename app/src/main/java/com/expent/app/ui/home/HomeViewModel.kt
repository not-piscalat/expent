package com.expent.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.BudgetPacing
import com.expent.app.core.CategorySpending
import com.expent.app.core.DebtPosition
import com.expent.app.core.budgetPacing
import com.expent.app.core.debtPosition
import com.expent.app.core.spendingByCategory
import com.expent.app.core.withBudgets
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.repository.CategoryRepository
import com.expent.app.data.repository.DebtRepository
import com.expent.app.data.repository.SettingsRepository
import com.expent.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val monthTransactions: List<TransactionWithCategory> = emptyList(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val spendingByCategory: List<CategorySpending> = emptyList(),
    val debtPosition: DebtPosition = DebtPosition(),
    val startingBalanceCents: Long = 0,
    /** Running-rate pacing per budgeted category; empty unless viewing the current month. */
    val pacingByCategory: Map<Long, BudgetPacing> = emptyMap(),
    val monthLabel: String = "",
    val isCurrentMonth: Boolean = true
) {
    val balanceCents: Long get() = incomeCents - expenseCents
    val netWorthCents: Long get() = startingBalanceCents + balanceCents + debtPosition.netCents
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    debtRepository: DebtRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val currentMonth: LocalDate get() = LocalDate.now().withDayOfMonth(1)

    private val selectedMonth = MutableStateFlow(currentMonth)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthTransactions: Flow<List<TransactionWithCategory>> = selectedMonth
        .flatMapLatest { month ->
            transactionRepository.observeBetweenWithCategory(
                startInclusive = monthStartMillis(month),
                endExclusive = monthEndMillis(month)
            )
        }

    private val categories: Flow<List<CategoryEntity>> = categoryRepository.observeAll()

    private val debts: Flow<List<DebtWithPaid>> = debtRepository.observeAll()

    private val startingBalance: Flow<Long> = settingsRepository.startingBalance

    val uiState: StateFlow<HomeUiState> = combine(
        monthTransactions, selectedMonth, categories, debts, startingBalance
    ) { transactions, month, categories, debts, startingBalance ->
        val budgets = categories.associate { it.id to it.budgetCents }
        val spending = transactions.spendingByCategory().withBudgets(budgets)
        val isCurrentMonth = month == currentMonth
        val today = LocalDate.now()
        HomeUiState(
            monthTransactions = transactions,
            incomeCents = transactions
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amountCents },
            expenseCents = transactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amountCents },
            spendingByCategory = spending,
            debtPosition = debts.debtPosition(),
            startingBalanceCents = startingBalance,
            pacingByCategory = if (isCurrentMonth) {
                spending.mapNotNull { item ->
                    val budget = item.budgetCents?.takeIf { it > 0 } ?: return@mapNotNull null
                    item.categoryId?.let { it to budgetPacing(item.amountCents, budget, today) }
                }.toMap()
            } else {
                emptyMap()
            },
            monthLabel = month.format(monthFormatter),
            isCurrentMonth = isCurrentMonth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    /** Moves forward one month but never beyond the current month. */
    fun nextMonth() {
        val next = selectedMonth.value.plusMonths(1)
        selectedMonth.value = if (next.isAfter(currentMonth)) currentMonth else next
    }

    private fun monthStartMillis(month: LocalDate): Long =
        month.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun monthEndMillis(month: LocalDate): Long =
        month.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
