package com.aliJafari.bbarq.ui.screens.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.ui.components.GlassCard
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.PlaceAvatar
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.utils.ReminderOffset

/** A tracked place, with its identity colour bleeding into the card. */
@Composable
fun PlaceCard(
    place: Place,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val activeReminders = ReminderOffset.entries.count { place.reminderOffsetsMask and it.bit != 0 }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        accent = placeColorOption(place.colorKey).color,
        onClick = onEditClick,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PlaceAvatar(colorKey = place.colorKey, iconKey = place.iconKey)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                PersianText(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                PersianText(
                    text = stringResource(R.string.place_id_line, place.billId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                PersianText(
                    text = if (activeReminders > 0) {
                        stringResource(R.string.reminders_count, activeReminders)
                    } else {
                        stringResource(R.string.reminders_off)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeReminders > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }

            FilledTonalIconButton(
                onClick = onEditClick,
                modifier = Modifier.size(IconSize.avatar - Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_place),
                    modifier = Modifier.size(IconSize.md - Spacing.xs),
                )
            }
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteClick()
                },
                modifier = Modifier.size(IconSize.avatar - Spacing.sm),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_place),
                    modifier = Modifier.size(IconSize.md - Spacing.xs),
                )
            }
        }
    }
}
