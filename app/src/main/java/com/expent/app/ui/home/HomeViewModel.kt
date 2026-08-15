package com.expent.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** One row of the monthly breakdown: a category and its total spent this month. */
data class CategorySpending(
    val categoryId: Long?,
    val name: String?,
    val iconName: String?,
    val colorArgb: Long,
    val amountCents: Long
)

data class HomeUiState(
    val monthTransactions: List<TransactionWithCategory> = emptyList(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val spendingByCategory: List<CategorySpending> = emptyList()
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthStart = today
        .withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val nextMonthStart = today
        .withDayOfMonth(1)
        .plusMonths(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val uiState: StateFlow<HomeUiState> = transactionRepository
        .observeBetweenWithCategory(monthStart, nextMonthStart)
        .map { transactions ->
            HomeUiState(
                monthTransactions = transactions,
                incomeCents = transactions
                    .filter { it.transaction.type == TransactionType.INCOME }
                    .sumOf { it.transaction.amountCents },
                expenseCents = transactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.amountCents },
                spendingByCategory = transactions.spendingByCategory()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    /** Groups the month's expenses by category, sorted by total descending. */
    private fun List<TransactionWithCategory>.spendingByCategory(): List<CategorySpending> =
        filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.transaction.categoryId }
            .map { (categoryId, items) ->
                CategorySpending(
                    categoryId = categoryId,
                    name = items.firstNotNullOfOrNull { it.categoryName },
                    iconName = items.firstNotNullOfOrNull { it.categoryIconName },
                    colorArgb = items.firstNotNullOfOrNull { it.categoryColorArgb } ?: 0xFF9E9E9E,
                    amountCents = items.sumOf { it.transaction.amountCents }
                )
            }
            .sortedByDescending { it.amountCents }
}
