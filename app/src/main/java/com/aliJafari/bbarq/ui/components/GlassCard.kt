package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.ui.theme.LocalIsDarkTheme
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * The app's primary container.
 *
 * A flat [androidx.compose.material3.ElevatedCard] made every list look like a
 * stack of grey bricks. This uses a vertical tonal gradient plus a gradient
 * hairline border to fake depth without drop shadows, optionally tinted by an
 * [accent] so a place's identity colour bleeds subtly into its own card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.985f else 1f, label = "glassScale")

    val tint = accent ?: scheme.primary
    val fill = remember(dark, tint, scheme.surfaceContainerHigh, scheme.surfaceContainerLow) {
        Brush.verticalGradient(
            listOf(
                scheme.surfaceContainerHigh,
                tint.copy(alpha = if (dark) 0.10f else 0.07f).compositeOver(scheme.surfaceContainerLow),
            ),
        )
    }
    val borderBrush = remember(dark, tint, scheme.outlineVariant) {
        Brush.linearGradient(
            listOf(
                tint.copy(alpha = if (dark) 0.40f else 0.28f),
                scheme.outlineVariant.copy(alpha = if (dark) 0.24f else 0.50f),
                Color.Transparent,
            ),
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill)
            .border(1.dp, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
