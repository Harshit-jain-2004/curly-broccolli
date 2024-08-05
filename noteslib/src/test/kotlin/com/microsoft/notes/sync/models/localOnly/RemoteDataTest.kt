package com.microsoft.notes.sync.models.localOnly

import com.microsoft.notes.utils.utils.parseMillisToISO8601String
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class RemoteDataTest {

    @Test
    fun should_parse_Millis_to_ISO8601_String() {
        val unixTimeStamp: Long = 1517588769000
        val ISO8601String = "2018-02-02T16:26:09.0000000Z"

        val stringResult = parseMillisToISO8601String(unixTimeStamp)
        assertThat(stringResult, iz(ISO8601String))
    }
}
