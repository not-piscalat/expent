package com.expent.app.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.FormValidation
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.repository.CategoryRepository
import com.expent.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val isEditing: Boolean = false,
    val canSave: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** -1 (or missing) means creating a new transaction; any positive value edits it. */
    private val transactionId: Long = savedStateHandle["transactionId"] ?: -1L
    private val editing: Boolean = transactionId > 0

    private val amountInput = MutableStateFlow("")
    private val type = MutableStateFlow(TransactionType.EXPENSE)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val dateMillis = MutableStateFlow(System.currentTimeMillis())
    private val note = MutableStateFlow("")

    /** Categories matching the currently selected type, so the picker stays in sync. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = type
        .flatMapLatest { categoryRepository.observeByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (editing) {
            viewModelScope.launch {
                val existing = transactionRepository.observeById(transactionId).first()
                    ?: return@launch
                amountInput.value = MoneyUtil.toInput(existing.amountCents)
                type.value = existing.type
                selectedCategoryId.value = existing.categoryId
                dateMillis.value = existing.timestamp
                note.value = existing.note.orEmpty()
            }
        }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        amountInput, type, selectedCategoryId, dateMillis, note
    ) { amount, t, categoryId, date, n ->
        AddTransactionUiState(
            amountInput = amount,
            type = t,
            selectedCategoryId = categoryId,
            dateMillis = date,
            note = n,
            isEditing = editing,
            canSave = FormValidation.canSaveTransaction(amount)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    fun updateAmount(input: String) {
        amountInput.value = MoneyUtil.sanitizeInput(input)
    }

    fun setType(newType: TransactionType) {
        type.value = newType
        selectedCategoryId.value = null
    }

    fun selectCategory(id: Long) {
        selectedCategoryId.value = id
    }

    fun updateDate(millis: Long) {
        dateMillis.value = millis
    }

    fun updateNote(text: String) {
        note.value = text
    }

    suspend fun save() {
        val cents = MoneyUtil.parse(amountInput.value) ?: return
        val categoryId = selectedCategoryId.value ?: categories.value.firstOrNull()?.id
        if (editing) {
            val current = transactionRepository.observeById(transactionId).first() ?: return
            transactionRepository.update(
                current.copy(
                    amountCents = cents,
                    type = type.value,
                    categoryId = categoryId,
                    note = note.value.trim().ifEmpty { null },
                    timestamp = dateMillis.value
                )
            )
        } else {
            transactionRepository.add(
                TransactionEntity(
                    amountCents = cents,
                    type = type.value,
                    categoryId = categoryId,
                    note = note.value.trim().ifEmpty { null },
                    timestamp = dateMillis.value
                )
            )
        }
    }
}
