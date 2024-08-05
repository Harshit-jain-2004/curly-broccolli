package com.microsoft.notes.ui.feed.recyclerview

import android.content.Context
import com.microsoft.notes.noteslib.R
import java.util.Calendar
import java.util.GregorianCalendar

sealed class TimeBucket(
    val id: String,
    private val intervalStartInclusive: Long,
    private val intervalEndExclusive: Long
) {
    class Empty : TimeBucket(
        id = ID_EMPTY,
        intervalStartInclusive = 0,
        intervalEndExclusive = 0
    ) {
        override fun getTitle(context: Context) = ""
    }

    class Future(currentTimeMillis: Long) : TimeBucket(
        id = ID_FUTURE,
        intervalStartInclusive = currentTimeMillis,
        intervalEndExclusive = Long.MAX_VALUE
    ) {
        override fun getTitle(context: Context) = ""
    }

    class Today(currentTimeMillis: Long) : TimeBucket(
        id = ID_TODAY,
        intervalStartInclusive = getCurrentDayStart(currentTimeMillis),
        intervalEndExclusive = currentTimeMillis
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_today)
    }

    class Yesterday(currentTimeMillis: Long) : TimeBucket(
        id = ID_YESTERDAY,
        intervalStartInclusive = getYesterdayStart(currentTimeMillis),
        intervalEndExclusive = getCurrentDayStart(currentTimeMillis)
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_yesterday)
    }

    class ThisWeek(currentTimeMillis: Long) : TimeBucket(
        id = ID_THIS_WEEK,
        intervalStartInclusive = getCurrentWeekStart(currentTimeMillis),
        intervalEndExclusive = currentTimeMillis
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_this_week)
    }

    class LastWeek(currentTimeMillis: Long) : TimeBucket(
        id = ID_LAST_WEEK,
        intervalStartInclusive = getLastWeekStart(currentTimeMillis),
        intervalEndExclusive = getCurrentWeekStart(currentTimeMillis)
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_last_week)
    }

    class ThisMonth(currentTimeMillis: Long) : TimeBucket(
        id = ID_THIS_MONTH,
        intervalStartInclusive = getCurrentMonthStart(currentTimeMillis),
        intervalEndExclusive = currentTimeMillis
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_this_month)
    }

    class LastMonth(currentTimeMillis: Long) : TimeBucket(
        id = ID_LAST_MONTH,
        intervalStartInclusive = getLastMonthStart(currentTimeMillis),
        intervalEndExclusive = getCurrentMonthStart(currentTimeMillis)
    ) {
        override fun getTitle(context: Context) =
            context.getString(R.string.title_last_month)
    }

    class Month(year: Int, private val month: Int) : TimeBucket(
        id = "${ID_MONTH}_${month}_$year",
        intervalStartInclusive = getMonthStart(year, month),
        intervalEndExclusive = getNextMonthStart(year, month)
    ) {
        override fun getTitle(context: Context) = getMonthName(month)
        companion object {
            fun create(timeMillis: Long): Month =
                Month(getYearFromTime(timeMillis), getMonthFromTime(timeMillis))
        }
    }

    class Year(private val year: Int) : TimeBucket(
        id = "${ID_YEAR}_$year",
        intervalStartInclusive = getYearStart(year),
        intervalEndExclusive = getYearStart(year + 1)
    ) {
        override fun getTitle(context: Context) = year.toString()
        companion object {
            fun create(timeMillis: Long) =
                Year(getYearFromTime(timeMillis))
        }
    }

    fun contains(timeMillis: Long): Boolean =
        timeMillis >= intervalStartInclusive && timeMillis < intervalEndExclusive

    abstract fun getTitle(context: Context): String

    companion object {
        const val ID_EMPTY = "Empty"
        const val ID_FUTURE = "Future"
        const val ID_TODAY = "Today"
        const val ID_YESTERDAY = "Yesterday"
        const val ID_THIS_WEEK = "ThisWeek"
        const val ID_LAST_WEEK = "LastWeek"
        const val ID_THIS_MONTH = "ThisMonth"
        const val ID_LAST_MONTH = "LastMonth"
        const val ID_MONTH = "Month"
        const val ID_YEAR = "Year"

        /**
         *  Returns a list of timeBuckets
         *
         *  @param currentTimeMillis current time in millis to feed to the individual ranges
         *  @param nMonth number of additional past Month buckets to be appended
         *
         *  with nMonth 0, returns list of ranges that adds to a continuous timeSpan [LastMonthStart, forever)
         *  Also, bucket list returned satisfies the following property:
         *      Let B[k] be the kth item in the list returned and tk be a timestamp value
         *      if t1 > t2 and t1 doesn't belong to any of B[0], B[1],....B[k]
         *      then, t2 is also guaranteed to not belong to any of B[0], B[1],.....B[k]
         */
        fun getAllBuckets(currentTimeMillis: Long, nMonth: Int = 0): List<TimeBucket> {
            val list = mutableListOf(
                Future(currentTimeMillis),
                Today(currentTimeMillis),
                Yesterday(currentTimeMillis),
                ThisWeek(currentTimeMillis),
                LastWeek(currentTimeMillis),
                ThisMonth(currentTimeMillis),
                LastMonth(currentTimeMillis)
            )

            val calendar = GregorianCalendar()
            calendar.timeInMillis = getLastMonthStart(currentTimeMillis)

            for (i in 0 until nMonth) {
                calendar.add(Calendar.MONTH, -1)
                list.add(Month(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)))
            }
            return list
        }
    }
}
