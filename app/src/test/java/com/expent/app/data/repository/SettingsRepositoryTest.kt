package com.expent.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.expent.app.core.CurrencyOption
import com.expent.app.core.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Guards per-account settings: each sign-in on a shared device keeps its own
 *  currency, theme, starting balance, and dismissed insights. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var uid: MutableStateFlow<String?>
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        uid = MutableStateFlow(null)
        repo = SettingsRepository(context, uid)
    }

    @Test
    fun `each account keeps its own currency`() = runBlocking {
        // Signed out: writes go to the device/legacy key.
        repo.setCurrency(CurrencyOption.PHP)
        assertEquals(CurrencyOption.PHP, repo.currency.first())

        // A fresh account inherits the device default until it changes it.
        uid.value = "alice"
        assertEquals(CurrencyOption.PHP, repo.currency.first())
        repo.setCurrency(CurrencyOption.USD)
        assertEquals(CurrencyOption.USD, repo.currency.first())

        // Bob never sees Alice's choice.
        uid.value = "bob"
        assertEquals(CurrencyOption.PHP, repo.currency.first())
        repo.setCurrency(CurrencyOption.NONE)
        assertEquals(CurrencyOption.NONE, repo.currency.first())

        // Alice's choice survives the switch back.
        uid.value = "alice"
        assertEquals(CurrencyOption.USD, repo.currency.first())

        // The device default is untouched by account changes.
        uid.value = null
        assertEquals(CurrencyOption.PHP, repo.currency.first())
    }

    @Test
    fun `theme and starting balance are per-account`() = runBlocking {
        repo.setStartingBalance(10_000)
        repo.setTheme(ThemeOption.LIGHT)

        uid.value = "alice"
        repo.setStartingBalance(25_000)
        repo.setTheme(ThemeOption.DARK)
        assertEquals(25_000L, repo.startingBalance.first())
        assertEquals(ThemeOption.DARK, repo.theme.first())

        uid.value = "bob"
        assertEquals(10_000L, repo.startingBalance.first())
        assertEquals(ThemeOption.LIGHT, repo.theme.first())

        uid.value = "alice"
        assertEquals(25_000L, repo.startingBalance.first())
        assertEquals(ThemeOption.DARK, repo.theme.first())
    }

    @Test
    fun `dismissing an insight is per-account and inherits legacy dismissals`() = runBlocking {
        repo.dismissInsight("legacy-1")

        uid.value = "alice"
        repo.dismissInsight("alice-1")
        assertEquals(setOf("legacy-1", "alice-1"), repo.dismissedInsightKeys.first())

        // Bob only sees the legacy dismissals, not Alice's.
        uid.value = "bob"
        assertEquals(setOf("legacy-1"), repo.dismissedInsightKeys.first())

        // Alice keeps accumulating her own (and the inherited legacy) set.
        uid.value = "alice"
        repo.dismissInsight("alice-2")
        assertEquals(setOf("legacy-1", "alice-1", "alice-2"), repo.dismissedInsightKeys.first())
    }
}
