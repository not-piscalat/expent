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

    fun format(
        cents: Long,
        locale: Locale = Locale.getDefault(),
        symbol: String = PESO_SYMBOL
    ): String {
        val sign = if (cents < 0) "-" else ""
        return sign + symbol + String.format(locale, "%,.2f", abs(cents) / 100.0)
    }

    /** Formats cents as an amount-input string (no grouping, no symbol), e.g. 123_456 -> "1234.56". */
    fun toInput(cents: Long): String {
        val absCents = abs(cents)
        val whole = absCents / 100
        val fraction = absCents % 100
        return if (fraction == 0L) {
            whole.toString()
        } else {
            "$whole.${fraction.toString().padStart(2, '0')}"
        }
    }

    /**
     * Keeps a typed amount field sane: digits only, one dot, at most two decimals.
     */
    fun sanitizeInput(input: String): String {
        val result = StringBuilder()
        var dotSeen = false
        var decimals = 0
        for (c in input) {
            when {
                c.isDigit() && decimals < 2 -> {
                    result.append(c)
                    if (dotSeen) decimals++
                }
                c == '.' && !dotSeen -> {
                    result.append('.')
                    dotSeen = true
                }
                else -> Unit // commas and anything else are dropped
            }
        }
        return result.toString()
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
