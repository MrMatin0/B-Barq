package com.aliJafari.bbarq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aliJafari.bbarq.R

/**
 * The app ships a Persian face (Vazir / IRANSans style) in two files. Compose
 * needs a weight for every style it resolves, otherwise it synthesises one and
 * Persian glyphs get visibly smeared. We map the whole weight range onto the
 * two real files so nothing is ever faux-bolded.
 */
val BBarqFontFamily = FontFamily(
    Font(R.font.font_reg, FontWeight.Light),
    Font(R.font.font_reg, FontWeight.Normal),
    Font(R.font.font_reg, FontWeight.Medium),
    Font(R.font.font_bold, FontWeight.SemiBold),
    Font(R.font.font_bold, FontWeight.Bold),
    Font(R.font.font_bold, FontWeight.ExtraBold),
)

/** Kept for backwards compatibility with older references. */
val AppFontFamily = BBarqFontFamily

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = BBarqFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Expressive type scale. Persian needs ~1.5x line height to breathe, and near
 * zero letter spacing because tracking breaks joined glyph forms.
 */
val Typography = Typography(
    displayLarge = style(52, 64, FontWeight.Bold, -0.5),
    displayMedium = style(42, 52, FontWeight.Bold, -0.25),
    displaySmall = style(34, 44, FontWeight.Bold),

    headlineLarge = style(30, 40, FontWeight.Bold),
    headlineMedium = style(26, 36, FontWeight.Bold),
    headlineSmall = style(22, 32, FontWeight.SemiBold),

    titleLarge = style(20, 30, FontWeight.SemiBold),
    titleMedium = style(17, 26, FontWeight.SemiBold),
    titleSmall = style(15, 24, FontWeight.SemiBold),

    bodyLarge = style(16, 26),
    bodyMedium = style(14, 23),
    bodySmall = style(12, 20),

    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 18, FontWeight.SemiBold),
    labelSmall = style(11, 16, FontWeight.Medium),
)
