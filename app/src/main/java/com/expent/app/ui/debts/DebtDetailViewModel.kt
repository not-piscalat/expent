package com.expent.app.ui.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.ShareCode
import com.expent.app.data.auth.AuthRepository
import com.expent.app.data.local.dao.DebtWithPaid
import com.expent.app.data.local.entity.DebtPaymentEntity
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Where the share dialog stands: idle, generating, ready with a code, or failed. */
sealed interface ShareState {
    data object Idle : ShareState
    data object Sharing : ShareState
    data class Ready(val code: String, val title: String) : ShareState
    data class Error(val message: String) : ShareState
}

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

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    /**
     * Shares this debt by generating (or reusing) a 6-character code. A
     * local-only debt is first stamped with a Firestore doc ID and the signed-in
     * user as creator — the sync engine then pushes the doc, code included.
     */
    fun shareDebt() {
        viewModelScope.launch {
            _shareState.value = ShareState.Sharing
            _shareState.value = try {
                val uid = authRepository.authState.first()?.uid
                    ?: return@launch
                val debt = debtRepository.observeById(debtId).first()
                    ?: return@launch
                val code = debt.shareCode
                    ?: ShareCode.generateUnique(debtRepository.getAllShareCodes())
                if (debt.remoteId == null) {
                    // First share: give the debt its remote identity so the
                    // syncer pushes it (participants = just the creator until
                    // someone joins with the code).
                    debtRepository.updateDebt(
                        debt.copy(
                            remoteId = UUID.randomUUID().toString(),
                            creatorId = uid,
                            otherParticipantId = null,
                            shareCode = code,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else if (debt.shareCode == null) {
                    // Already shared via the old dev hook but code-less: mint one.
                    debtRepository.updateDebt(
                        debt.copy(
                            shareCode = code,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                ShareState.Ready(code, debt.title)
            } catch (e: Exception) {
                ShareState.Error(e.message ?: "Sharing failed")
            }
        }
    }

    fun resetShare() {
        _shareState.value = ShareState.Idle
    }

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
