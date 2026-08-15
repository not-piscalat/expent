package com.expent.app.core

/** Currencies users can display amounts in. [NONE] hides the symbol entirely. */
enum class CurrencyOption(val code: String, val symbol: String) {
    PHP("PHP", "₱"),
    USD("USD", "$"),
    NONE("NONE", "");

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: PHP
    }
}
