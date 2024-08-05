package com.microsoft.notes.ui.feed.recyclerview

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.GregorianCalendar

fun getCurrentDayStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    return calendar.timeInMillis
}

fun getYesterdayStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    calendar.add(Calendar.DATE, -1)
    return calendar.timeInMillis
}

fun getCurrentWeekStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    return calendar.timeInMillis
}

fun getLastWeekStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.add(Calendar.WEEK_OF_MONTH, -1)
    return calendar.timeInMillis
}

fun getCurrentMonthStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}

fun getLastMonthStart(currentTimeMillis: Long): Long {
    val calendar = getCalendarAtDayStart(currentTimeMillis)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.add(Calendar.MONTH, -1)
    return calendar.timeInMillis
}

fun getMonthStart(year: Int, month: Int): Long = GregorianCalendar(year, month, 1).timeInMillis

fun getNextMonthStart(year: Int, month: Int): Long {
    val calendar = GregorianCalendar(year, month, 1)
    calendar.add(Calendar.MONTH, 1)
    return calendar.timeInMillis
}

fun getMonthFromTime(timeMillis: Long): Int {
    val calendar = GregorianCalendar()
    calendar.timeInMillis = timeMillis
    return calendar.get(Calendar.MONTH)
}

fun getYearStart(year: Int): Long = GregorianCalendar(year, 0, 1).timeInMillis

fun getYearFromTime(timeMillis: Long): Int {
    val calendar = GregorianCalendar()
    calendar.timeInMillis = timeMillis
    return calendar.get(Calendar.YEAR)
}

private fun getCalendarAtDayStart(currentTimeMillis: Long): Calendar {
    val calendar = GregorianCalendar()
    calendar.timeInMillis = currentTimeMillis

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

fun getMonthName(month: Int): String = DateFormatSymbols().months[month]

// Returns the next TimeBucket in the list defined by 'bucketsIterator' that contains the 'timeMillis'
// if 'timeMillis' is not found in the list, then TimeBucket.Year object containing the 'timeMillis' is returned
fun getNextBucket(timeMillis: Long, bucketsIterator: Iterator<TimeBucket>): TimeBucket {
    while (bucketsIterator.hasNext()) {
        val bucket = bucketsIterator.next()
        if (bucket.contains(timeMillis)) {
            return bucket
        }
    }
    return TimeBucket.Year.create(timeMillis)
}
