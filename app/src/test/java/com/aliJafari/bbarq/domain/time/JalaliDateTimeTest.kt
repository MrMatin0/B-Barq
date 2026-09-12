package com.aliJafari.bbarq.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Expected epochs were computed independently from the JDF conversion algorithm
 * plus a fixed +03:30 Tehran offset. Every asserted instant is after 2022, when
 * Iran abolished DST, so these numbers do not depend on the tzdata version.
 * The DST-era case asserts a round trip instead.
 */
class JalaliDateTimeTest {

    @Test
    fun `nowruz 1404 resolves to tehran midnight`() {
        assertEquals(1742502600000L, JalaliDateTime(1404, 1, 1).toEpochMillis())
    }

    @Test
    fun `date with time resolves in tehran zone`() {
        assertEquals(1757709000000L, JalaliDateTime(1404, 6, 22).toEpochMillis())
        assertEquals(1757791800000L, JalaliDateTime(1404, 6, 22, 23, 0).toEpochMillis())
        assertEquals(1758639600000L, JalaliDateTime(1404, 7, 1, 18, 30).toEpochMillis())
    }

    @Test
    fun `epoch maps back to jalali fields`() {
        assertEquals(
            JalaliDateTime(1405, 6, 21, 13, 45),
            JalaliDateTime.fromEpochMillis(1789208100000L),
        )
    }

    @Test
    fun `conversion round trips across the dst era`() {
        // Iran was on +04:30 in summer 1400, so this only holds if the zone
        // database is doing the work rather than a hardcoded offset.
        val summer1400 = JalaliDateTime(1400, 4, 1, 12, 0)
        assertEquals(summer1400, JalaliDateTime.fromEpochMillis(summer1400.toEpochMillis()))

        val today = JalaliDateTime(1405, 6, 21, 0, 0)
        assertEquals(today, JalaliDateTime.fromEpochMillis(today.toEpochMillis()))
    }

    @Test
    fun `esfand length follows the real leap years`() {
        // 1403 and 1408 are leap (30 day Esfand); 1404 and 1405 are not.
        assertNotNull(JalaliDateTime.orNull(1403, 12, 30))
        assertNotNull(JalaliDateTime.orNull(1408, 12, 30))
        assertNull(JalaliDateTime.orNull(1404, 12, 30))
        assertNull(JalaliDateTime.orNull(1405, 12, 30))
        assertNotNull(JalaliDateTime.orNull(1404, 12, 29))
    }

    @Test
    fun `invalid fields produce null instead of throwing`() {
        assertNull(JalaliDateTime.orNull(1404, 13, 1))
        assertNull(JalaliDateTime.orNull(1404, 6, 32))
        assertNull(JalaliDateTime.orNull(1404, 7, 31))
        assertNull(JalaliDateTime.orNull(1404, 6, 22, 24, 0))
        assertNull(JalaliDateTime.orNull(1404, 6, 22, 0, 60))
        assertNull(JalaliDateTime.orNull(0, 1, 1))
    }

    @Test
    fun `parses the api date and time shapes`() {
        assertEquals(JalaliDateTime(1404, 6, 22), JalaliDateTime.parse("1404/6/22"))
        assertEquals(JalaliDateTime(1404, 6, 22), JalaliDateTime.parse("1404-06-22"))
        assertEquals(
            JalaliDateTime(1404, 6, 22, 18, 0),
            JalaliDateTime.parse("1404/06/22", "18:00:00"),
        )
        assertEquals(
            JalaliDateTime(1404, 6, 22, 9, 5),
            JalaliDateTime.parse(" 1404/06/22 ", " 9:05 "),
        )
    }

    @Test
    fun `reads a twenty four hour end time as midnight`() {
        assertEquals(JalaliDateTime(1404, 6, 22), JalaliDateTime.parse("1404/06/22", "24:00"))
    }

    @Test
    fun `rejects unusable input`() {
        assertNull(JalaliDateTime.parse(null))
        assertNull(JalaliDateTime.parse(""))
        assertNull(JalaliDateTime.parse("   "))
        assertNull(JalaliDateTime.parse("1404/06"))
        assertNull(JalaliDateTime.parse("not a date"))
        assertNull(JalaliDateTime.parse("1404/ab/22"))
        assertNull(JalaliDateTime.parse("1404/06/22", "nope"))
    }

    @Test
    fun `counts calendar days across month and year boundaries`() {
        assertEquals(1, JalaliDateTime(1404, 6, 31).daysUntil(JalaliDateTime(1404, 7, 1)))
        assertEquals(1, JalaliDateTime(1403, 12, 30).daysUntil(JalaliDateTime(1404, 1, 1)))
        assertEquals(10, JalaliDateTime(1404, 6, 22).daysUntil(JalaliDateTime(1404, 7, 1)))
        assertEquals(-1, JalaliDateTime(1404, 7, 1).daysUntil(JalaliDateTime(1404, 6, 31)))
        assertEquals(
            0,
            JalaliDateTime(1404, 6, 22, 1, 0).daysUntil(JalaliDateTime(1404, 6, 22, 23, 30)),
        )
    }

    @Test
    fun `labels are zero padded and locale independent`() {
        assertEquals("09:05", JalaliDateTime(1404, 6, 22, 9, 5).timeLabel())
        assertEquals("00:00", JalaliDateTime(1404, 6, 22).timeLabel())
        assertEquals("1404/06/22", JalaliDateTime(1404, 6, 22).dateLabel())
    }

    @Test
    fun `compares chronologically`() {
        assertTrue(JalaliDateTime(1404, 6, 22, 8, 0) < JalaliDateTime(1404, 6, 22, 9, 0))
        assertTrue(JalaliDateTime(1404, 6, 31) < JalaliDateTime(1404, 7, 1))
        assertTrue(JalaliDateTime(1405, 1, 1) > JalaliDateTime(1404, 12, 29))
    }
}
