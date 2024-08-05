package com.microsoft.notes.ui.feed.recyclerview

import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import org.hamcrest.CoreMatchers.`is` as iz

class TimeBucketUtilsTest {

    @Test
    fun `getCurrentDayStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 6, 15, 20, 40, 12)

        val expected = GregorianCalendar(2020, 6, 15)
        assertThat(getCurrentDayStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getYesterdayStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 6, 15, 20, 40, 12)

        val expected = GregorianCalendar(2020, 6, 14)
        assertThat(getYesterdayStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getYesterdayStart first day of month test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 6, 1, 20, 40, 12)

        val expected = GregorianCalendar(2020, 5, 30)
        assertThat(getYesterdayStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getWeekStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 19, 20, 40, 12) // Friday

        val expected = GregorianCalendar(2020, 5, 14)
        if (expected.firstDayOfWeek == Calendar.MONDAY) {
            expected.add(Calendar.DAY_OF_MONTH, 1)
        }
        assertThat(getCurrentWeekStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getWeekStart near month start test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 2, 20, 40, 12) // Tuesday

        val expected = GregorianCalendar(2020, 4, 31)
        if (expected.firstDayOfWeek == Calendar.MONDAY) {
            expected.add(Calendar.DAY_OF_MONTH, 1)
        }
        assertThat(getCurrentWeekStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getLastWeekStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 19, 20, 40, 12) // Friday

        val expected = GregorianCalendar(2020, 5, 7)
        if (expected.firstDayOfWeek == Calendar.MONDAY) {
            expected.add(Calendar.DAY_OF_MONTH, 1)
        }
        assertThat(getLastWeekStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getLastWeekStart near month start test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 2, 20, 40, 12) // Tuesday

        val expected = GregorianCalendar(2020, 4, 24)
        if (expected.firstDayOfWeek == Calendar.MONDAY) {
            expected.add(Calendar.DAY_OF_MONTH, 1)
        }
        assertThat(getLastWeekStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getCurrentMonthStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 21, 20, 40, 12)

        val expected = GregorianCalendar(2020, 5, 1)
        assertThat(getCurrentMonthStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getLastMonthStart test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 5, 21, 20, 40, 12)

        val expected = GregorianCalendar(2020, 4, 1)
        assertThat(getLastMonthStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }

    @Test
    fun `getLastMonthStart near year start test`() {
        val currentTime = GregorianCalendar()
        currentTime.set(2020, 1, 21, 20, 40, 12)

        val expected = GregorianCalendar(2019, 12, 1)
        assertThat(getLastMonthStart(currentTime.timeInMillis), iz(expected.timeInMillis))
    }
}
