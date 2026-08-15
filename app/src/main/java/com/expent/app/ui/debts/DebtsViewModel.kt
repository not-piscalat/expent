package com.expent.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    debtRepository: DebtRepository
) : ViewModel() {

    val debts: StateFlow<List<DebtWithPaid>> = debtRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
