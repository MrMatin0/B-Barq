package com.aliJafari.bbarq.domain.model

/**
 * How far ahead of an outage the user wants to be warned.
 *
 * [bit] is the persisted bitmask position; it must not be reordered or reused,
 * existing installs already store these values.
 *
 * Unlike the previous version this carries no string resource id, so the type
 * stays usable from pure Kotlin. Labels are resolved in the UI.
 */
enum class ReminderOffset(val minutes: Int, val bit: Int) {
    TWO_HOURS(minutes = 120, bit = 1),
    ONE_HOUR(minutes = 60, bit = 2),
    THIRTY_MIN(minutes = 30, bit = 4),
    FIVE_MIN(minutes = 5, bit = 8),
    ;

    companion object {

        fun fromMask(mask: Int): Set<ReminderOffset> =
            entries.filterTo(mutableSetOf()) { mask and it.bit != 0 }

        fun toMask(offsets: Set<ReminderOffset>): Int =
            offsets.fold(0) { mask, offset -> mask or offset.bit }
    }
}
