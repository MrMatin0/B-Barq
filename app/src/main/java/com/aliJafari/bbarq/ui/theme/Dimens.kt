package com.aliJafari.bbarq.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens on a strict 4dp baseline grid.
 * Nothing in the UI layer should hardcode a dp value for layout gaps.
 */
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    /** Screen edge gutter. */
    val gutter = 16.dp

    /** Bottom inset so list content clears the FAB and the navigation bar. */
    val listBottom = 120.dp
}

/** Corner radius tokens. Expressive means generous, consistent rounding. */
object Radius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 26.dp
    val xxl = 34.dp
    val pill = 100.dp
}

/** Elevation tokens (tonal + shadow). */
object Elevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 10.dp
}

/** Icon sizing tokens. */
object IconSize {
    val xs = 14.dp
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 32.dp
    val avatar = 44.dp
}

val BBarqShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
