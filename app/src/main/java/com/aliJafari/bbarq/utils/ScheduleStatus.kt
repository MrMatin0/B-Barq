package com.aliJafari.bbarq.utils

import android.content.Context
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.model.Outage
import java.util.Calendar

/**
 * Outage timing logic.
 *
 * This used to live next to a composable in the UI package, which meant the
 * foreground service had to import from `ui.main`. It is pure domain logic, so
 * it lives here and both layers depend on it instead of on each other.
 */
enum class ScheduleUrgency { ENDED, ONGOING, SOON, TODAY, UPCOMING }

data class ScheduleStatus(val label: String, val urgency: ScheduleUrgency)

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

fun daysBetween(from: Calendar, to: Calendar): Int {
    fun Calendar.atStartOfDay(): Calendar = (clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return ((to.atStartOfDay().timeInMillis - from.atStartOfDay().timeInMillis) / DAY_MILLIS).toInt()
}

/** Absolute start of the outage, or -1 when the API gave us nothing usable. */
fun Outage.startEpochMillis(): Long {
    val day = date?.takeIf { it.isNotBlank() } ?: return -1L
    return day.toEpochMillis(startTime?.takeIf { it.isNotBlank() } ?: "00:00").takeIf { it != -1L } ?: -1L
}

/** Absolute end of the outage, wrapping past midnight when needed. */
fun Outage.endEpochMillis(): Long {
    val day = date?.takeIf { it.isNotBlank() } ?: return -1L
    val end = endTime?.takeIf { it.isNotBlank() } ?: return -1L
    val start = startEpochMillis()
    var target = day.toEpochMillis(end).takeIf { it != -1L } ?: return -1L
    if (start != -1L && target <= start) target += DAY_MILLIS
    return target
}

/** Outage length in minutes, or null when the API did not report both ends. */
fun Outage.durationMinutes(): Int? {
    val start = startEpochMillis()
    val end = endEpochMillis()
    if (start == -1L || end == -1L || end <= start) return null
    return ((end - start) / 60_000L).toInt()
}

/**
 * How far through the outage we are, in 0f..1f. Returns null unless the outage
 * is actually running right now, so callers can skip the progress affordance.
 */
fun Outage.progressFraction(now: Long = System.currentTimeMillis()): Float? {
    val start = startEpochMillis()
    val end = endEpochMillis()
    if (start == -1L || end == -1L || end <= start) return null
    if (now < start || now > end) return null
    return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
}

fun relativeStatus(outage: Outage, context: Context): ScheduleStatus {
    val unknown = { ScheduleStatus(context.getString(R.string.value_not_available), ScheduleUrgency.UPCOMING) }

    val date = outage.date?.takeIf { it.isNotBlank() } ?: return unknown()
    val startTime = outage.startTime?.takeIf { it.isNotBlank() }

    // No time from the API: fall back to a day-level status.
    if (startTime == null) {
        val dateOnlyTarget = date.toEpochMillis("00:00")
        if (dateOnlyTarget == -1L) return unknown()

        val daysDiff = daysBetween(
            Calendar.getInstance(),
            Calendar.getInstance().apply { timeInMillis = dateOnlyTarget },
        )
        return when {
            daysDiff < 0 -> ScheduleStatus(context.getString(R.string.status_ended), ScheduleUrgency.ENDED)
            daysDiff == 0 -> ScheduleStatus(context.getString(R.string.today), ScheduleUrgency.SOON)
            daysDiff == 1 -> ScheduleStatus(context.getString(R.string.tomorrow), ScheduleUrgency.TODAY)
            else -> ScheduleStatus(context.getString(R.string.in_days, daysDiff), ScheduleUrgency.UPCOMING)
        }
    }

    val startTarget = date.toEpochMillis(startTime)
    if (startTarget == -1L) return unknown()

    var endTarget = outage.endTime?.takeIf { it.isNotBlank() }?.let { date.toEpochMillis(it) }?.takeIf { it != -1L }
    if (endTarget != null && endTarget <= startTarget) endTarget += DAY_MILLIS

    val now = Calendar.getInstance().timeInMillis

    if (endTarget != null && now > endTarget) {
        return ScheduleStatus(context.getString(R.string.status_ended), ScheduleUrgency.ENDED)
    }
    if (now >= startTarget) {
        return ScheduleStatus(context.getString(R.string.status_ongoing), ScheduleUrgency.ONGOING)
    }

    val minutesDiff = (startTarget - now) / 60_000L
    val daysDiff = daysBetween(
        Calendar.getInstance(),
        Calendar.getInstance().apply { timeInMillis = startTarget },
    )
    return when (daysDiff) {
        0 -> {
            val roundedTotal = if (minutesDiff % 60 >= 50) minutesDiff + (60 - minutesDiff % 60) else minutesDiff
            val hours = roundedTotal / 60
            val mins = roundedTotal % 60
            val label = when {
                hours < 1 -> context.getString(R.string.in_minutes, minutesDiff)
                mins <= 10 -> context.resources.getQuantityString(R.plurals.in_hours, hours.toInt(), hours)
                else -> context.getString(R.string.in_hours_minutes, hours, mins)
            }
            ScheduleStatus(label, if (hours < 3) ScheduleUrgency.SOON else ScheduleUrgency.TODAY)
        }

        1 -> ScheduleStatus(context.getString(R.string.tomorrow), ScheduleUrgency.TODAY)
        else -> ScheduleStatus(context.getString(R.string.in_days, daysDiff), ScheduleUrgency.UPCOMING)
    }
}
