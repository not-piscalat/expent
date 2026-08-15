package com.expent.app.ui.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.FormValidation
import com.expent.app.core.RecurringFrequency
import com.expent.app.core.RecurringSchedule
import com.expent.app.core.util.MoneyUtil
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.local.entity.TransactionType
import com.expent.app.data.repository.CategoryRepository
import com.expent.app.data.repository.RecurringRepository
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
import java.time.LocalDate
import javax.inject.Inject

private data class MainFields(
    val title: String,
    val amountInput: String,
    val type: TransactionType,
    val selectedCategoryId: Long?
)

private data class ScheduleFields(
    val note: String,
    val frequency: RecurringFrequency,
    val dayOfMonth: Int,
    val dayOfWeek: Int
)

data class AddRecurringUiState(
    val title: String = "",
    val amountInput: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val note: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfMonth: Int = LocalDate.now().dayOfMonth,
    val dayOfWeek: Int = 1,
    val isEditing: Boolean = false,
    val canSave: Boolean = false
)

@HiltViewModel
class AddRecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** -1 (or missing) means creating a new template; any positive value edits it. */
    private val templateId: Long = savedStateHandle["templateId"] ?: -1L
    private val editing: Boolean = templateId > 0

    private val title = MutableStateFlow("")
    private val amountInput = MutableStateFlow("")
    private val type = MutableStateFlow(TransactionType.EXPENSE)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val note = MutableStateFlow("")
    private val frequency = MutableStateFlow(RecurringFrequency.MONTHLY)
    private val dayOfMonth = MutableStateFlow(LocalDate.now().dayOfMonth)
    private val dayOfWeek = MutableStateFlow(1)

    /** Categories matching the currently selected type, so the picker stays in sync. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = type
        .flatMapLatest { categoryRepository.observeByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (editing) {
            viewModelScope.launch {
                val existing = recurringRepository.getById(templateId) ?: return@launch
                title.value = existing.title
                amountInput.value = MoneyUtil.toInput(existing.amountCents)
                type.value = existing.type
                selectedCategoryId.value = existing.categoryId
                note.value = existing.note.orEmpty()
                frequency.value = existing.frequency
                dayOfMonth.value = existing.dayOfMonth
                dayOfWeek.value = existing.dayOfWeek
            }
        }
    }

    val uiState: StateFlow<AddRecurringUiState> = combine(
        combine(title, amountInput, type, selectedCategoryId) { t, amount, ty, categoryId ->
            MainFields(t, amount, ty, categoryId)
        },
        combine(note, frequency, dayOfMonth, dayOfWeek) { n, freq, dom, dow ->
            ScheduleFields(n, freq, dom, dow)
        }
    ) { main, schedule ->
        AddRecurringUiState(
            title = main.title,
            amountInput = main.amountInput,
            type = main.type,
            selectedCategoryId = main.selectedCategoryId,
            note = schedule.note,
            frequency = schedule.frequency,
            dayOfMonth = schedule.dayOfMonth,
            dayOfWeek = schedule.dayOfWeek,
            isEditing = editing,
            canSave = FormValidation.canSaveRecurring(main.title, main.amountInput)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddRecurringUiState())

    fun updateTitle(text: String) {
        title.value = text
    }

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

    fun updateNote(text: String) {
        note.value = text
    }

    fun setFrequency(newFrequency: RecurringFrequency) {
        frequency.value = newFrequency
    }

    fun setDayOfMonth(day: Int) {
        dayOfMonth.value = day
    }

    fun setDayOfWeek(day: Int) {
        dayOfWeek.value = day
    }

    suspend fun save() {
        val cents = MoneyUtil.parse(amountInput.value) ?: return
        val categoryId = selectedCategoryId.value ?: categories.value.firstOrNull()?.id
        val today = LocalDate.now()

        if (editing) {
            val existing = recurringRepository.getById(templateId) ?: return
            val scheduleChanged = existing.frequency != frequency.value ||
                existing.dayOfMonth != dayOfMonth.value ||
                existing.dayOfWeek != dayOfWeek.value
            recurringRepository.upsert(
                existing.copy(
                    title = title.value.trim(),
                    amountCents = cents,
                    type = type.value,
                    categoryId = categoryId,
                    note = note.value.trim().ifEmpty { null },
                    frequency = frequency.value,
                    dayOfMonth = dayOfMonth.value,
                    dayOfWeek = dayOfWeek.value,
                    nextDueEpochDay = if (scheduleChanged) {
                        RecurringSchedule.firstDueDate(
                            today, frequency.value, dayOfMonth.value, dayOfWeek.value
                        ).toEpochDay()
                    } else {
                        existing.nextDueEpochDay
                    }
                )
            )
        } else {
            recurringRepository.upsert(
                RecurringTemplateEntity(
                    title = title.value.trim(),
                    amountCents = cents,
                    type = type.value,
                    categoryId = categoryId,
                    note = note.value.trim().ifEmpty { null },
                    frequency = frequency.value,
                    dayOfMonth = dayOfMonth.value,
                    dayOfWeek = dayOfWeek.value,
                    nextDueEpochDay = RecurringSchedule.firstDueDate(
                        today, frequency.value, dayOfMonth.value, dayOfWeek.value
                    ).toEpochDay()
                )
            )
        }
    }
}
