package com.aliJafari.bbarq.domain.time

import saman.zamani.persiandate.PersianDate

/**
 * The only part of [PersianDate] this app trusts.
 *
 * `jalali_to_gregorian` and `gregorian_to_jalali` are pure integer arithmetic
 * over their arguments (the JDF algorithm): they read and write no state, so a
 * single shared instance is safe from any thread.
 *
 * Everything else in that class is avoided on purpose:
 *  - its internal timestamp is computed with a [java.text.SimpleDateFormat] in
 *    the *device default* timezone and locale, so it cannot be pinned to Tehran;
 *  - its setters are order dependent and throw, e.g. `setShDay(31)` validates
 *    against whichever month happens to be set at that moment;
 *  - its leap-year check is a 33-year cycle approximation;
 *  - its `after()` is inverted.
 */
internal object JalaliCalendarConverter {

    private val converter = PersianDate()

    data class YearMonthDay(val year: Int, val month: Int, val day: Int)

    fun toGregorian(year: Int, month: Int, day: Int): YearMonthDay =
        converter.jalali_to_gregorian(year, month, day)
            .let { YearMonthDay(it[0], it[1], it[2]) }

    fun toJalali(year: Int, month: Int, day: Int): YearMonthDay =
        converter.gregorian_to_jalali(year, month, day)
            .let { YearMonthDay(it[0], it[1], it[2]) }

    /**
     * Days in a Jalali month.
     *
     * Esfand is decided by round-tripping day 30 through the converter rather
     * than by a separate leap-year rule. That way "valid" means exactly "the
     * converter agrees", which is what guarantees
     * [JalaliDateTime.fromEpochMillis] can never produce a value that
     * [JalaliDateTime]'s own validation rejects.
     */
    fun monthLength(year: Int, month: Int): Int = when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        else -> {
            val gregorian = toGregorian(year, 12, 30)
            val roundTrip = toJalali(gregorian.year, gregorian.month, gregorian.day)
            if (roundTrip.year == year && roundTrip.month == 12 && roundTrip.day == 30) 30 else 29
        }
    }
}
