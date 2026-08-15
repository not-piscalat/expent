package com.expent.app.core.util

import kotlin.math.abs
import java.util.Locale

/**
 * Amounts are stored as integer minor units (cents) everywhere in the app.
 * This is the single place they become human-readable strings.
 *
 * Currently hardcoded to Philippine Peso; TODO: make the currency configurable
 * once settings exist.
 */
object MoneyUtil {

    private const val PESO_SYMBOL = "₱"

    fun format(cents: Long, locale: Locale = Locale.getDefault()): String {
        val sign = if (cents < 0) "-" else ""
        return sign + PESO_SYMBOL + String.format(locale, "%,.2f", abs(cents) / 100.0)
    }

    /**
     * Parses a user-typed amount into minor units (cents).
     * Accepts an optional leading minus, thousands separators (commas) and at most
     * two decimal places. Returns null when the input is not a valid amount.
     */
    fun parse(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return null

        val negative = cleaned.startsWith("-")
        val body = if (negative) cleaned.drop(1) else cleaned
        if (body.isEmpty()) return null

        val parts = body.split('.')
        if (parts.size > 2) return null

        val whole = parts[0].ifEmpty { "0" }
        if (whole.any { !it.isDigit() }) return null

        val fraction = parts.getOrNull(1) ?: ""
        if (fraction.length > 2 || fraction.any { !it.isDigit() }) return null

        val wholeCents = whole.toLongOrNull()?.let { it * 100 } ?: return null
        val fractionCents = if (fraction.isEmpty()) 0L else fraction.padEnd(2, '0').toLong()
        val total = wholeCents + fractionCents

        return if (negative) -total else total
    }
}
