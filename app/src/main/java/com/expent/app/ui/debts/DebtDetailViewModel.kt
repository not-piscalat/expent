package com.expent.app.ui.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val authRepository: AuthRepository,
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

    /**
     * TEMPORARY step-3 hook: marks this debt as shared with a test participant
     * so the Firestore sync engine has something to push. Replaced by the real
     * share flow (step 4). No-op when already shared or signed out.
     */
    fun devShareDebt() {
        viewModelScope.launch {
            val uid = authRepository.authState.first()?.uid ?: return@launch
            val debt = debtRepository.observeById(debtId).first() ?: return@launch
            if (debt.remoteId != null) return@launch
            debtRepository.updateDebt(
                debt.copy(
                    remoteId = UUID.randomUUID().toString(),
                    creatorId = uid,
                    otherParticipantId = "dev-partner",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
