package com.microsoft.notes.noteslib

import com.microsoft.notes.utils.logging.TestConstants
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class GlobalPollerTest {

    lateinit var globalPoller: GlobalPoller

    @Before
    fun setup() {
        globalPoller = GlobalPoller(false, false, false, false, false, TestConstants.TEST_USER_ID, dispatch = {})
    }

    @Test
    fun should_start_polling() {

        with(globalPoller) {
            startPolling()
            assertThat(timers.isNotEmpty(), iz(true))
            assertThat(timers.size, iz(3))
        }
    }

    @Test
    fun should_stop_polling() {
        with(globalPoller) {
            startPolling()

            stopPolling()
            assertThat(timers.isEmpty(), iz(true))
        }
    }

    @Test
    fun should_check_if_polling_is_running() {
        with(globalPoller) {
            startPolling()
            assertThat(isPollingRunning(), iz(true))

            stopPolling()
            assertThat(isPollingRunning(), iz(false))
        }
    }
}
