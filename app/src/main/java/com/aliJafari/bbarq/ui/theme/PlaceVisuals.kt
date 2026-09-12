package com.aliJafari.bbarq.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.aliJafari.bbarq.R

/**
 * Place identity tokens.
 *
 * Every swatch carries an explicit [ColorOption.onColor]: the old palette tinted
 * icons white unconditionally, which made the light swatches unreadable.
 */
@Immutable
data class ColorOption(
    val key: String,
    val color: Color,
    val onColor: Color = Color.White,
)

@Immutable
data class IconOption(val key: String, val icon: Int)

val PlaceColorOptions: List<ColorOption> = listOf(
    ColorOption("blue", Color(0xFF4C6EF5)),
    ColorOption("red", Color(0xFFCE1A2F)),
    ColorOption("teal", Color(0xFF12B886)),
    ColorOption("amber", Color(0xFFF59F00), Color(0xFF3A2600)),
    ColorOption("idk", Color(0xFFE64980)),
    ColorOption("purple", Color(0xFF9775FA)),
    ColorOption("green", Color(0xFF66A80F)),
    ColorOption("cyan", Color(0xFF15AABF)),
    ColorOption("gray", Color(0xFF868E96)),
    ColorOption("white", Color(0xFFF3F3F3), Color(0xFF2B2723)),
)

val PlaceIconOptions: List<IconOption> = listOf(
    IconOption("home", R.drawable.ic_home),
    IconOption("business", R.drawable.ic_work),
    IconOption("apartment", R.drawable.ic_apartment),
    IconOption("university", R.drawable.ic_university),
    IconOption("store", R.drawable.ic_shop),
    IconOption("fridge", R.drawable.ic_fridge),
    IconOption("game", R.drawable.ic_game),
    IconOption("warehouse", R.drawable.ic_garage),
    IconOption("server", R.drawable.ic_server),
)

fun placeColorOption(key: String): ColorOption =
    PlaceColorOptions.firstOrNull { it.key == key } ?: PlaceColorOptions.first()

fun placeIconOption(key: String): IconOption =
    PlaceIconOptions.firstOrNull { it.key == key } ?: PlaceIconOptions.first()
