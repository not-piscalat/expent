package com.expent.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AddTransactionUiState(
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val canSave: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

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

    val uiState: StateFlow<AddTransactionUiState> = combine(
        amountInput, type, selectedCategoryId, dateMillis, note
    ) { amount, t, categoryId, date, n ->
        AddTransactionUiState(
            amountInput = amount,
            type = t,
            selectedCategoryId = categoryId,
            dateMillis = date,
            note = n,
            canSave = MoneyUtil.parse(amount)?.let { it > 0 } == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    fun updateAmount(input: String) {
        amountInput.value = sanitizeAmount(input)
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
        transactionRepository.add(
            TransactionEntity(
                amountCents = cents,
                type = type.value,
                categoryId = selectedCategoryId.value ?: categories.value.firstOrNull()?.id,
                note = note.value.trim().ifEmpty { null },
                timestamp = dateMillis.value
            )
        )
    }

    /** Keeps the amount field sane: digits only, one dot, at most two decimals. */
    private fun sanitizeAmount(input: String): String {
        val result = StringBuilder()
        var dotSeen = false
        var decimals = 0
        for (c in input) {
            when {
                c.isDigit() && decimals < 2 -> {
                    result.append(c)
                    if (dotSeen) decimals++
                }
                c == '.' && !dotSeen -> {
                    result.append('.')
                    dotSeen = true
                }
                else -> Unit // commas and anything else are dropped
            }
        }
        return result.toString()
    }
}
