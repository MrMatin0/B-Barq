package com.aliJafari.bbarq.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.ui.theme.statusColors
import com.aliJafari.bbarq.utils.ScheduleStatus
import com.aliJafari.bbarq.utils.ScheduleUrgency

/**
 * Status chip for an outage.
 *
 * Replaces StatusBadge and its hardcoded ARGB literals. Active and imminent
 * outages get a breathing halo dot instead of a static icon, which reads as
 * "live" at a glance without any text.
 */
@Composable
fun StatusPill(
    status: ScheduleStatus,
    modifier: Modifier = Modifier,
    pulse: Boolean = true,
) {
    val colors = statusColors(status.urgency)
    val live = pulse &&
        (status.urgency == ScheduleUrgency.ONGOING || status.urgency == ScheduleUrgency.SOON)

    val transition = rememberInfiniteTransition(label = "statusPulse")
    val bloom by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusBloom",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(colors.container)
            .padding(horizontal = Spacing.sm + Spacing.xxs, vertical = Spacing.xs + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
    ) {
        if (live) {
            Box(
                modifier = Modifier
                    .size(IconSize.sm)
                    .clip(CircleShape)
                    .background(colors.glow.copy(alpha = 0.18f + 0.62f * bloom)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(Spacing.sm)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        } else {
            Icon(
                painter = painterResource(colors.iconRes),
                contentDescription = null,
                modifier = Modifier.size(IconSize.xs),
                tint = colors.content,
            )
        }
        PersianText(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.content,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
