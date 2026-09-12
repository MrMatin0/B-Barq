package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing

/** Sweeping gradient used by all skeleton placeholders. */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return remember(progress, base, highlight) {
        val shift = progress * 1200f
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(shift - 420f, 0f),
            end = Offset(shift, 420f),
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.sm),
    brush: Brush = rememberShimmerBrush(),
) {
    Box(modifier = modifier.clip(shape).background(brush))
}

/**
 * Skeleton stand-in for an outage card.
 *
 * Replaces the indeterminate LinearProgressIndicator that used to sit above the
 * list: users now see the shape of the content that is coming.
 */
@Composable
fun OutageCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ShimmerBox(Modifier.size(IconSize.avatar), CircleShape, brush)
            ShimmerBox(Modifier.fillMaxWidth(0.45f).height(Spacing.lg), brush = brush)
            Box(Modifier.weight(1f))
            ShimmerBox(Modifier.size(width = Spacing.huge + Spacing.lg, height = Spacing.xxl), RoundedCornerShape(Radius.pill), brush)
        }
        Box(Modifier.height(Spacing.lg))
        ShimmerBox(Modifier.fillMaxWidth(0.7f).height(Spacing.xxl), RoundedCornerShape(Radius.sm), brush)
        Box(Modifier.height(Spacing.md))
        ShimmerBox(Modifier.fillMaxWidth().height(Spacing.md), brush = brush)
        Box(Modifier.height(Spacing.sm))
        ShimmerBox(Modifier.fillMaxWidth(0.55f).height(Spacing.md), brush = brush)
    }
}
