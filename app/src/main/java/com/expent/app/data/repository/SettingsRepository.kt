package com.expent.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expent.app.core.CurrencyOption
import com.expent.app.core.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val currencyKey = stringPreferencesKey("currency")
    private val startingBalanceKey = longPreferencesKey("starting_balance")
    private val themeKey = stringPreferencesKey("theme")

    val currency: Flow<CurrencyOption> = context.settingsDataStore.data
        .map { prefs -> CurrencyOption.fromCode(prefs[currencyKey]) }

    /** Cash on hand when the user started tracking; 0 means not set. */
    val startingBalance: Flow<Long> = context.settingsDataStore.data
        .map { prefs -> prefs[startingBalanceKey] ?: 0L }

    val theme: Flow<ThemeOption> = context.settingsDataStore.data
        .map { prefs -> ThemeOption.fromCode(prefs[themeKey]) }

    suspend fun setCurrency(option: CurrencyOption) {
        context.settingsDataStore.edit { prefs ->
            prefs[currencyKey] = option.code
        }
    }

    suspend fun setStartingBalance(cents: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[startingBalanceKey] = cents
        }
    }

    suspend fun setTheme(option: ThemeOption) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeKey] = option.code
        }
    }
}
