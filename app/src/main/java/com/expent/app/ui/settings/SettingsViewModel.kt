package com.expent.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expent.app.core.CurrencyOption
import com.expent.app.core.ThemeOption
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

    val startingBalance: StateFlow<Long> = settingsRepository.startingBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val theme: StateFlow<ThemeOption> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeOption.SYSTEM)

    fun setCurrency(option: CurrencyOption) {
        viewModelScope.launch {
            settingsRepository.setCurrency(option)
        }
    }

    fun setStartingBalance(cents: Long) {
        viewModelScope.launch {
            settingsRepository.setStartingBalance(cents)
        }
    }

    fun setTheme(option: ThemeOption) {
        viewModelScope.launch {
            settingsRepository.setTheme(option)
        }
    }
}
