package com.expent.app.ui.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val debtId: Long = checkNotNull(savedStateHandle["debtId"])

    val debt: StateFlow<DebtWithPaid?> = debtRepository.observeByIdWithPaid(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments: StateFlow<List<DebtPaymentEntity>> = debtRepository.observePayments(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordPayment(amountCents: Long, note: String?) {
        viewModelScope.launch {
            debtRepository.addPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amountCents = amountCents,
                    timestamp = System.currentTimeMillis(),
                    note = note
                )
            )
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            debtRepository.deletePaymentById(paymentId)
        }
    }

    fun deleteDebt() {
        viewModelScope.launch {
            debtRepository.deleteDebtById(debtId)
        }
    }
}
