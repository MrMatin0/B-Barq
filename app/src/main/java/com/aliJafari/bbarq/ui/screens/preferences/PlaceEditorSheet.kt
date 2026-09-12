package com.aliJafari.bbarq.ui.screens.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.ui.components.AnimatedFilterChip
import com.aliJafari.bbarq.ui.components.BBarqButton
import com.aliJafari.bbarq.ui.components.BBarqButtonTone
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.SectionHeader
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.PlaceColorOptions
import com.aliJafari.bbarq.ui.theme.PlaceIconOptions
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.utils.ReminderOffset
import com.aliJafari.bbarq.utils.digitsOnly

private const val BILL_ID_LENGTH = 13

/**
 * Add / edit a place.
 *
 * Same fields as before, but the sheet now explains itself, counts the bill id
 * digits as you type, animates every selection and keeps its actions reachable
 * above the navigation bar.
 */
@Composable
fun PlaceEditorSheet(
    place: Place?,
    onCancel: () -> Unit,
    onSave: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    var name by remember(place) { mutableStateOf(place?.name.orEmpty()) }
    var billId by remember(place) { mutableStateOf(place?.billId.orEmpty()) }
    var colorKey by remember(place) { mutableStateOf(place?.colorKey ?: PlaceColorOptions.first().key) }
    var iconKey by remember(place) { mutableStateOf(place?.iconKey ?: PlaceIconOptions.first().key) }
    var remindersEnabled by remember(place) { mutableStateOf(place?.remindersEnabled ?: false) }
    var reminderMask by remember(place) { mutableIntStateOf(place?.reminderOffsetsMask ?: 0) }

    val nameError = name.isBlank()
    val billIdError = billId.length != BILL_ID_LENGTH
    val canSave = !nameError && !billIdError

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            PersianText(
                text = stringResource(
                    if (place == null) R.string.add_place_title else R.string.edit_place_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            PersianText(
                text = stringResource(
                    if (place == null) {
                        R.string.place_editor_subtitle_add
                    } else {
                        R.string.place_editor_subtitle_edit
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { PersianText(stringResource(R.string.place_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = nameError && name.isNotEmpty(),
        )

        OutlinedTextField(
            value = billId,
            onValueChange = { billId = it.digitsOnly(BILL_ID_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = { PersianText(stringResource(R.string.place_bill_id)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = billId.isNotEmpty() && billIdError,
            supportingText = {
                PersianText(
                    text = if (billId.isNotEmpty() && billIdError) {
                        stringResource(R.string.field_error_invalid_id_count)
                    } else {
                        stringResource(R.string.bill_id_helper)
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            trailingIcon = {
                PersianText(
                    text = stringResource(R.string.bill_id_counter, billId.length, BILL_ID_LENGTH),
                    modifier = Modifier.padding(end = Spacing.md),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (billIdError) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold,
                )
            },
        )

        SectionHeader(title = stringResource(R.string.place_color))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlaceColorOptions.forEach { option ->
                val selected = colorKey == option.key
                val swatchSize by animateDpAsState(
                    targetValue = if (selected) IconSize.avatar else IconSize.xl,
                    label = "swatchSize",
                )
                Box(
                    modifier = Modifier
                        .size(IconSize.avatar + Spacing.xs)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            colorKey = option.key
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(swatchSize)
                            .clip(CircleShape)
                            .background(option.color),
                    )
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.md),
                            tint = option.onColor,
                        )
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.place_icon))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlaceIconOptions.forEach { option ->
                val selected = iconKey == option.key
                val background by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    label = "iconTileBackground",
                )
                Box(
                    modifier = Modifier
                        .size(IconSize.avatar + Spacing.xs)
                        .clip(CircleShape)
                        .background(background)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            iconKey = option.key
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.md),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                PersianText(
                    text = stringResource(R.string.place_reminders_switch_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                PersianText(
                    text = stringResource(R.string.place_reminders_switch_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    remindersEnabled = checked
                    if (!checked) reminderMask = 0
                },
            )
        }

        AnimatedVisibility(
            visible = remindersEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PersianText(
                    text = stringResource(R.string.place_reminders_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReminderOffset.entries.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        rowOptions.forEach { offset ->
                            val checked = reminderMask and offset.bit != 0
                            AnimatedFilterChip(
                                label = stringResource(offset.labelRes),
                                selected = checked,
                                onClick = {
                                    reminderMask = reminderMask xor offset.bit
                                    if (reminderMask == 0) remindersEnabled = false
                                },
                                modifier = Modifier.weight(1f),
                                centerLabel = true,
                                iconRes = if (checked) R.drawable.ic_check else null,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm, bottom = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BBarqButton(
                text = stringResource(R.string.cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                tone = BBarqButtonTone.Outline,
            )
            BBarqButton(
                text = stringResource(R.string.save_button_text),
                onClick = {
                    onSave(
                        Place(
                            id = place?.id ?: 0,
                            name = name.trim(),
                            billId = billId,
                            colorKey = colorKey,
                            iconKey = iconKey,
                            reminderOffsetsMask = if (remindersEnabled) reminderMask else 0,
                            // Not editable here, but it must survive the round
                            // trip: rebuilding it as 0 would drag the place to
                            // the top of the list on every save.
                            sortOrder = place?.sortOrder ?: 0,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = canSave,
            )
        }
    }
}
