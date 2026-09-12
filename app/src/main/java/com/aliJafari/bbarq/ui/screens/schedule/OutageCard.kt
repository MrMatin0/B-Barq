package com.aliJafari.bbarq.ui.screens.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.ui.components.GlassCard
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.PlaceAvatar
import com.aliJafari.bbarq.ui.components.StatusPill
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.ui.theme.statusColors
import com.aliJafari.bbarq.utils.ScheduleUrgency
import com.aliJafari.bbarq.utils.durationMinutes
import com.aliJafari.bbarq.utils.progressFraction
import com.aliJafari.bbarq.utils.relativeStatus

/**
 * The hero of the app.
 *
 * The old card was four identical icon+text rows with a grey "Share" strip at
 * the bottom, so nothing told you whether the power was about to go out or
 * whether the entry was a week away. This version leads with the countdown,
 * colours itself by urgency, shows how far through a running outage you are,
 * and hides secondary metadata behind a tap.
 */
@Composable
fun OutageCard(
    schedule: PlaceOutage,
    now: Long,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colorOption = placeColorOption(schedule.place.colorKey)

    val status = remember(schedule, now) { relativeStatus(schedule.outage, context) }
    val colors = statusColors(status.urgency)
    val progress = remember(schedule, now) { schedule.outage.progressFraction(now) }
    val duration = remember(schedule) { schedule.outage.durationMinutes() }

    var expanded by rememberSaveable(schedule.outage.id, schedule.place.id) { mutableStateOf(false) }

    val headline = when (status.urgency) {
        ScheduleUrgency.ONGOING -> stringResource(R.string.outage_in_progress)
        ScheduleUrgency.ENDED -> stringResource(R.string.outage_finished)
        else -> stringResource(R.string.outage_countdown, status.label)
    }
    val unknown = stringResource(R.string.value_not_available)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        accent = colorOption.color,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            expanded = !expanded
        },
        contentPadding = PaddingValues(Spacing.none),
    ) {
        // Identity stripe: the place colour is the first thing you see.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.xs)
                .background(
                    Brush.horizontalGradient(
                        listOf(colorOption.color, colorOption.color.copy(alpha = 0.12f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                PlaceAvatar(
                    colorKey = schedule.place.colorKey,
                    iconKey = schedule.place.iconKey,
                    size = IconSize.avatar - Spacing.xs,
                )
                PersianText(
                    text = schedule.place.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                StatusPill(status = status)
            }

            OutageHeadline(text = headline, urgency = status.urgency)

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.sm)
                        .clip(RoundedCornerShape(Radius.pill)),
                    color = colors.accent,
                    trackColor = colors.container,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaChip(
                    iconRes = R.drawable.ic_calendar,
                    text = schedule.outage.date?.takeIf { it.isNotBlank() } ?: unknown,
                )
                MetaChip(
                    iconRes = R.drawable.ic_clock,
                    text = stringResource(
                        R.string.schedule_time_range,
                        schedule.outage.startTime?.takeIf { it.isNotBlank() } ?: unknown,
                        schedule.outage.endTime?.takeIf { it.isNotBlank() } ?: unknown,
                    ),
                )
            }

            if (duration != null) {
                val durationText = if (duration >= 60) {
                    stringResource(R.string.duration_hours_minutes, duration / 60, duration % 60)
                } else {
                    stringResource(R.string.duration_minutes, duration)
                }
                MetaChip(
                    iconRes = R.drawable.ic_bolt,
                    text = stringResource(
                        R.string.schedule_time_range,
                        stringResource(R.string.label_duration),
                        durationText,
                    ),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DetailRow(
                        label = stringResource(R.string.outage_reason),
                        value = schedule.outage.reason?.takeIf { it.isNotBlank() } ?: unknown,
                    )
                    DetailRow(
                        label = stringResource(R.string.outage_address),
                        value = schedule.outage.address?.takeIf { it.isNotBlank() } ?: unknown,
                    )
                }
            }

            ShareRow(accentColorKey = schedule.place.colorKey, onShare = onShare)
        }
    }
}

@Composable
private fun OutageHeadline(
    text: String,
    urgency: ScheduleUrgency,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(urgency)
    val live = urgency == ScheduleUrgency.ONGOING || urgency == ScheduleUrgency.SOON
    val transition = rememberInfiniteTransition(label = "headlineGlow")
    val bloom by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "headlineBloom",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(if (live) IconSize.md else IconSize.sm)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.accent,
                            colors.glow.copy(alpha = if (live) 0.15f + 0.55f * bloom else 0.2f),
                        ),
                    ),
                ),
        )
        PersianText(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = colors.content,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
    }
}

@Composable
private fun MetaChip(
    iconRes: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xxs),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PersianText(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        PersianText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        PersianText(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ShareRow(
    accentColorKey: String,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = placeColorOption(accentColorKey).color
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(accent.copy(alpha = 0.12f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShare()
            }
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bill),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        PersianText(
            text = stringResource(R.string.share),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}
