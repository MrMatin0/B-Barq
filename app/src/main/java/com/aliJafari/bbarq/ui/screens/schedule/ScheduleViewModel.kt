package com.aliJafari.bbarq.ui.screens.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Place
import com.aliJafari.bbarq.data.repository.OutageRepository
import com.aliJafari.bbarq.data.repository.PlaceOutage
import com.aliJafari.bbarq.data.repository.PlaceRepository
import com.aliJafari.bbarq.utils.BillIDNot13Chars
import com.aliJafari.bbarq.utils.BillIDNotFoundException
import com.aliJafari.bbarq.utils.ReminderOffset
import com.aliJafari.bbarq.utils.RequestUnsuccessful
import com.aliJafari.bbarq.utils.UpdateChecker
import com.aliJafari.bbarq.utils.scheduleReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the outage schedule.
 *
 * All of this used to be `mutableStateOf` fields on MainActivity, which meant
 * every rotation dropped the fetched schedules and every fetch raced itself.
 */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val placeRepository = PlaceRepository.getInstance(application)
    private val outageRepository = OutageRepository(application)

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ScheduleEffect>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<ScheduleEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            placeRepository.places.collect { places ->
                _state.update { current ->
                    current.copy(
                        places = places,
                        selectedPlaceId = current.selectedPlaceId
                            ?.takeIf { selected -> places.any { it.id == selected } },
                    )
                }
                load(userInitiated = false)
            }
        }
        viewModelScope.launch { placeRepository.refresh() }
        viewModelScope.launch {
            UpdateChecker.checkForUpdate()?.let { info ->
                _state.update { it.copy(update = info) }
            }
        }
    }

    fun onEvent(event: ScheduleEvent) {
        when (event) {
            ScheduleEvent.Refresh -> load(userInitiated = true)
            ScheduleEvent.AddPlace -> _effects.tryEmit(ScheduleEffect.OpenPlaceEditor)
            ScheduleEvent.DismissUpdate -> _state.update { it.copy(update = null) }
            is ScheduleEvent.SelectPlace -> _state.update { it.copy(selectedPlaceId = event.placeId) }
            is ScheduleEvent.Share -> _effects.tryEmit(ScheduleEffect.ShareSchedule(event.schedule))
        }
    }

    /** Re-fetch, e.g. when the app comes back to the foreground. */
    fun reload() = load(userInitiated = false)

    private fun load(userInitiated: Boolean) {
        val places = _state.value.places
        if (places.isEmpty()) {
            loadJob?.cancel()
            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    outages = emptyList(),
                    errorMessage = null,
                    emptyMessage = null,
                )
            }
            return
        }

        // A background reload never interrupts work already in flight; an
        // explicit pull-to-refresh always wins.
        if (loadJob?.isActive == true && !userInitiated) return
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = !userInitiated,
                    isRefreshing = userInitiated,
                    errorMessage = null,
                )
            }

            val previous = _state.value.outages
            val result = withContext(Dispatchers.IO) { fetch(places, previous) }

            _state.update { current ->
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    outages = result.schedules,
                    errorMessage = result.messages.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                    emptyMessage = if (result.schedules.isEmpty() && result.messages.isEmpty()) {
                        allClearMessage()
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun fetch(places: List<Place>, previous: List<PlaceOutage>): FetchResult {
        val context = getApplication<Application>()
        val schedules = mutableListOf<PlaceOutage>()
        val messages = mutableListOf<String>()

        places.forEach { place ->
            try {
                outageRepository.fetchOutages(place.billId).forEach { outage ->
                    schedules += PlaceOutage(place = place, outage = outage)
                    ReminderOffset.entries.forEach { offset ->
                        val enabled = place.reminderOffsetsMask and offset.bit != 0
                        scheduleReminder(context, outage, place.name, offset, enabled)
                    }
                }
            } catch (e: BillIDNot13Chars) {
                messages += context.getString(R.string.place_fetch_invalid_count, place.name)
            } catch (e: BillIDNotFoundException) {
                messages += context.getString(R.string.place_fetch_invalid_bill_id, place.name)
            } catch (e: RequestUnsuccessful) {
                messages += context.getString(R.string.place_fetch_failed, place.name, e.details)
                // Transient failure: keep whatever we already had on screen.
                schedules += previous.filter { it.place.billId == place.billId }
            }
        }
        return FetchResult(schedules, messages)
    }

    private fun allClearMessage(): String = getApplication<Application>()
        .resources
        .getStringArray(R.array.no_power_cut_messages)
        .random()

    private data class FetchResult(
        val schedules: List<PlaceOutage>,
        val messages: List<String>,
    )
}
