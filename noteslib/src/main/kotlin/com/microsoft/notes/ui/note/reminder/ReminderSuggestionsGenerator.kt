package com.microsoft.notes.ui.note.reminder

import org.threeten.bp.DayOfWeek
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Logic to get time reminder suggestions
 * Rules copied from Todo app
 */

fun getTimeReminderSuggestions(currentDate: ZonedDateTime): List<ZonedDateTime> {
    val suggestions = mutableListOf<ZonedDateTime>()
    val laterToday = getReminderLaterTodaySuggestion(currentDate)

    if (laterToday != null) {
        suggestions.add(laterToday)
    }
    suggestions.add(getReminderTomorrowSuggestion(currentDate))
    val nextWeek = getReminderNextWeekSuggestion(currentDate)
    if (nextWeek != null) {
        suggestions.add(nextWeek)
    }

    return suggestions
}

fun getReminderLaterTodaySuggestion(currentDate: ZonedDateTime): ZonedDateTime? {
    // Threshold for disabling the suggestion is set to 8:29 PM of the current day
    var suggestionDate = currentDate
    val threshold = suggestionDate.withHour(20).withMinute(29).withSecond(0).withNano(0)
    if (suggestionDate.isAfter(threshold)) {
        return null
    }

    // If the minute is 29 or later, round up to the next hour
    if (suggestionDate.minute >= 29) {
        suggestionDate = suggestionDate.plusHours(1).withMinute(0)
    }

    // Add 3 hours to the rounded hour
    return suggestionDate.plusHours(3).withMinute(0).withSecond(0).withNano(0)
}

fun getReminderTomorrowSuggestion(currentDate: ZonedDateTime): ZonedDateTime {
    return currentDate.plusDays(1)
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
}

fun getReminderNextWeekSuggestion(currentDate: ZonedDateTime): ZonedDateTime? {
    val tomorrow = getReminderTomorrowSuggestion(currentDate)
    val firstDayOfTheWeek = DayOfWeek.MONDAY // Adjust if needed for different start of the week
    val nextWeek = currentDate.with(TemporalAdjusters.next(firstDayOfTheWeek))
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    return if (tomorrow.isEqual(nextWeek)) null else nextWeek
}
