package com.microsoft.notes.utils.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

const val UTC = "UTC"
const val ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"

private fun createUTCISO8601DateFormat(): SimpleDateFormat {
    val timezone = TimeZone.getTimeZone(UTC)
    val dateFormat = SimpleDateFormat(ISO8601_FORMAT, Locale.US)
    dateFormat.timeZone = timezone
    return dateFormat
}

fun parseMillisToISO8601String(date: Long): String {
    return try {
        createUTCISO8601DateFormat().format(date)
    } catch (e: ParseException) {
        "0"
    }
}

fun parseISO8601StringToMillis(date: String): Long {
    return try {
        createUTCISO8601DateFormat().parse(date).time
    } catch (e: ParseException) {
        0L
    }
}
