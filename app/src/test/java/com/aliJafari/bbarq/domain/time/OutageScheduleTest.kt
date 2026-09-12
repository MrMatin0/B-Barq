package com.aliJafari.bbarq.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutageScheduleTest {

    private val eighteenToTwenty = requireNotNull(
        OutageSchedule.parse("1404/06/22", "18:00", "20:00"),
    )

    /** The case that used to produce negative durations all over the app. */
    private val acrossMidnight = requireNotNull(
        OutageSchedule.parse("1404/06/22", "23:00", "01:00"),
    )

    @Test
    fun `computes duration for a same day window`() {
        assertEquals(120, eighteenToTwenty.durationMinutes())
    }

    @Test
    fun `wraps an end time past midnight to the next day`() {
        assertEquals(120, acrossMidnight.durationMinutes())
        assertEquals(
            JalaliDateTime(1404, 6, 23, 1, 0).toEpochMillis(),
            acrossMidnight.endEpochMillis,
        )
    }

    @Test
    fun `reads a twenty four hour end as the following midnight`() {
        val schedule = requireNotNull(OutageSchedule.parse("1404/06/22", "22:00", "24:00"))
        assertEquals(120, schedule.durationMinutes())
    }

    @Test
    fun `has no duration without both ends`() {
        val openEnded = requireNotNull(OutageSchedule.parse("1404/06/22", "18:00", null))
        assertNull(openEnded.durationMinutes())
        assertNull(openEnded.endEpochMillis)
        assertNull(openEnded.progressFraction(openEnded.startEpochMillis!! + 60_000L))
    }

    @Test
    fun `keeps the day when no start time is reported`() {
        val dayOnly = requireNotNull(OutageSchedule.parse("1404/06/22", null, null))
        assertNull(dayOnly.startEpochMillis)
        assertNull(dayOnly.durationMinutes())
        assertEquals(JalaliDateTime(1404, 6, 22).toEpochMillis(), dayOnly.dayStartEpochMillis)
        assertFalse(dayOnly.isRunning(dayOnly.dayStartEpochMillis))
    }

    @Test
    fun `reports progress only while running`() {
        val start = eighteenToTwenty.startEpochMillis!!
        assertNull(eighteenToTwenty.progressFraction(start - 60_000L))
        assertEquals(0f, eighteenToTwenty.progressFraction(start)!!, 0.001f)
        assertEquals(0.5f, eighteenToTwenty.progressFraction(start + 3_600_000L)!!, 0.001f)
        assertEquals(1f, eighteenToTwenty.progressFraction(start + 7_200_000L)!!, 0.001f)
        assertNull(eighteenToTwenty.progressFraction(start + 7_200_001L))
    }

    @Test
    fun `tracks running and ended state`() {
        val start = acrossMidnight.startEpochMillis!!
        assertFalse(acrossMidnight.isRunning(start - 1L))
        assertTrue(acrossMidnight.isRunning(start + 60_000L))
        assertFalse(acrossMidnight.hasEnded(start + 60_000L))
        assertTrue(acrossMidnight.hasEnded(start + 7_200_001L))
    }

    @Test
    fun `an outage with no reported end never expires`() {
        val openEnded = requireNotNull(OutageSchedule.parse("1404/06/22", "18:00", null))
        val start = openEnded.startEpochMillis!!
        assertTrue(openEnded.isRunning(start + JalaliDateTime.DAY_MILLIS))
        assertFalse(openEnded.hasEnded(start + JalaliDateTime.DAY_MILLIS))
    }

    @Test
    fun `counts minutes until the start`() {
        val start = eighteenToTwenty.startEpochMillis!!
        assertEquals(90L, eighteenToTwenty.minutesUntilStart(start - 90 * 60_000L))
        assertEquals(-5L, eighteenToTwenty.minutesUntilStart(start + 5 * 60_000L))
    }

    @Test
    fun `is null when the date itself is unusable`() {
        assertNull(OutageSchedule.parse(null, "18:00", "20:00"))
        assertNull(OutageSchedule.parse("", "18:00", "20:00"))
        assertNull(OutageSchedule.parse("1404/13/40", "18:00", "20:00"))
    }
}
