package com.expent.app.core

/** Theme preference. [SYSTEM] follows the device setting. */
enum class ThemeOption(val code: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    fun resolvesToDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromCode(code: String?): ThemeOption =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}
