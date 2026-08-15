package com.expent.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.CurrencyOption
import com.expent.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currency: StateFlow<CurrencyOption> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrencyOption.PHP)

    fun setCurrency(option: CurrencyOption) {
        viewModelScope.launch {
            settingsRepository.setCurrency(option)
        }
    }
}
