package com.microsoft.notes.utils

import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun epochToDate(epochMillis: Long): String {
    val date = Date(epochMillis)
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(date)
}

fun epochToTime(epochMillis: Long): String {
    val date = Date(epochMillis)
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}

fun isToday(localDateTime: ZonedDateTime): Boolean =
    localDateTime.toLocalDate().isEqual(LocalDate.now())

fun isTomorrow(localDateTime: ZonedDateTime): Boolean =
    localDateTime.toLocalDate().isEqual(LocalDate.now().plusDays(1))

fun dateTimeToTimeString(localDateTime: ZonedDateTime): String {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return localDateTime.format(formatter)
}
