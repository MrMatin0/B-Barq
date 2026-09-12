package com.aliJafari.bbarq.ui.screens.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.AppLanguage
import com.aliJafari.bbarq.ui.components.BBarqButton
import com.aliJafari.bbarq.ui.components.NoticeCard
import com.aliJafari.bbarq.ui.components.NoticeTone
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.SectionHeader
import com.aliJafari.bbarq.ui.components.SettingsRow
import com.aliJafari.bbarq.ui.components.SettingsToggleRow
import com.aliJafari.bbarq.ui.main.AppPermission
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * Preferences tab.
 *
 * Stateless: it renders [PreferencesUiState] plus the list of permissions the
 * shell says are missing, and reports back through [onEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    missingPermissions: List<AppPermission>,
    contentPadding: PaddingValues,
    onEvent: (PreferencesEvent) -> Unit,
    onPermissionClick: (AppPermission) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLogoutDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(PreferencesEvent.DismissLogout) },
            title = {
                PersianText(
                    text = stringResource(R.string.action_logout),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                PersianText(
                    text = stringResource(R.string.logout_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(PreferencesEvent.ConfirmLogout) }) {
                    PersianText(
                        text = stringResource(R.string.yes),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PreferencesEvent.DismissLogout) }) {
                    PersianText(text = stringResource(R.string.cancel))
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.sm,
            bottom = Spacing.listBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (missingPermissions.isNotEmpty()) {
            item(key = "permissions-header") {
                SectionHeader(title = stringResource(R.string.permissions_section_title))
            }
            items(missingPermissions, key = { it.name }) { permission ->
                NoticeCard(
                    title = stringResource(permission.titleRes),
                    message = stringResource(permission.subtitleRes),
                    iconRes = permission.iconRes,
                    tone = NoticeTone.Warning,
                    actionLabel = stringResource(R.string.permission_request_cta_text),
                    onAction = { onPermissionClick(permission) },
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
        }

        item(key = "places-header") {
            SectionHeader(
                title = stringResource(R.string.places_section_title),
                trailing = {
                    PersianText(
                        text = stringResource(R.string.places_count, state.places.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        if (state.places.isEmpty()) {
            item(key = "places-empty") {
                PersianText(
                    text = stringResource(R.string.empty_places_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // One lazy item on purpose: the reorderable column translates its
            // own rows, which lazy layout would otherwise reposition mid-drag.
            item(key = "places-list") {
                ReorderablePlaceList(
                    places = state.places,
                    onMove = { from, to -> onEvent(PreferencesEvent.MovePlace(from, to)) },
                    onEdit = { onEvent(PreferencesEvent.EditPlace(it)) },
                    onDelete = { onEvent(PreferencesEvent.DeletePlace(it)) },
                )
            }

            if (state.canReorderPlaces) {
                item(key = "reorder-hint") {
                    PersianText(
                        text = stringResource(R.string.reorder_places_hint),
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "add-place") {
            BBarqButton(
                text = stringResource(R.string.add_new_place),
                onClick = { onEvent(PreferencesEvent.AddPlace) },
                modifier = Modifier.padding(vertical = Spacing.sm),
                iconRes = R.drawable.ic_add_place,
                fillWidth = true,
            )
        }

        item(key = "appearance-header") {
            SectionHeader(title = stringResource(R.string.appearance_section_title))
        }
        item(key = "dark-mode") {
            SettingsToggleRow(
                title = stringResource(R.string.dark_mode_title),
                checked = state.darkMode,
                onCheckedChange = { onEvent(PreferencesEvent.SetDarkMode(it)) },
                subtitle = stringResource(R.string.dark_mode_subtitle),
                iconRes = R.drawable.ic_bed,
            )
        }
        item(key = "language") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    PersianText(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PersianText(
                        text = stringResource(R.string.language_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEachIndexed { index, lang ->
                        SegmentedButton(
                            selected = state.language == lang,
                            onClick = { onEvent(PreferencesEvent.SetLanguage(lang)) },
                            shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                        ) {
                            PersianText(text = lang.nativeName, maxLines = 1)
                        }
                    }
                }
            }
        }

        item(key = "account-header") {
            SectionHeader(title = stringResource(R.string.account_section_title))
        }
        item(key = "about") {
            SettingsRow(
                title = stringResource(R.string.action_about),
                subtitle = stringResource(R.string.about_subtitle),
                iconRes = R.drawable.ic_about,
                onClick = { onEvent(PreferencesEvent.OpenAbout) },
            )
        }
        item(key = "logout") {
            SettingsRow(
                title = stringResource(R.string.action_logout),
                subtitle = stringResource(R.string.logout_card_subtitle),
                iconRes = R.drawable.ic_delete,
                iconTint = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onEvent(PreferencesEvent.RequestLogout) },
            )
        }
        item(key = "logout-tip") {
            PersianText(
                text = stringResource(R.string.action_logout_tip),
                modifier = Modifier.padding(horizontal = Spacing.xs),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "version") {
            PersianText(
                text = stringResource(R.string.version, state.versionName),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xl),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
