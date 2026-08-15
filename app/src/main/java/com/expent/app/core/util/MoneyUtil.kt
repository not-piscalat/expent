package com.expent.app.core.util

import java.util.Locale

/**
 * Amounts are stored as integer minor units (cents) everywhere in the app.
 * This is the single place they become human-readable strings.
 *
 * TODO: make the currency symbol configurable once settings exist (defaults to none).
 */
object MoneyUtil {

    fun format(cents: Long, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%,.2f", cents / 100.0)
}
