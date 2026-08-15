package com.expent.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.MonthOption
import com.expent.app.core.TransactionFilters
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val availableMonths: List<MonthOption> = emptyList(),
    val selectedMonthStart: Long? = null,
    val searchQuery: String = "",
    val isFiltering: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")

    private val availableMonths = TransactionFilters.recentMonths()

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeAllWithCategory(),
        selectedMonth,
        searchQuery
    ) { transactions, monthStart, query ->
        val monthEnd = availableMonths.firstOrNull { it.startMillis == monthStart }?.endMillis
        TransactionsUiState(
            transactions = TransactionFilters.filter(transactions, monthStart, monthEnd, query),
            availableMonths = availableMonths,
            selectedMonthStart = monthStart,
            searchQuery = query,
            isFiltering = monthStart != null || query.isNotBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun selectMonth(startMillis: Long?) {
        selectedMonth.value = startMillis
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }
}
