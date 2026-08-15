package com.expent.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringScheduleTest {

    // --- firstDueDate: first occurrence strictly after today ---

    @Test
    fun `monthly first due is this month when the day is still ahead`() {
        val today = LocalDate.of(2026, 1, 15)
        assertEquals(
            LocalDate.of(2026, 1, 20),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.MONTHLY, dayOfMonth = 20, dayOfWeek = 1)
        )
    }

    @Test
    fun `monthly first due never lands on or before today`() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals(
            LocalDate.of(2026, 2, 1),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.MONTHLY, dayOfMonth = 1, dayOfWeek = 1)
        )
        assertEquals(
            LocalDate.of(2026, 1, 15),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.MONTHLY, dayOfMonth = 15, dayOfWeek = 1)
        )
    }

    @Test
    fun `monthly first due clamps to short months`() {
        val today = LocalDate.of(2026, 2, 10) // non-leap Feb
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.MONTHLY, dayOfMonth = 31, dayOfWeek = 1)
        )
        assertEquals(
            LocalDate.of(2026, 3, 31),
            RecurringSchedule.firstDueDate(
                LocalDate.of(2026, 3, 1), RecurringFrequency.MONTHLY, dayOfMonth = 31, dayOfWeek = 1
            )
        )
    }

    @Test
    fun `monthly first due on the due day itself moves to next month`() {
        val today = LocalDate.of(2026, 3, 31)
        assertEquals(
            LocalDate.of(2026, 4, 30), // April has 30 days
            RecurringSchedule.firstDueDate(today, RecurringFrequency.MONTHLY, dayOfMonth = 31, dayOfWeek = 1)
        )
    }

    @Test
    fun `weekly first due is this week when the weekday is still ahead`() {
        // Tuesday, 2026-01-06; Friday = 5
        val today = LocalDate.of(2026, 1, 6)
        assertEquals(
            LocalDate.of(2026, 1, 9),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.WEEKLY, dayOfMonth = 1, dayOfWeek = 5)
        )
    }

    @Test
    fun `weekly first due on the same weekday moves a full week ahead`() {
        // Monday, 2026-01-05
        val today = LocalDate.of(2026, 1, 5)
        assertEquals(
            LocalDate.of(2026, 1, 12),
            RecurringSchedule.firstDueDate(today, RecurringFrequency.WEEKLY, dayOfMonth = 1, dayOfWeek = 1)
        )
    }

    // --- nextDueDate: the occurrence after a given due date ---

    @Test
    fun `monthly next due keeps the day anchor across short months`() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RecurringSchedule.nextDueDate(LocalDate.of(2026, 1, 31), RecurringFrequency.MONTHLY, dayOfMonth = 31)
        )
        assertEquals(
            LocalDate.of(2026, 3, 31),
            RecurringSchedule.nextDueDate(LocalDate.of(2026, 2, 28), RecurringFrequency.MONTHLY, dayOfMonth = 31)
        )
        assertEquals(
            LocalDate.of(2026, 4, 30),
            RecurringSchedule.nextDueDate(LocalDate.of(2026, 3, 31), RecurringFrequency.MONTHLY, dayOfMonth = 31)
        )
    }

    @Test
    fun `monthly next due rolls over the year boundary`() {
        assertEquals(
            LocalDate.of(2027, 1, 31),
            RecurringSchedule.nextDueDate(LocalDate.of(2026, 12, 31), RecurringFrequency.MONTHLY, dayOfMonth = 31)
        )
    }

    @Test
    fun `weekly next due is exactly seven days later`() {
        assertEquals(
            LocalDate.of(2026, 1, 12),
            RecurringSchedule.nextDueDate(LocalDate.of(2026, 1, 5), RecurringFrequency.WEEKLY, dayOfMonth = 1)
        )
    }

    // --- resumeDueDate: skipping occurrences missed while paused ---

    @Test
    fun `resume keeps a due date still in the future`() {
        val today = LocalDate.of(2026, 1, 15)
        val future = LocalDate.of(2026, 1, 20).toEpochDay()
        assertEquals(
            future,
            RecurringSchedule.resumeDueDate(future, today, RecurringFrequency.MONTHLY, dayOfMonth = 20, dayOfWeek = 1)
        )
    }

    @Test
    fun `resume skips occurrences missed while paused`() {
        val today = LocalDate.of(2026, 1, 15)
        assertEquals(
            LocalDate.of(2026, 1, 20).toEpochDay(),
            RecurringSchedule.resumeDueDate(
                LocalDate.of(2026, 1, 10).toEpochDay(), today, RecurringFrequency.MONTHLY, dayOfMonth = 20, dayOfWeek = 1
            )
        )
    }

    @Test
    fun `resume on the due day itself moves to the next occurrence`() {
        val today = LocalDate.of(2026, 1, 15)
        assertEquals(
            LocalDate.of(2026, 2, 15).toEpochDay(),
            RecurringSchedule.resumeDueDate(
                today.toEpochDay(), today, RecurringFrequency.MONTHLY, dayOfMonth = 15, dayOfWeek = 1
            )
        )
    }

    @Test
    fun `resume skips missed weekly occurrences`() {
        val today = LocalDate.of(2026, 1, 12) // Monday
        assertEquals(
            LocalDate.of(2026, 1, 19).toEpochDay(),
            RecurringSchedule.resumeDueDate(
                LocalDate.of(2026, 1, 5).toEpochDay(), today, RecurringFrequency.WEEKLY, dayOfMonth = 1, dayOfWeek = 1
            )
        )
    }
}
