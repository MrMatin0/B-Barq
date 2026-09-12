package com.aliJafari.bbarq.domain.usecase

import com.aliJafari.bbarq.domain.model.Outage
import com.aliJafari.bbarq.domain.status.OutageStatus
import com.aliJafari.bbarq.domain.time.JalaliDateTime
import com.aliJafari.bbarq.domain.time.OutageSchedule
import javax.inject.Inject

private const val MINUTES_PER_HOUR = 60L

/** Remainders at or above this round up to the next whole hour. */
private const val ROUND_UP_FROM_MINUTES = 50L

/** Within this many minutes of a whole hour, the minutes are not worth showing. */
private const val WHOLE_HOUR_SLACK_MINUTES = 10

/**
 * Turns a schedule into a status, relative to [now].
 *
 * Pure, injectable and clock-injectable, which is what makes the boundaries
 * testable. It reproduces the previously shipped labels and urgencies exactly,
 * including the "round up from 50 minutes" rule that makes 1h55m read as
 * "in 2 hours".
 */
class GetOutageStatusUseCase @Inject constructor() {

    operator fun invoke(outage: Outage, now: Long = System.currentTimeMillis()): OutageStatus =
        invoke(outage.schedule, now)

    operator fun invoke(
        schedule: OutageSchedule?,
        now: Long = System.currentTimeMillis(),
    ): OutageStatus {
        if (schedule == null) return OutageStatus.Unknown
        val today = JalaliDateTime.fromEpochMillis(now)

        // No time of day reported: fall back to a day-level status.
        val start = schedule.startEpochMillis
            ?: return dayLevelStatus(today.daysUntil(schedule.date))

        if (schedule.hasEnded(now)) return OutageStatus.Ended
        if (now >= start) return OutageStatus.Ongoing

        val days = today.daysUntil(JalaliDateTime.fromEpochMillis(start))
        return when {
            days <= 0 -> sameDayStatus(schedule.minutesUntilStart(now) ?: 0L)
            days == 1 -> OutageStatus.Tomorrow
            else -> OutageStatus.InDays(days)
        }
    }

    private fun dayLevelStatus(days: Int): OutageStatus = when {
        days < 0 -> OutageStatus.Ended
        days == 0 -> OutageStatus.Today
        days == 1 -> OutageStatus.Tomorrow
        else -> OutageStatus.InDays(days)
    }

    private fun sameDayStatus(minutesUntilStart: Long): OutageStatus {
        val remainder = minutesUntilStart % MINUTES_PER_HOUR
        val rounded = if (remainder >= ROUND_UP_FROM_MINUTES) {
            minutesUntilStart + (MINUTES_PER_HOUR - remainder)
        } else {
            minutesUntilStart
        }
        val hours = (rounded / MINUTES_PER_HOUR).toInt()
        val minutes = (rounded % MINUTES_PER_HOUR).toInt()
        return when {
            hours < 1 -> OutageStatus.StartsInMinutes(minutesUntilStart.toInt())
            minutes <= WHOLE_HOUR_SLACK_MINUTES -> OutageStatus.StartsInHours(hours)
            else -> OutageStatus.StartsInHoursAndMinutes(hours, minutes)
        }
    }
}
