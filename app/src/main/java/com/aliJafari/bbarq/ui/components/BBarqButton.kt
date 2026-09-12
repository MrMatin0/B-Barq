package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing

enum class BBarqButtonTone { Primary, Tonal, Outline, Ghost, Danger }

/**
 * The single button of the app.
 *
 * Everything expressive lives here so screens never restyle a button inline:
 * pill shape, 52dp touch target, press-scale spring, haptic tick on tap and an
 * inline spinner that keeps the layout from jumping while work is in flight.
 */
@Composable
fun BBarqButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: BBarqButtonTone = BBarqButtonTone.Primary,
    iconRes: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "buttonScale")
    val haptic = LocalHapticFeedback.current

    val container = when (tone) {
        BBarqButtonTone.Primary -> scheme.primary
        BBarqButtonTone.Tonal -> scheme.secondaryContainer
        BBarqButtonTone.Outline, BBarqButtonTone.Ghost -> scheme.surface.copy(alpha = 0f)
        BBarqButtonTone.Danger -> scheme.errorContainer
    }
    val content = when (tone) {
        BBarqButtonTone.Primary -> scheme.onPrimary
        BBarqButtonTone.Tonal -> scheme.onSecondaryContainer
        BBarqButtonTone.Outline -> scheme.onSurface
        BBarqButtonTone.Ghost -> scheme.primary
        BBarqButtonTone.Danger -> scheme.onErrorContainer
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Radius.pill),
        color = container,
        contentColor = content,
        border = if (tone == BBarqButtonTone.Outline) {
            BorderStroke(1.dp, scheme.outlineVariant)
        } else {
            null
        },
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(IconSize.md),
                    color = content,
                    strokeWidth = 2.dp,
                )

                iconRes != null -> Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.md),
                )
            }
            PersianText(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
