package com.aliJafari.bbarq.domain.model

import com.aliJafari.bbarq.domain.time.JalaliDateTime
import com.aliJafari.bbarq.domain.time.OutageSchedule

/**
 * A single planned outage for one place.
 *
 * [id] is a composite string, not the API's `outage_number`: that number is only
 * unique per subscription per day, so using it as a primary key collided across
 * places and dates.
 */
data class Outage(
    val id: String,
    val outageNumber: Int,
    val placeId: Long,
    val schedule: OutageSchedule,
    val reason: String? = null,
    val address: String? = null,
) {

    val startLabel: String? get() = schedule.start?.timeLabel()

    val endLabel: String? get() = schedule.end?.timeLabel()

    companion object {

        fun idOf(placeId: Long, outageNumber: Int, date: JalaliDateTime): String =
            "${placeId}_${outageNumber}_${date.dateLabel()}"
    }
}
