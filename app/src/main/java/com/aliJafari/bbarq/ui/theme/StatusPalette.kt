package com.aliJafari.bbarq.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.utils.ScheduleUrgency

/**
 * Semantic colours for a single outage urgency level.
 *
 * [glow] is intentionally translucent: it is used as the outer bloom behind
 * active/imminent outages so the card carries visual weight without shouting.
 */
@Immutable
data class StatusColors(
    val container: Color,
    val content: Color,
    val accent: Color,
    val glow: Color,
    val iconRes: Int,
)

@Immutable
data class StatusPalette(
    val ongoing: StatusColors,
    val soon: StatusColors,
    val today: StatusColors,
    val upcoming: StatusColors,
    val ended: StatusColors,
) {
    fun of(urgency: ScheduleUrgency): StatusColors = when (urgency) {
        ScheduleUrgency.ONGOING -> ongoing
        ScheduleUrgency.SOON -> soon
        ScheduleUrgency.TODAY -> today
        ScheduleUrgency.UPCOMING -> upcoming
        ScheduleUrgency.ENDED -> ended
    }
}

internal val LightStatusPalette = StatusPalette(
    ongoing = StatusColors(Color(0xFFFFDAD6), Color(0xFF8C1D18), Color(0xFFE5484D), Color(0x4DE5484D), R.drawable.ic_alert),
    soon = StatusColors(Color(0xFFFFE3BF), Color(0xFF8A4B00), Color(0xFFF07A13), Color(0x40F07A13), R.drawable.ic_bolt),
    today = StatusColors(Color(0xFFFFF2C2), Color(0xFF6F5200), Color(0xFFD4A017), Color(0x33D4A017), R.drawable.ic_clock),
    upcoming = StatusColors(Color(0xFFD8F3E3), Color(0xFF1B5E3A), Color(0xFF2FA36B), Color(0x2E2FA36B), R.drawable.ic_calendar),
    ended = StatusColors(Color(0xFFE7E1DA), Color(0xFF5C554D), Color(0xFF8B837A), Color(0x1A8B837A), R.drawable.ic_check),
)

internal val DarkStatusPalette = StatusPalette(
    ongoing = StatusColors(Color(0xFF5A1712), Color(0xFFFFB4AB), Color(0xFFFF6B66), Color(0x59FF6B66), R.drawable.ic_alert),
    soon = StatusColors(Color(0xFF4A2B00), Color(0xFFFFC077), Color(0xFFFF9F2E), Color(0x4DFF9F2E), R.drawable.ic_bolt),
    today = StatusColors(Color(0xFF453400), Color(0xFFF5D77A), Color(0xFFE8C24A), Color(0x40E8C24A), R.drawable.ic_clock),
    upcoming = StatusColors(Color(0xFF10361F), Color(0xFF8FE0B4), Color(0xFF44C98A), Color(0x3344C98A), R.drawable.ic_calendar),
    ended = StatusColors(Color(0xFF2C2823), Color(0xFFB5ADA4), Color(0xFF8B837A), Color(0x1A8B837A), R.drawable.ic_check),
)

/** Status colours for the current theme. */
val LocalStatusPalette = staticCompositionLocalOf { LightStatusPalette }

/** Whether the app is currently rendering its dark scheme (drives glass tinting). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun statusColors(urgency: ScheduleUrgency): StatusColors = LocalStatusPalette.current.of(urgency)
