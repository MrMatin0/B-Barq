package com.aliJafari.bbarq.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shell state: which tab is showing, whether the monitoring service is alive,
 * and which permissions are still missing.
 *
 * Deliberately knows nothing about Android APIs. The Activity reports platform
 * facts in through [onSystemStateChanged] and performs [MainEffect]s on the way
 * back out, which keeps this unit-testable.
 */
class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<MainEffect>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<MainEffect> = _effects.asSharedFlow()

    fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.ToggleService -> _effects.tryEmit(MainEffect.ToggleService)
            MainIntent.OpenAbout -> _effects.tryEmit(MainEffect.OpenAbout)
            is MainIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            is MainIntent.FixPermission ->
                _effects.tryEmit(MainEffect.RequestPermission(intent.permission))
        }
    }

    fun onSystemStateChanged(serviceRunning: Boolean, missingPermissions: List<AppPermission>) {
        _state.update {
            it.copy(serviceRunning = serviceRunning, missingPermissions = missingPermissions)
        }
    }

    fun selectTab(tab: MainTab) = onIntent(MainIntent.SelectTab(tab))
}
