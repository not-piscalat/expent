package com.expent.app.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.CategorySuggestion
import com.expent.app.core.CategorySuggester
import com.expent.app.core.FormValidation
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.dao.TransactionWithCategory
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
    val suggestions: List<CategorySuggestion> = emptyList(),
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

    /** Every transaction the user has categorized, the suggester's training set. */
    private val history: StateFlow<List<TransactionWithCategory>> = transactionRepository.observeAllWithCategory()
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

    /** Categories suggested from the note, learned from the user's own history. */
    val suggestions: StateFlow<List<CategorySuggestion>> = combine(
        note, type, history, categories
    ) { n, t, history, cats ->
        if (n.isBlank()) emptyList()
        else CategorySuggester.suggest(n, history, cats, t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class MainFields(
        val amountInput: String,
        val type: TransactionType,
        val selectedCategoryId: Long?,
        val dateMillis: Long,
        val note: String
    )

    val uiState: StateFlow<AddTransactionUiState> = combine(
        combine(amountInput, type, selectedCategoryId, dateMillis, note) { amount, t, categoryId, date, n ->
            MainFields(amount, t, categoryId, date, n)
        },
        suggestions
    ) { main, suggestions ->
        AddTransactionUiState(
            amountInput = main.amountInput,
            type = main.type,
            selectedCategoryId = main.selectedCategoryId,
            dateMillis = main.dateMillis,
            note = main.note,
            suggestions = suggestions,
            isEditing = editing,
            canSave = FormValidation.canSaveTransaction(main.amountInput)
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
