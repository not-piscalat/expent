package com.expent.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.RecurringSchedule
import com.expent.app.data.local.entity.RecurringTemplateEntity
import com.expent.app.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    val templates: StateFlow<List<RecurringTemplateEntity>> = recurringRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            recurringRepository.delete(template)
        }
    }

    /** Pauses or resumes a template. Resuming skips any occurrences missed while paused. */
    fun setActive(template: RecurringTemplateEntity, active: Boolean) {
        viewModelScope.launch {
            val nextDue = if (active) {
                RecurringSchedule.resumeDueDate(
                    nextDueEpochDay = template.nextDueEpochDay,
                    today = java.time.LocalDate.now(),
                    frequency = template.frequency,
                    dayOfMonth = template.dayOfMonth,
                    dayOfWeek = template.dayOfWeek
                )
            } else {
                template.nextDueEpochDay
            }
            recurringRepository.upsert(template.copy(isActive = active, nextDueEpochDay = nextDue))
        }
    }
}
