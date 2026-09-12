package com.aliJafari.bbarq.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.PlaceAvatar
import com.aliJafari.bbarq.ui.components.StatusPill
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.utils.relativeStatus

/**
 * Off-screen card rendered to a bitmap for sharing.
 *
 * Kept deliberately opaque and light-themed: it ends up in a chat app, not in
 * our surface stack, so glass effects and dark tints would look broken there.
 */
@Composable
fun ShareableScheduleCard(schedule: PlaceOutage) {
    val context = LocalContext.current
    val colorOption = placeColorOption(schedule.place.colorKey)
    val status = relativeStatus(schedule.outage, context)
    val unknown = stringResource(R.string.value_not_available)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .clip(RoundedCornerShape(Radius.xl))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.sm)
                .background(
                    Brush.horizontalGradient(
                        listOf(colorOption.color, colorOption.color.copy(alpha = 0.2f)),
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
                )
                PersianText(
                    text = schedule.place.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                StatusPill(status = status, pulse = false)
            }

            ShareMetaRow(
                iconRes = R.drawable.ic_calendar,
                text = schedule.outage.date?.takeIf { it.isNotBlank() } ?: unknown,
            )
            ShareMetaRow(
                iconRes = R.drawable.ic_clock,
                text = stringResource(
                    R.string.schedule_time_range,
                    schedule.outage.startTime?.takeIf { it.isNotBlank() } ?: unknown,
                    schedule.outage.endTime?.takeIf { it.isNotBlank() } ?: unknown,
                ),
            )
            ShareMetaRow(
                iconRes = R.drawable.ic_bolt,
                text = schedule.outage.reason?.takeIf { it.isNotBlank() } ?: unknown,
            )
            ShareMetaRow(
                iconRes = R.drawable.ic_location,
                text = schedule.outage.address?.takeIf { it.isNotBlank() } ?: unknown,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PersianText(
                text = "github.com/hesCalledAJ/B-Barq",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ShareMetaRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(IconSize.sm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PersianText(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
    }
}
