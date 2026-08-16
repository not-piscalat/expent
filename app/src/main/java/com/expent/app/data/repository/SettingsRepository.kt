package com.expent.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expent.app.core.CurrencyOption
import com.expent.app.core.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Per-account preferences: currency, theme, starting balance, and dismissed
 * insights are namespaced by the signed-in uid, so each sign-in on a shared
 * device gets its own values. Signed out (null uid) reads and writes the plain
 * legacy keys.
 *
 * Reads fall back to the legacy key when an account has never set a value, so
 * existing device settings keep working after the upgrade — the account's value
 * diverges from the device default the first time it changes a setting.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userUid: Flow<String?>
) {

    private val legacyCurrencyKey = stringPreferencesKey("currency")
    private val legacyStartingBalanceKey = longPreferencesKey("starting_balance")
    private val legacyThemeKey = stringPreferencesKey("theme")
    private val legacyDismissedInsightsKey = stringSetPreferencesKey("dismissed_insights")

    private fun key(uid: String?, base: String): String = if (uid == null) base else "${base}_$uid"

    private fun currencyKey(uid: String?) = stringPreferencesKey(key(uid, "currency"))
    private fun startingBalanceKey(uid: String?) = longPreferencesKey(key(uid, "starting_balance"))
    private fun themeKey(uid: String?) = stringPreferencesKey(key(uid, "theme"))
    private fun dismissedInsightsKey(uid: String?) = stringSetPreferencesKey(key(uid, "dismissed_insights"))

    val currency: Flow<CurrencyOption> = combine(context.settingsDataStore.data, userUid) { prefs, uid ->
        CurrencyOption.fromCode(prefs[currencyKey(uid)] ?: prefs[legacyCurrencyKey])
    }

    /** Cash on hand when the user started tracking; 0 means not set. */
    val startingBalance: Flow<Long> = combine(context.settingsDataStore.data, userUid) { prefs, uid ->
        prefs[startingBalanceKey(uid)] ?: prefs[legacyStartingBalanceKey] ?: 0L
    }

    val theme: Flow<ThemeOption> = combine(context.settingsDataStore.data, userUid) { prefs, uid ->
        ThemeOption.fromCode(prefs[themeKey(uid)] ?: prefs[legacyThemeKey])
    }

    /** Insight keys the user has reviewed and dismissed. */
    val dismissedInsightKeys: Flow<Set<String>> = combine(context.settingsDataStore.data, userUid) { prefs, uid ->
        prefs[dismissedInsightsKey(uid)] ?: prefs[legacyDismissedInsightsKey] ?: emptySet()
    }

    suspend fun setCurrency(option: CurrencyOption) {
        val uid = userUid.first()
        context.settingsDataStore.edit { prefs ->
            prefs[currencyKey(uid)] = option.code
        }
    }

    suspend fun setStartingBalance(cents: Long) {
        val uid = userUid.first()
        context.settingsDataStore.edit { prefs ->
            prefs[startingBalanceKey(uid)] = cents
        }
    }

    suspend fun setTheme(option: ThemeOption) {
        val uid = userUid.first()
        context.settingsDataStore.edit { prefs ->
            prefs[themeKey(uid)] = option.code
        }
    }

    suspend fun dismissInsight(key: String) {
        val uid = userUid.first()
        context.settingsDataStore.edit { prefs ->
            // Merge the legacy set on the first per-account write so insights
            // dismissed before the account diverged stay dismissed.
            val merged = (prefs[dismissedInsightsKey(uid)] ?: prefs[legacyDismissedInsightsKey] ?: emptySet()) + key
            prefs[dismissedInsightsKey(uid)] = merged
        }
    }
}
