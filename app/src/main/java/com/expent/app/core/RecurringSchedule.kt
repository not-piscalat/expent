package com.expent.app.core

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/** How often a recurring template repeats. */
@Serializable
enum class RecurringFrequency { MONTHLY, WEEKLY }

/**
 * Pure schedule math for recurring transactions.
 *
 * Due dates are stored as epoch days. Monthly dates are clamped to the target
 * month's length (a "31st" template lands on Feb 28), and the day anchor is
 * preserved across months (Feb 28 -> Mar 31).
 */
object RecurringSchedule {

    /** First occurrence strictly after [today], so a fresh template never logs immediately. */
    fun firstDueDate(
        today: LocalDate,
        frequency: RecurringFrequency,
        dayOfMonth: Int,
        dayOfWeek: Int
    ): LocalDate = when (frequency) {
        RecurringFrequency.MONTHLY -> {
            val firstOfMonth = today.withDayOfMonth(1)
            val candidate = firstOfMonth.withDayOfMonth(dayOfMonth.coerceIn(1, firstOfMonth.lengthOfMonth()))
            if (candidate.isAfter(today)) {
                candidate
            } else {
                val nextMonth = firstOfMonth.plusMonths(1)
                nextMonth.withDayOfMonth(dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth()))
            }
        }
        RecurringFrequency.WEEKLY -> {
            val target = DayOfWeek.of(dayOfWeek.coerceIn(1, 7))
            var candidate = today.plusDays(((target.value - today.dayOfWeek.value + 7) % 7).toLong())
            if (!candidate.isAfter(today)) candidate = candidate.plusWeeks(1)
            candidate
        }
    }

    /** The occurrence after [previous], keeping the month-day anchor for monthly schedules. */
    fun nextDueDate(
        previous: LocalDate,
        frequency: RecurringFrequency,
        dayOfMonth: Int
    ): LocalDate = when (frequency) {
        RecurringFrequency.MONTHLY -> {
            val nextMonth = previous.withDayOfMonth(1).plusMonths(1)
            nextMonth.withDayOfMonth(dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth()))
        }
        RecurringFrequency.WEEKLY -> previous.plusWeeks(1)
    }
}
