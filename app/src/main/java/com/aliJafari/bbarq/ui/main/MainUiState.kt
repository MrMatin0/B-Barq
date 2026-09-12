package com.aliJafari.bbarq.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aliJafari.bbarq.R

/**
 * A runtime capability the app needs but does not have yet.
 *
 * This replaces three parallel booleans that had to be threaded through four
 * composables each; screens now just render the list they are handed.
 */
enum class AppPermission(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Notifications(
        R.string.allow_post_notifications,
        R.string.notification_permissoin_subtitle,
        R.drawable.ic_bell,
    ),
    BatteryOptimization(
        R.string.allow_background_battery_usage,
        R.string.battery_permissoin_subtitle,
        R.drawable.ic_bolt,
    ),
    ExactAlarms(
        R.string.allow_setting_exact_alarms,
        R.string.alarm_permissoin_subtitle,
        R.drawable.ic_alert,
    ),
}

enum class MainTab(
    @StringRes val titleRes: Int,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Schedules(R.string.upcoming_outages, R.string.schedules_tab, R.drawable.ic_schedule_tab),
    Preferences(R.string.preferences_title, R.string.prefs_tab, R.drawable.ic_prefs),
}

@Immutable
data class MainUiState(
    val selectedTab: MainTab = MainTab.Schedules,
    val serviceRunning: Boolean = false,
    val missingPermissions: List<AppPermission> = emptyList(),
)

sealed interface MainIntent {
    data object ToggleService : MainIntent
    data object OpenAbout : MainIntent
    data class SelectTab(val tab: MainTab) : MainIntent
    data class FixPermission(val permission: AppPermission) : MainIntent
}

/** Work only the Activity can carry out. */
sealed interface MainEffect {
    data object ToggleService : MainEffect
    data object OpenAbout : MainEffect
    data class RequestPermission(val permission: AppPermission) : MainEffect
}
