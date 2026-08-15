package com.expent.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** A selectable month: label plus the exact millisecond range it covers. */
data class MonthOption(
    val label: String,
    val startMillis: Long,
    val endMillis: Long
)

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

    private val availableMonths = recentMonths()

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeAllWithCategory(),
        selectedMonth,
        searchQuery
    ) { transactions, monthStart, query ->
        val monthEnd = availableMonths.firstOrNull { it.startMillis == monthStart }?.endMillis
        val effectiveStart = monthStart ?: Long.MIN_VALUE
        val filtering = monthStart != null || query.isNotBlank()
        val q = query.trim()

        TransactionsUiState(
            transactions = if (filtering) {
                transactions.filter { tx ->
                    val inMonth = monthEnd == null ||
                        (tx.transaction.timestamp >= effectiveStart && tx.transaction.timestamp < monthEnd)
                    val matchesQuery = q.isEmpty() ||
                        tx.categoryName?.contains(q, ignoreCase = true) == true ||
                        tx.transaction.note?.contains(q, ignoreCase = true) == true
                    inMonth && matchesQuery
                }
            } else {
                transactions
            },
            availableMonths = availableMonths,
            selectedMonthStart = monthStart,
            searchQuery = query,
            isFiltering = filtering
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

    private fun recentMonths(count: Int = 12): List<MonthOption> {
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
        val current = LocalDate.now().withDayOfMonth(1)
        return (0 until count).map { offset ->
            val startDate = current.minusMonths(offset.toLong())
            val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = startDate.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            MonthOption(startDate.format(formatter), start, end)
        }
    }
}
