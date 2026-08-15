package com.expent.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionWithCategory>> =
        transactionRepository.observeAllWithCategory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
