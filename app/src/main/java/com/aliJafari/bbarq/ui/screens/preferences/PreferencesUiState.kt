package com.aliJafari.bbarq.ui.screens.preferences

import androidx.compose.runtime.Immutable
import com.aliJafari.bbarq.BuildConfig
import com.aliJafari.bbarq.data.local.AppLanguage
import com.aliJafari.bbarq.data.model.Place

@Immutable
data class PreferencesUiState(
    val places: List<Place> = emptyList(),
    val darkMode: Boolean = false,
    val language: AppLanguage = AppLanguage.FA,
    val editingPlace: Place? = null,
    val isEditorVisible: Boolean = false,
    val isLogoutDialogVisible: Boolean = false,
    val versionName: String = BuildConfig.VERSION_NAME,
)

sealed interface PreferencesEvent {
    data object AddPlace : PreferencesEvent
    data object DismissEditor : PreferencesEvent
    data object RequestLogout : PreferencesEvent
    data object DismissLogout : PreferencesEvent
    data object ConfirmLogout : PreferencesEvent
    data object OpenAbout : PreferencesEvent
    data class EditPlace(val place: Place) : PreferencesEvent
    data class DeletePlace(val place: Place) : PreferencesEvent
    data class SavePlace(val place: Place) : PreferencesEvent
    data class SetDarkMode(val enabled: Boolean) : PreferencesEvent
    data class SetLanguage(val language: AppLanguage) : PreferencesEvent
}

/** Things only an Activity can do. */
sealed interface PreferencesEffect {
    data object NavigateToLogin : PreferencesEffect
    data object OpenAbout : PreferencesEffect
    data class ApplyLanguage(val language: AppLanguage) : PreferencesEffect
}
