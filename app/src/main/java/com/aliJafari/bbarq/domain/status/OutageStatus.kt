package com.aliJafari.bbarq.domain.status

/** How much attention an outage deserves right now. Drives the ticker interval. */
enum class ScheduleUrgency { ENDED, ONGOING, SOON, TODAY, UPCOMING }

private const val SOON_HOURS = 3

/**
 * The status of an outage as a value, not a string.
 *
 * The domain layer cannot build user-facing text: it has no [android.content.Context]
 * and no access to string resources, which is exactly the point. The UI maps
 * these cases onto the existing `R.string` entries, so Persian and English
 * both keep working and the data layer stops formatting sentences.
 */
sealed interface OutageStatus {

    val urgency: ScheduleUrgency

    /** The API gave us nothing usable. */
    data object Unknown : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.UPCOMING
    }

    data object Ongoing : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.ONGOING
    }

    data object Ended : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.ENDED
    }

    /**
     * Today, with no time of day reported.
     *
     * Maps to [ScheduleUrgency.SOON] while [Tomorrow] maps to
     * [ScheduleUrgency.TODAY]. That looks off by one, and it is: it reproduces
     * the shipped behaviour the notification refresh interval already depends
     * on. Change it deliberately, not by accident.
     */
    data object Today : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.SOON
    }

    data object Tomorrow : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.TODAY
    }

    data class InDays(val days: Int) : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.UPCOMING
    }

    data class StartsInMinutes(val minutes: Int) : OutageStatus {
        override val urgency: ScheduleUrgency = ScheduleUrgency.SOON
    }

    data class StartsInHours(val hours: Int) : OutageStatus {
        override val urgency: ScheduleUrgency
            get() = if (hours < SOON_HOURS) ScheduleUrgency.SOON else ScheduleUrgency.TODAY
    }

    data class StartsInHoursAndMinutes(val hours: Int, val minutes: Int) : OutageStatus {
        override val urgency: ScheduleUrgency
            get() = if (hours < SOON_HOURS) ScheduleUrgency.SOON else ScheduleUrgency.TODAY
    }
}
