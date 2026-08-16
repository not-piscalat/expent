package com.expent.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Expent's palette: light and dark purple. Light theme is lavender-paper with a
// deep violet primary; dark theme is plum-night with a light lavender primary.
// The violet is deliberately bluer and brighter than Material's default purple
// so it reads as Expent's, not the framework's.

// Light theme
val Violet = Color(0xFF6C4BD1)
val VioletDeep = Color(0xFF3E2A7D)
val LavenderMist = Color(0xFFE8E1FA)
val LavenderPaper = Color(0xFFF1EEF9)
val LavenderLine = Color(0xFFD9D2EA)
val Amethyst = Color(0xFF8E4FA8)
val InkViolet = Color(0xFF211D2E)
val MutedViolet = Color(0xFF5B5769)

// Dark theme
val PlumNight = Color(0xFF161123)
val PlumSurface = Color(0xFF1E1831)
val PlumRaised = Color(0xFF262042)
val Lavender = Color(0xFFC5B3F5)
val LavenderBright = Color(0xFFE7DEFF)
val PlumContainer = Color(0xFF44307E)
val PlumMuted = Color(0xFFB3ADC6)

internal val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = LavenderMist,
    onPrimaryContainer = VioletDeep,
    secondary = Amethyst,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E4F7),
    onSecondaryContainer = Color(0xFF381C41),
    tertiary = Color(0xFF7D5AA6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF2E1445),
    background = LavenderPaper,
    surface = Color(0xFFFCFBFE),
    surfaceVariant = Color(0xFFECE8F6),
    onSurface = InkViolet,
    onSurfaceVariant = MutedViolet,
    outline = LavenderLine,
    outlineVariant = Color(0xFFE4DFF1),
    error = Color(0xFFBA1A1A)
)

internal val DarkColors = darkColorScheme(
    primary = Lavender,
    onPrimary = Color(0xFF2E1E5E),
    primaryContainer = PlumContainer,
    onPrimaryContainer = LavenderBright,
    secondary = Color(0xFFD7A9EC),
    onSecondary = Color(0xFF3B1B45),
    secondaryContainer = Color(0xFF553361),
    onSecondaryContainer = Color(0xFFF6D9FF),
    tertiary = Color(0xFFCBB7F0),
    onTertiary = Color(0xFF322553),
    tertiaryContainer = Color(0xFF4A3B6E),
    onTertiaryContainer = Color(0xFFE9DEFF),
    background = PlumNight,
    surface = PlumSurface,
    surfaceVariant = Color(0xFF2B2540),
    onSurface = Color(0xFFEDE8FB),
    onSurfaceVariant = PlumMuted,
    outline = Color(0xFF4A435F),
    outlineVariant = Color(0xFF37314D),
    error = Color(0xFFFFB4AB)
)
