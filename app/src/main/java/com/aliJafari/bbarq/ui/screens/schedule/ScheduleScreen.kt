package com.aliJafari.bbarq.ui.screens.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.ui.components.AnimatedFilterChip
import com.aliJafari.bbarq.ui.components.BBarqButton
import com.aliJafari.bbarq.ui.components.BBarqButtonTone
import com.aliJafari.bbarq.ui.components.BBarqEmptyState
import com.aliJafari.bbarq.ui.components.NoticeCard
import com.aliJafari.bbarq.ui.components.NoticeTone
import com.aliJafari.bbarq.ui.components.OutageCardSkeleton
import com.aliJafari.bbarq.ui.components.PersianText
import com.aliJafari.bbarq.ui.components.UpdateBanner
import com.aliJafari.bbarq.ui.components.rememberTickingNow
import com.aliJafari.bbarq.ui.theme.LocalStatusPalette
import com.aliJafari.bbarq.ui.theme.Spacing
import com.aliJafari.bbarq.ui.theme.placeColorOption
import com.aliJafari.bbarq.ui.theme.placeIconOption

/**
 * Upcoming outages.
 *
 * Stateless by design: it renders a [ScheduleUiState] and emits
 * [ScheduleEvent]s, so it can be previewed and tested without a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    contentPadding: PaddingValues,
    onEvent: (ScheduleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberTickingNow()
    val visibleOutages = state.visibleOutages

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ScheduleEvent.Refresh) },
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.sm,
                bottom = Spacing.listBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.update?.let { info ->
                item(key = "update-banner") {
                    UpdateBanner(
                        updateInfo = info,
                        onDismiss = { onEvent(ScheduleEvent.DismissUpdate) },
                    )
                }
            }

            state.errorMessage?.let { error ->
                item(key = "error-card") {
                    NoticeCard(
                        title = stringResource(R.string.network_request_failed),
                        message = error,
                        tone = NoticeTone.Error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { onEvent(ScheduleEvent.Refresh) },
                    )
                }
                if (visibleOutages.isNotEmpty()) {
                    item(key = "stale-hint") {
                        PersianText(
                            text = stringResource(R.string.stale_data_notice),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (state.showFilters) {
                item(key = "place-filters") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        item(key = "filter-all") {
                            AnimatedFilterChip(
                                label = stringResource(R.string.all),
                                selected = state.selectedPlaceId == null,
                                onClick = { onEvent(ScheduleEvent.SelectPlace(null)) },
                            )
                        }
                        items(state.places, key = { it.id }) { place ->
                            AnimatedFilterChip(
                                label = place.name,
                                selected = state.selectedPlaceId == place.id,
                                onClick = { onEvent(ScheduleEvent.SelectPlace(place.id)) },
                                accent = placeColorOption(place.colorKey).color,
                                iconRes = placeIconOption(place.iconKey).icon,
                            )
                        }
                    }
                }
            }

            if (state.showSkeletons) {
                item(key = "loading-label") {
                    PersianText(
                        text = stringResource(R.string.loading_schedules),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                items(count = 3, key = { index -> "skeleton-$index" }) {
                    OutageCardSkeleton()
                }
            } else if (visibleOutages.isEmpty()) {
                item(key = "empty-state") {
                    if (!state.hasPlaces) {
                        BBarqEmptyState(
                            iconRes = R.drawable.ic_add_place,
                            title = stringResource(R.string.no_places_title),
                            message = stringResource(R.string.empty_places_message),
                            actionLabel = stringResource(R.string.add_first_place),
                            onAction = { onEvent(ScheduleEvent.AddPlace) },
                        )
                    } else if (state.errorMessage == null) {
                        BBarqEmptyState(
                            iconRes = R.drawable.ic_check,
                            title = stringResource(R.string.all_clear_title),
                            message = state.emptyMessage,
                            accent = LocalStatusPalette.current.upcoming.accent,
                        )
                    }
                }
            }

            items(
                items = visibleOutages,
                key = { "${it.place.id}-${it.outage.id}-${it.outage.date}-${it.outage.startTime}" },
            ) { schedule ->
                OutageCard(
                    schedule = schedule,
                    now = now,
                    onShare = { onEvent(ScheduleEvent.Share(schedule)) },
                    modifier = Modifier.animateItem(),
                )
            }

            if (state.hasPlaces && !state.isLoading && !state.isRefreshing) {
                item(key = "manual-refresh") {
                    BBarqButton(
                        text = stringResource(R.string.refresh),
                        onClick = { onEvent(ScheduleEvent.Refresh) },
                        tone = BBarqButtonTone.Ghost,
                        iconRes = R.drawable.ic_renew,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}
