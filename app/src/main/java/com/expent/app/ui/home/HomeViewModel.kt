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

data class HomeUiState(
    val monthTransactions: List<TransactionWithCategory> = emptyList(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0
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
                    .sumOf { it.transaction.amountCents }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
