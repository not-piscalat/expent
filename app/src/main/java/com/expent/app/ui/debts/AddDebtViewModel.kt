package com.expent.app.ui.debts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.DebtEntity
import com.expent.app.data.local.entity.DebtType
import com.expent.app.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDebtUiState(
    val title: String = "",
    val personName: String = "",
    val type: DebtType = DebtType.LENT,
    val amountInput: String = "",
    val dueDateMillis: Long? = null,
    val note: String = "",
    val isEditing: Boolean = false,
    val canSave: Boolean = false
)

@HiltViewModel
class AddDebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** -1 (or missing) means creating a new debt; any positive value edits that debt. */
    private val debtId: Long = savedStateHandle["debtId"] ?: -1L

    private val _uiState = MutableStateFlow(AddDebtUiState(isEditing = debtId > 0))
    val uiState: StateFlow<AddDebtUiState> = _uiState

    init {
        if (debtId > 0) {
            viewModelScope.launch {
                val debt = debtRepository.observeById(debtId).first() ?: return@launch
                _uiState.value = AddDebtUiState(
                    title = debt.title,
                    personName = debt.personName.orEmpty(),
                    type = debt.type,
                    amountInput = MoneyUtil.toInput(debt.amountCents),
                    dueDateMillis = debt.dueTimestamp,
                    note = debt.note.orEmpty(),
                    isEditing = true,
                    canSave = true
                )
            }
        }
    }

    fun updateTitle(value: String) = update { it.copy(title = value) }

    fun updatePersonName(value: String) = update { it.copy(personName = value) }

    fun setType(value: DebtType) = update { it.copy(type = value) }

    fun updateAmount(input: String) = update { it.copy(amountInput = MoneyUtil.sanitizeInput(input)) }

    fun updateDueDate(millis: Long?) = update { it.copy(dueDateMillis = millis) }

    fun updateNote(value: String) = update { it.copy(note = value) }

    suspend fun save() {
        val cents = MoneyUtil.parse(_uiState.value.amountInput) ?: return
        val state = _uiState.value
        if (debtId > 0) {
            val current = debtRepository.observeById(debtId).first() ?: return
            debtRepository.updateDebt(
                current.copy(
                    title = state.title.trim(),
                    personName = state.personName.trim().ifEmpty { null },
                    type = state.type,
                    amountCents = cents,
                    note = state.note.trim().ifEmpty { null },
                    dueTimestamp = state.dueDateMillis
                )
            )
        } else {
            debtRepository.addDebt(
                DebtEntity(
                    title = state.title.trim(),
                    personName = state.personName.trim().ifEmpty { null },
                    type = state.type,
                    amountCents = cents,
                    note = state.note.trim().ifEmpty { null },
                    dueTimestamp = state.dueDateMillis
                )
            )
        }
    }

    private fun update(transform: (AddDebtUiState) -> AddDebtUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(canSave = canSave(next))
        }
    }

    private fun canSave(state: AddDebtUiState): Boolean =
        state.title.isNotBlank() && MoneyUtil.parse(state.amountInput)?.let { it > 0 } == true
}
