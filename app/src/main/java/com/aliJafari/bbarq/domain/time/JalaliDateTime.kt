package com.aliJafari.bbarq.domain.time

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.time.Duration

/**
 * An immutable Jalali date and wall-clock time, always interpreted in
 * [TimeZonePolicy.ID].
 *
 * Instances are always valid: the constructor rejects impossible field
 * combinations. Data coming off the wire goes through [parse] or [orNull],
 * which return null instead of throwing, so bad API payloads never become
 * control flow.
 */
data class JalaliDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int = 0,
    val minute: Int = 0,
) : Comparable<JalaliDateTime> {

    init {
        require(year >= 1) { "Jalali year must be positive" }
        require(month in 1..12) { "Jalali month must be in 1..12" }
        require(day in 1..JalaliCalendarConverter.monthLength(year, month)) {
            "Jalali day $day is out of range for $year/$month"
        }
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }

    /** This instant as epoch millis, resolved in the Tehran zone. */
    fun toEpochMillis(): Long {
        val gregorian = JalaliCalendarConverter.toGregorian(year, month, day)
        return TimeZonePolicy.emptyCalendar()
            .apply { set(gregorian.year, gregorian.month - 1, gregorian.day, hour, minute, 0) }
            .timeInMillis
    }

    fun atStartOfDay(): JalaliDateTime =
        if (hour == 0 && minute == 0) this else copy(hour = 0, minute = 0)

    fun startOfDayEpochMillis(): Long = atStartOfDay().toEpochMillis()

    /**
     * Whole calendar days from this date to [other], ignoring time of day.
     * Negative when [other] is in the past.
     *
     * Rounded rather than truncated so a historical DST transition (Iran had
     * them until 2022) cannot turn a 23 hour day into zero days.
     */
    fun daysUntil(other: JalaliDateTime): Int {
        val difference = other.startOfDayEpochMillis() - startOfDayEpochMillis()
        return (difference.toDouble() / DAY_MILLIS).roundToLong().toInt()
    }

    operator fun plus(duration: Duration): JalaliDateTime =
        fromEpochMillis(toEpochMillis() + duration.inWholeMilliseconds)

    operator fun minus(duration: Duration): JalaliDateTime =
        fromEpochMillis(toEpochMillis() - duration.inWholeMilliseconds)

    /**
     * Zero padded `HH:mm` in [Locale.ROOT].
     *
     * The locale matters: the app switches the process locale to `fa`, and a
     * Persian locale would render Persian digits here, which must never reach
     * an API payload, a database key or a comparison. Display-time digit
     * shaping is the UI's job.
     */
    fun timeLabel(): String = "%02d:%02d".format(Locale.ROOT, hour, minute)

    /** Zero padded `YYYY/MM/DD` in [Locale.ROOT], sortable as text. */
    fun dateLabel(): String = "%04d/%02d/%02d".format(Locale.ROOT, year, month, day)

    override fun compareTo(other: JalaliDateTime): Int = compareValuesBy(
        this,
        other,
        { it.year },
        { it.month },
        { it.day },
        { it.hour },
        { it.minute },
    )

    companion object {

        internal const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L

        /** Never throws: any instant maps to a valid Jalali date. */
        fun fromEpochMillis(millis: Long): JalaliDateTime {
            val calendar = TimeZonePolicy.calendarAt(millis)
            val jalali = JalaliCalendarConverter.toJalali(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
            )
            return JalaliDateTime(
                year = jalali.year,
                month = jalali.month,
                day = jalali.day,
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
            )
        }

        /** Null instead of an exception when the fields do not describe a real date. */
        fun orNull(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): JalaliDateTime? =
            runCatching { JalaliDateTime(year, month, day, hour, minute) }.getOrNull()

        /**
         * Parses the API's `YYYY/MM/DD` (or `YYYY-MM-DD`) date and optional
         * `HH:mm` / `HH:mm:ss` time. Returns null for anything unusable, which
         * callers treat as "this field was not reported".
         *
         * Seconds are dropped, and a `24:00` end time is read as `00:00`; the
         * midnight wrap in [OutageSchedule] then moves it to the next day.
         */
        fun parse(date: String?, time: String? = null): JalaliDateTime? {
            val parts = date?.trim()?.split('/', '-')?.takeIf { it.size == 3 } ?: return null
            val year = parts[0].trim().toIntOrNull() ?: return null
            val month = parts[1].trim().toIntOrNull() ?: return null
            val day = parts[2].trim().toIntOrNull() ?: return null
            val clock = parseClock(time) ?: return null
            return orNull(year, month, day, clock.first, clock.second)
        }

        private fun parseClock(time: String?): Pair<Int, Int>? {
            val raw = time?.trim()?.takeIf { it.isNotEmpty() } ?: return 0 to 0
            val parts = raw.split(':')
            val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            if (hour == 24 && minute == 0) return 0 to 0
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour to minute
        }
    }
}
