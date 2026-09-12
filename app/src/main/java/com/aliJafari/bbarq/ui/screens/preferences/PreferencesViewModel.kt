package com.aliJafari.bbarq.ui.screens.preferences

import android.app.Application
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aliJafari.bbarq.data.local.AuthStorage
import com.aliJafari.bbarq.data.local.PreferencesManager
import com.aliJafari.bbarq.data.repository.PlaceRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns everything on the preferences tab.
 *
 * MainActivity used to hold these as fields and mutate them from composable
 * lambdas, which is why toggling the theme could race the place list reload.
 */
class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val placeRepository = PlaceRepository.getInstance(application)
    private val prefsManager = PreferencesManager(application)

    private val systemInDarkMode: Boolean
        get() = (application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private val _state = MutableStateFlow(
        PreferencesUiState(
            darkMode = prefsManager.getDarkMode(systemInDarkMode),
            language = prefsManager.getLanguage(),
        ),
    )
    val state: StateFlow<PreferencesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PreferencesEffect>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<PreferencesEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            placeRepository.places.collect { places ->
                _state.update { it.copy(places = places) }
            }
        }
    }

    fun onEvent(event: PreferencesEvent) {
        when (event) {
            PreferencesEvent.AddPlace ->
                _state.update { it.copy(editingPlace = null, isEditorVisible = true) }

            PreferencesEvent.DismissEditor ->
                _state.update { it.copy(isEditorVisible = false) }

            PreferencesEvent.RequestLogout ->
                _state.update { it.copy(isLogoutDialogVisible = true) }

            PreferencesEvent.DismissLogout ->
                _state.update { it.copy(isLogoutDialogVisible = false) }

            PreferencesEvent.ConfirmLogout -> {
                _state.update { it.copy(isLogoutDialogVisible = false) }
                AuthStorage(getApplication()).clearToken()
                _effects.tryEmit(PreferencesEffect.NavigateToLogin)
            }

            PreferencesEvent.OpenAbout -> _effects.tryEmit(PreferencesEffect.OpenAbout)

            is PreferencesEvent.EditPlace ->
                _state.update { it.copy(editingPlace = event.place, isEditorVisible = true) }

            is PreferencesEvent.DeletePlace -> viewModelScope.launch {
                placeRepository.deletePlace(event.place)
            }

            is PreferencesEvent.SavePlace -> viewModelScope.launch {
                placeRepository.savePlace(event.place)
                _state.update { it.copy(isEditorVisible = false) }
            }

            is PreferencesEvent.SetDarkMode -> {
                prefsManager.setDarkMode(event.enabled)
                _state.update { it.copy(darkMode = event.enabled) }
            }

            is PreferencesEvent.SetLanguage -> {
                prefsManager.setLanguage(event.language)
                _state.update { it.copy(language = event.language) }
                _effects.tryEmit(PreferencesEffect.ApplyLanguage(event.language))
            }
        }
    }
}
