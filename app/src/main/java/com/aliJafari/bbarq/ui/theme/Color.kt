package com.aliJafari.bbarq.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * B-Barq raw palette.
 *
 * The product is about power outages, so the brand hue is an "electric amber",
 * balanced by a cool indigo secondary and a calm teal tertiary. Neutrals are
 * warm so dark mode reads like a dimmed room instead of a cold slab.
 */

// ---- Brand: electric amber ----
val Amber10 = Color(0xFF2A1800)
val Amber20 = Color(0xFF472A00)
val Amber30 = Color(0xFF693D00)
val Amber40 = Color(0xFF8F5300)
val Amber50 = Color(0xFFB86B00)
val Amber60 = Color(0xFFE08800)
val Amber70 = Color(0xFFFFB95C)
val Amber80 = Color(0xFFFFCE8C)
val Amber90 = Color(0xFFFFDDB3)
val Amber95 = Color(0xFFFFEFD9)

// ---- Secondary: indigo ----
val Indigo10 = Color(0xFF0C132E)
val Indigo20 = Color(0xFF1B2649)
val Indigo30 = Color(0xFF323D60)
val Indigo40 = Color(0xFF4A5578)
val Indigo80 = Color(0xFFB4C5FF)
val Indigo90 = Color(0xFFDBE2FF)

// ---- Tertiary: teal ----
val Teal10 = Color(0xFF00201F)
val Teal20 = Color(0xFF00363A)
val Teal30 = Color(0xFF004F53)
val Teal40 = Color(0xFF00696E)
val Teal80 = Color(0xFF80D4DA)
val Teal90 = Color(0xFF9CF1F6)

// ---- Warm neutrals ----
val Neutral06 = Color(0xFF100E0A)
val Neutral10 = Color(0xFF16130F)
val Neutral12 = Color(0xFF1F1B16)
val Neutral17 = Color(0xFF231F1A)
val Neutral22 = Color(0xFF2E2924)
val Neutral26 = Color(0xFF39342E)
val Neutral30 = Color(0xFF4F4539)
val Neutral34 = Color(0xFF34302A)
val Neutral50 = Color(0xFF827568)
val Neutral60 = Color(0xFF9C8F80)
val Neutral80 = Color(0xFFD3C4B4)
val Neutral88 = Color(0xFFE9E1D9)
val Neutral90 = Color(0xFFEBE1D9)
val Neutral92 = Color(0xFFEFE7DF)
val Neutral94 = Color(0xFFF4EDE5)
val Neutral96 = Color(0xFFFAF3EC)
val Neutral98 = Color(0xFFFDF8F4)
val NeutralVariantLight = Color(0xFFEFE0D0)

// ---- Error ----
val Red10 = Color(0xFF410E0B)
val Red20 = Color(0xFF690005)
val Red30 = Color(0xFF93000A)
val Red40 = Color(0xFFB3261E)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

/**
 * Aliases for the names the old Compose template shipped with, so any stray
 * reference keeps compiling while pointing at the new brand colours.
 */
val Purple80 = Amber70
val PurpleGrey80 = Indigo80
val Pink80 = Teal80
val Purple40 = Amber40
val PurpleGrey40 = Indigo40
val Pink40 = Teal40

internal val BBarqLightColors = lightColorScheme(
    primary = Amber40,
    onPrimary = Color.White,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,
    inversePrimary = Amber70,
    secondary = Indigo40,
    onSecondary = Color.White,
    secondaryContainer = Indigo90,
    onSecondaryContainer = Indigo20,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral98,
    onBackground = Neutral12,
    surface = Neutral98,
    onSurface = Neutral12,
    surfaceVariant = NeutralVariantLight,
    onSurfaceVariant = Neutral30,
    surfaceTint = Amber40,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE2DAD1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral88,
    outline = Neutral50,
    outlineVariant = Neutral80,
    inverseSurface = Neutral34,
    inverseOnSurface = Color(0xFFF8EFE7),
    scrim = Color.Black,
)

internal val BBarqDarkColors = darkColorScheme(
    primary = Amber70,
    onPrimary = Amber20,
    primaryContainer = Amber30,
    onPrimaryContainer = Amber90,
    inversePrimary = Amber40,
    secondary = Indigo80,
    onSecondary = Indigo20,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Indigo90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    surfaceTint = Amber70,
    surfaceBright = Color(0xFF3D3831),
    surfaceDim = Neutral10,
    surfaceContainerLowest = Neutral06,
    surfaceContainerLow = Neutral12,
    surfaceContainer = Neutral17,
    surfaceContainerHigh = Neutral22,
    surfaceContainerHighest = Neutral26,
    outline = Neutral60,
    outlineVariant = Neutral30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral34,
    scrim = Color.Black,
)
