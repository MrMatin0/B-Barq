package com.aliJafari.bbarq.domain.usecase

import com.aliJafari.bbarq.domain.status.OutageStatus
import com.aliJafari.bbarq.domain.status.ScheduleUrgency
import com.aliJafari.bbarq.domain.time.JalaliDateTime
import com.aliJafari.bbarq.domain.time.OutageSchedule
import org.junit.Assert.assertEquals
import org.junit.Test

class GetOutageStatusUseCaseTest {

    private val useCase = GetOutageStatusUseCase()

    /** 1404/06/22, 23:00 to 01:00: also exercises the midnight wrap. */
    private val schedule = requireNotNull(OutageSchedule.parse("1404/06/22", "23:00", "01:00"))
    private val start = schedule.startEpochMillis!!

    private fun minutesBefore(minutes: Long): Long = start - minutes * 60_000L

    @Test
    fun `no schedule is unknown`() {
        assertEquals(OutageStatus.Unknown, useCase(null))
    }

    @Test
    fun `ongoing while running`() {
        assertEquals(OutageStatus.Ongoing, useCase(schedule, start))
        assertEquals(OutageStatus.Ongoing, useCase(schedule, start + 60 * 60_000L))
    }

    @Test
    fun `ended after the wrapped end`() {
        assertEquals(OutageStatus.Ended, useCase(schedule, schedule.endEpochMillis!! + 1L))
    }

    @Test
    fun `minutes when under an hour away`() {
        assertEquals(OutageStatus.StartsInMinutes(45), useCase(schedule, minutesBefore(45)))
        assertEquals(
            ScheduleUrgency.SOON,
            useCase(schedule, minutesBefore(45)).urgency,
        )
    }

    @Test
    fun `rounds up from fifty minutes past the hour`() {
        // 1h50m reads as "in 2 hours", matching the shipped behaviour.
        assertEquals(OutageStatus.StartsInHours(2), useCase(schedule, minutesBefore(110)))
        assertEquals(OutageStatus.StartsInHours(1), useCase(schedule, minutesBefore(55)))
    }

    @Test
    fun `drops minutes within ten of a whole hour`() {
        assertEquals(OutageStatus.StartsInHours(2), useCase(schedule, minutesBefore(125)))
    }

    @Test
    fun `keeps hours and minutes otherwise`() {
        assertEquals(
            OutageStatus.StartsInHoursAndMinutes(2, 30),
            useCase(schedule, minutesBefore(150)),
        )
    }

    @Test
    fun `urgency drops to today beyond three hours`() {
        val fourHoursOut = useCase(schedule, minutesBefore(240))
        assertEquals(OutageStatus.StartsInHours(4), fourHoursOut)
        assertEquals(ScheduleUrgency.TODAY, fourHoursOut.urgency)
        assertEquals(ScheduleUrgency.SOON, useCase(schedule, minutesBefore(150)).urgency)
    }

    @Test
    fun `tomorrow and later are day granular`() {
        val nextDay = requireNotNull(OutageSchedule.parse("1404/06/23", "01:00", "03:00"))
        val noon = JalaliDateTime(1404, 6, 22, 12, 0).toEpochMillis()
        assertEquals(OutageStatus.Tomorrow, useCase(nextDay, noon))

        val nextMonth = requireNotNull(OutageSchedule.parse("1404/07/01", "18:30", "20:30"))
        assertEquals(OutageStatus.InDays(10), useCase(nextMonth, noon))
    }

    @Test
    fun `falls back to day level when no start time is reported`() {
        val dayOnly = requireNotNull(OutageSchedule.parse("1404/06/22", null, null))
        val noon = JalaliDateTime(1404, 6, 22, 12, 0).toEpochMillis()

        assertEquals(OutageStatus.Today, useCase(dayOnly, noon))
        assertEquals(ScheduleUrgency.SOON, useCase(dayOnly, noon).urgency)
        assertEquals(
            OutageStatus.Ended,
            useCase(dayOnly, JalaliDateTime(1404, 6, 23, 12, 0).toEpochMillis()),
        )
        assertEquals(
            OutageStatus.Tomorrow,
            useCase(dayOnly, JalaliDateTime(1404, 6, 21, 12, 0).toEpochMillis()),
        )
        assertEquals(
            OutageStatus.InDays(3),
            useCase(dayOnly, JalaliDateTime(1404, 6, 19, 12, 0).toEpochMillis()),
        )
    }
}
