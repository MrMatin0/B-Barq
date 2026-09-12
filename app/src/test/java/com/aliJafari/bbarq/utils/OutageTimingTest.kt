package com.aliJafari.bbarq.utils

import com.aliJafari.bbarq.data.model.Outage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Timing maths behind the countdown and the progress bar.
 *
 * Deliberately anchored to the outage's own start instant rather than to a
 * wall-clock literal, so the assertions do not depend on the machine's time
 * zone or on when the suite happens to run.
 */
class OutageTimingTest {

    private fun outage(
        date: String? = "1404/06/21",
        startTime: String? = "14:00",
        endTime: String? = "16:00",
    ) = Outage(
        id = 1,
        reason = null,
        date = date,
        time = null,
        startTime = startTime,
        endTime = endTime,
        billId = null,
        address = null,
    )

    @Test
    fun `duration is the gap between both ends`() {
        assertEquals(120, outage().durationMinutes())
    }

    @Test
    fun `duration wraps an outage that runs past midnight`() {
        assertEquals(120, outage(startTime = "23:00", endTime = "01:00").durationMinutes())
    }

    @Test
    fun `duration is unknown when the api omits an end`() {
        assertNull(outage(endTime = null).durationMinutes())
        assertNull(outage(endTime = "").durationMinutes())
    }

    @Test
    fun `duration is unknown when the date is unusable`() {
        assertNull(outage(date = null).durationMinutes())
        assertNull(outage(date = "not-a-date").durationMinutes())
    }

    @Test
    fun `progress is null before and after the outage`() {
        val subject = outage()
        val start = subject.startEpochMillis()
        assertTrue(start != -1L)

        assertNull(subject.progressFraction(start - 60_000L))
        assertNull(subject.progressFraction(start + (3 * 60 * 60 * 1000L)))
    }

    @Test
    fun `progress reaches the halfway point mid outage`() {
        val subject = outage()
        val start = subject.startEpochMillis()
        val halfway = start + (60 * 60 * 1000L)

        assertEquals(0.5f, subject.progressFraction(halfway)!!, 0.001f)
    }

    @Test
    fun `progress is clamped to the unit interval at the edges`() {
        val subject = outage()
        val start = subject.startEpochMillis()
        val end = subject.endEpochMillis()

        assertEquals(0f, subject.progressFraction(start)!!, 0.001f)
        assertEquals(1f, subject.progressFraction(end)!!, 0.001f)
    }

    @Test
    fun `start is unresolvable without a date`() {
        assertEquals(-1L, outage(date = null).startEpochMillis())
        assertEquals(-1L, outage(date = "").startEpochMillis())
    }
}
