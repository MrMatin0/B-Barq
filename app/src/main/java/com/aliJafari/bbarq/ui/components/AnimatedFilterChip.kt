package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * Selectable chip with animated container, border and press feedback.
 *
 * Used for the per-place schedule filter and for reminder offsets, so both
 * surfaces animate identically instead of one using FilterChip and the other a
 * hand-rolled Surface.
 */
@Composable
fun AnimatedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    iconRes: Int? = null,
    centerLabel: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val container by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.18f) else scheme.surfaceContainerHigh,
        label = "chipContainer",
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.80f) else scheme.outlineVariant.copy(alpha = 0.55f),
        label = "chipBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
        label = "chipContent",
    )
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "chipScale")

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(Radius.pill))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(Radius.pill))
            .clickable(interactionSource = interaction, indication = ripple()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = Spacing.md + Spacing.xxs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centerLabel) {
            Arrangement.spacedBy(Spacing.xs + Spacing.xxs, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(Spacing.xs + Spacing.xxs)
        },
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(IconSize.sm),
                tint = if (selected) accent else content,
            )
        }
        PersianText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
