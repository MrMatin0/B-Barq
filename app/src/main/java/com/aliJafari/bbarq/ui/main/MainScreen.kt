package com.aliJafari.bbarq.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.screens.preferences.PlaceEditorSheet
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesEvent
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesScreen
import com.aliJafari.bbarq.ui.screens.preferences.PreferencesUiState
import com.aliJafari.bbarq.ui.screens.schedule.ScheduleEvent
import com.aliJafari.bbarq.ui.screens.schedule.ScheduleScreen
import com.aliJafari.bbarq.ui.screens.schedule.ScheduleUiState
import com.aliJafari.bbarq.ui.theme.IconSize
import com.aliJafari.bbarq.ui.theme.Radius
import com.aliJafari.bbarq.ui.theme.Spacing

/**
 * App shell.
 *
 * Purely presentational: it wires three immutable states to two stateless
 * screens and forwards intents. No Android APIs, no business logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainState: MainUiState,
    scheduleState: ScheduleUiState,
    preferencesState: PreferencesUiState,
    onIntent: (MainIntent) -> Unit,
    onScheduleEvent: (ScheduleEvent) -> Unit,
    onPreferencesEvent: (PreferencesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        PersianText(
                            text = stringResource(mainState.selectedTab.titleRes),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        PersianText(
                            text = when (mainState.selectedTab) {
                                MainTab.Schedules -> stringResource(
                                    if (mainState.serviceRunning) {
                                        R.string.monitoring_active
                                    } else {
                                        R.string.monitoring_inactive
                                    },
                                )

                                MainTab.Preferences -> stringResource(
                                    R.string.places_count,
                                    preferencesState.places.size,
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.menu_more),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { PersianText(stringResource(R.string.action_about)) },
                            onClick = {
                                menuExpanded = false
                                onIntent(MainIntent.OpenAbout)
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                MainTab.entries.forEach { tab ->
                    val selected = mainState.selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIntent(MainIntent.SelectTab(tab))
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.lg),
                            )
                        },
                        label = {
                            PersianText(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainState.selectedTab == MainTab.Schedules,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIntent(MainIntent.ToggleService)
                    },
                    expanded = true,
                    shape = RoundedCornerShape(Radius.lg),
                    containerColor = if (mainState.serviceRunning) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (mainState.serviceRunning) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    icon = {
                        Crossfade(
                            targetState = mainState.serviceRunning,
                            label = "serviceFabIcon",
                        ) { running ->
                            Icon(
                                painter = painterResource(
                                    if (running) R.drawable.ic_pause else R.drawable.ic_play,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.lg),
                            )
                        }
                    },
                    text = {
                        PersianText(
                            text = stringResource(
                                if (mainState.serviceRunning) {
                                    R.string.stop_service_fab
                                } else {
                                    R.string.start_service_fab
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    },
                )
            }
        },
    ) { padding ->
        Crossfade(targetState = mainState.selectedTab, label = "tabContent") { tab ->
            when (tab) {
                MainTab.Schedules -> ScheduleScreen(
                    state = scheduleState,
                    contentPadding = padding,
                    onEvent = onScheduleEvent,
                )

                MainTab.Preferences -> PreferencesScreen(
                    state = preferencesState,
                    missingPermissions = mainState.missingPermissions,
                    contentPadding = padding,
                    onEvent = onPreferencesEvent,
                    onPermissionClick = { onIntent(MainIntent.FixPermission(it)) },
                )
            }
        }
    }

    if (preferencesState.isEditorVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onPreferencesEvent(PreferencesEvent.DismissEditor) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = Radius.xxl, topEnd = Radius.xxl),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            PlaceEditorSheet(
                place = preferencesState.editingPlace,
                onCancel = { onPreferencesEvent(PreferencesEvent.DismissEditor) },
                onSave = { onPreferencesEvent(PreferencesEvent.SavePlace(it)) },
            )
        }
    }
}
