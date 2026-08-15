package com.expent.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand: emerald green, fitting for a money app.
val Emerald = Color(0xFF0E9F6E)
val EmeraldDark = Color(0xFF0B7A54)
val Mint = Color(0xFFB7F5D4)
val Forest = Color(0xFF073B27)

internal val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Forest,
    secondary = Color(0xFF4E6E5D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F4E0),
    onSecondaryContainer = Color(0xFF0B2A1D),
    tertiary = Color(0xFF00696E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9CF1F7),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFF8FAF8),
    surface = Color(0xFFF8FAF8),
    error = Color(0xFFBA1A1A)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF52D9A3),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Mint,
    secondary = Color(0xFFB4CCBB),
    onSecondary = Color(0xFF20352B),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFD0E8D6),
    tertiary = Color(0xFF80D4DA),
    onTertiary = Color(0xFF00373A),
    tertiaryContainer = Color(0xFF004F53),
    onTertiaryContainer = Color(0xFF9CF1F7),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    error = Color(0xFFFFB4AB)
)
