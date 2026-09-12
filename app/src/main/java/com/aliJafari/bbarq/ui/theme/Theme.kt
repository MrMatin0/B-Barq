package com.aliJafari.bbarq.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * B-Barq theme.
 *
 * Dynamic colour is opt-in rather than default: the brand palette carries the
 * "electricity" semantics of the product, and wallpaper-derived schemes were
 * washing the outage status colours out.
 */
@Composable
fun BBarqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> BBarqDarkColors
        else -> BBarqLightColors
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalStatusPalette provides if (darkTheme) DarkStatusPalette else LightStatusPalette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BBarqShapes,
            content = content,
        )
    }
}
