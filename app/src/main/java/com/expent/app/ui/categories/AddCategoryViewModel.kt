package com.expent.app.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.FormValidation
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCategoryUiState(
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val iconName: String? = null,
    val colorArgb: Long = DEFAULT_COLOR_ARGB,
    val budgetInput: String = "",
    val isEditing: Boolean = false,
    val canSave: Boolean = false
) {
    companion object {
        const val DEFAULT_COLOR_ARGB = 0xFF26A69AL
    }
}

@HiltViewModel
class AddCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** -1 (or missing) means creating a new category; any positive value edits it. */
    private val categoryId: Long = savedStateHandle["categoryId"] ?: -1L

    private val _uiState = MutableStateFlow(AddCategoryUiState(isEditing = categoryId > 0))
    val uiState: StateFlow<AddCategoryUiState> = _uiState

    init {
        if (categoryId > 0) {
            viewModelScope.launch {
                val category = categoryRepository.getById(categoryId) ?: return@launch
                _uiState.value = AddCategoryUiState(
                    name = category.name,
                    type = category.type,
                    iconName = category.iconName,
                    colorArgb = category.colorArgb,
                    budgetInput = category.budgetCents?.let { MoneyUtil.toInput(it) }.orEmpty(),
                    isEditing = true,
                    canSave = true
                )
            }
        }
    }

    fun updateName(value: String) = update { it.copy(name = value) }

    fun setType(value: TransactionType) = update { it.copy(type = value) }

    fun selectIcon(name: String) = update { it.copy(iconName = name) }

    fun selectColor(colorArgb: Long) = update { it.copy(colorArgb = colorArgb) }

    fun updateBudget(input: String) = update { it.copy(budgetInput = MoneyUtil.sanitizeInput(input)) }

    suspend fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        val budgetCents = MoneyUtil.parse(state.budgetInput)?.takeIf { it > 0 }
        if (categoryId > 0) {
            val current = categoryRepository.getById(categoryId) ?: return
            categoryRepository.update(
                current.copy(
                    name = state.name.trim(),
                    type = state.type,
                    iconName = state.iconName,
                    colorArgb = state.colorArgb,
                    budgetCents = budgetCents
                )
            )
        } else {
            categoryRepository.add(
                CategoryEntity(
                    name = state.name.trim(),
                    type = state.type,
                    iconName = state.iconName,
                    colorArgb = state.colorArgb,
                    isDefault = false,
                    sortOrder = 1000,
                    budgetCents = budgetCents
                )
            )
        }
    }

    private fun update(transform: (AddCategoryUiState) -> AddCategoryUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(canSave = FormValidation.canSaveCategory(next.name))
        }
    }
}
