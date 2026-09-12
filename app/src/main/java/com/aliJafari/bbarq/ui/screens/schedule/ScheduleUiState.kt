package com.aliJafari.bbarq.ui.screens.schedule

import androidx.compose.runtime.Immutable
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.utils.UpdateInfo
import com.aliJafari.bbarq.utils.startEpochMillis

/**
 * Everything the schedule screen renders, in one immutable snapshot.
 *
 * Marked [Immutable] so Compose can skip the whole subtree when the reference
 * has not changed; the derived values are lazy so filtering does not run on
 * every recomposition.
 */
@Immutable
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val places: List<Place> = emptyList(),
    val outages: List<PlaceOutage> = emptyList(),
    val selectedPlaceId: Long? = null,
    val errorMessage: String? = null,
    val emptyMessage: String? = null,
    val update: UpdateInfo? = null,
) {
    val hasPlaces: Boolean get() = places.isNotEmpty()

    val showFilters: Boolean get() = places.size > 1

    val showSkeletons: Boolean get() = isLoading && outages.isEmpty()

    /** Outages for the selected place (or all of them), soonest first. */
    val visibleOutages: List<PlaceOutage> by lazy(LazyThreadSafetyMode.NONE) {
        val filtered = if (selectedPlaceId == null) {
            outages
        } else {
            outages.filter { it.place.id == selectedPlaceId }
        }
        filtered.sortedBy { it.outage.startEpochMillis() }
    }
}

/** Everything the schedule screen can ask the ViewModel to do. */
sealed interface ScheduleEvent {
    data object Refresh : ScheduleEvent
    data object AddPlace : ScheduleEvent
    data object DismissUpdate : ScheduleEvent
    data class SelectPlace(val placeId: Long?) : ScheduleEvent
    data class Share(val schedule: PlaceOutage) : ScheduleEvent
    data class Copy(val schedule: PlaceOutage) : ScheduleEvent
}

/** One-shot side effects that need an Activity to carry them out. */
sealed interface ScheduleEffect {
    data object OpenPlaceEditor : ScheduleEffect
    data class ShareSchedule(val schedule: PlaceOutage) : ScheduleEffect
    data class CopySchedule(val schedule: PlaceOutage) : ScheduleEffect
}
