package com.aliJafari.bbarq.domain.time

import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * The single source of truth for the timezone outage times are expressed in.
 *
 * Outage schedules are published by the utility in Tehran local time. Reading
 * them in the device timezone silently shifts every reminder for anyone
 * travelling, so no conversion in this app is allowed to use the default zone.
 *
 * Deliberately not a fixed +03:30 offset: Iran observed DST until 2022, so a
 * hardcoded offset misreads any timestamp from before then. The zone database
 * knows the history; we do not.
 */
object TimeZonePolicy {

    const val ID: String = "Asia/Tehran"

    /**
     * A fresh [TimeZone] per call. [TimeZone] instances are mutable, and handing
     * out a shared one invites action at a distance.
     */
    val zone: TimeZone
        get() = TimeZone.getTimeZone(ID)

    /**
     * An empty calendar in [zone]. Every field, milliseconds included, is
     * cleared, so callers cannot accidentally inherit "now".
     */
    internal fun emptyCalendar(): GregorianCalendar = GregorianCalendar(zone).apply { clear() }

    /** A calendar in [zone] positioned at [epochMillis]. */
    internal fun calendarAt(epochMillis: Long): GregorianCalendar =
        GregorianCalendar(zone).apply { timeInMillis = epochMillis }
}
