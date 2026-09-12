package com.aliJafari.bbarq.domain.time

/**
 * The time window of a single outage.
 *
 * [start] and [end] are nullable because the API reports either, both or
 * neither. When [start] is missing the outage is only known at day granularity,
 * and callers fall back to [date].
 *
 * This type owns the one and only midnight-wrap rule in the app: an end at or
 * before the start belongs to the next day, so `18:00 -> 02:00` is six hours
 * everywhere instead of being negative in some call sites and clamped in others.
 */
data class OutageSchedule(
    val date: JalaliDateTime,
    val start: JalaliDateTime?,
    val end: JalaliDateTime?,
) {

    val dayStartEpochMillis: Long by lazy(LazyThreadSafetyMode.PUBLICATION) {
        date.startOfDayEpochMillis()
    }

    val startEpochMillis: Long? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        start?.toEpochMillis()
    }

    val endEpochMillis: Long? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val raw = end?.toEpochMillis() ?: return@lazy null
        val from = startEpochMillis
        if (from != null && raw <= from) raw + JalaliDateTime.DAY_MILLIS else raw
    }

    /** Length in minutes, or null when the API did not report both ends. */
    fun durationMinutes(): Int? {
        val from = startEpochMillis ?: return null
        val to = endEpochMillis ?: return null
        return if (to > from) ((to - from) / MINUTE_MILLIS).toInt() else null
    }

    fun isRunning(now: Long = System.currentTimeMillis()): Boolean {
        val from = startEpochMillis ?: return false
        val to = endEpochMillis ?: return now >= from
        return now in from..to
    }

    /** False when the end is unknown: an outage with no reported end never expires. */
    fun hasEnded(now: Long = System.currentTimeMillis()): Boolean {
        val to = endEpochMillis ?: return false
        return now > to
    }

    /** Negative once the outage has started. Null when the start is unknown. */
    fun minutesUntilStart(now: Long = System.currentTimeMillis()): Long? {
        val from = startEpochMillis ?: return null
        return (from - now) / MINUTE_MILLIS
    }

    /**
     * How far through the outage we are, in 0f..1f, or null unless it is
     * actually running, so callers can skip the progress affordance.
     */
    fun progressFraction(now: Long = System.currentTimeMillis()): Float? {
        val from = startEpochMillis ?: return null
        val to = endEpochMillis ?: return null
        if (to <= from || now < from || now > to) return null
        return ((now - from).toFloat() / (to - from).toFloat()).coerceIn(0f, 1f)
    }

    companion object {

        private const val MINUTE_MILLIS = 60_000L

        /**
         * Builds a schedule from raw API strings. Null only when the date itself
         * is unusable; a missing or malformed time simply narrows the precision.
         */
        fun parse(date: String?, startTime: String?, endTime: String?): OutageSchedule? {
            val day = JalaliDateTime.parse(date) ?: return null
            return OutageSchedule(
                date = day.atStartOfDay(),
                start = JalaliDateTime.parse(date, startTime),
                end = JalaliDateTime.parse(date, endTime),
            )
        }
    }
}
