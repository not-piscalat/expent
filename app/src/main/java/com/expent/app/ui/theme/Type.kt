package com.expent.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expent.app.R

/**
 * Expent's type voice: a modern grotesque for structure, the system sans for
 * reading, and monospace for money. Figures are set like entries in a register —
 * tabular, unambiguous, and visibly different from the words around them.
 */
private val Sans = FontFamily.SansSerif
val MoneyFigures = FontFamily.Monospace

/** The display face: Space Grotesk — modern, slightly technical, unmistakable. */
val Grotesk = FontFamily(
    Font(R.font.space_grotesk, weight = FontWeight.Bold)
)

/** Oversized display figures for the hero and headlines. */
val DisplayBig = TextStyle(
    fontFamily = Grotesk,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.3).sp
)

/** Section and card titles. */
val TitleStrong = TextStyle(
    fontFamily = Grotesk,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp
)

/** The oversized running-total figure on the Home hero. */
val MoneyDisplay = TextStyle(
    fontFamily = MoneyFigures,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1).sp
)

/** Mid-size money figures (card subtotals, rows). */
val MoneyMedium = TextStyle(
    fontFamily = MoneyFigures,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp
)

/** Small money figures (captions, budget footnotes). */
val MoneySmall = TextStyle(
    fontFamily = MoneyFigures,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp
)

/** The figure being written into the ledger: the amount input on entry forms. */
val MoneyInput = TextStyle(
    fontFamily = MoneyFigures,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.5).sp
)

val ExpentTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp
    )
)
