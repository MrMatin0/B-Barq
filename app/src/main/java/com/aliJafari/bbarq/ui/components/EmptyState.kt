package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * Empty / all-clear state.
 *
 * The old version was a bare centred string. This gives it a glowing vector
 * badge and a scale-in entrance so "no outages today" feels like good news
 * rather than a failed load.
 */
@Composable
fun BBarqEmptyState(
    iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    var appeared by remember { mutableStateOf(false) }
    val badgeScale by animateFloatAsState(if (appeared) 1f else 0.82f, label = "emptyScale")
    val contentAlpha by animateFloatAsState(if (appeared) 1f else 0f, label = "emptyAlpha")
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxxl, horizontal = Spacing.lg)
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.huge * 2)
                .graphicsLayer {
                    scaleX = badgeScale
                    scaleY = badgeScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha = 0.26f),
                            accent.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.huge + Spacing.lg)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.xl),
                    tint = accent,
                )
            }
        }

        PersianText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            PersianText(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Box(Modifier.padding(top = Spacing.sm)) {
                BBarqButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}
