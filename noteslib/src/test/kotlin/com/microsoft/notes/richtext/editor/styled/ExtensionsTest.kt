package com.microsoft.notes.richtext.editor.styled

import android.content.Context
import android.os.Build
import com.microsoft.notes.noteslib.BuildConfig
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.TimeZone
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = [Build.VERSION_CODES.LOLLIPOP]
)
class ExtensionsTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun `should parse a Long into a RFC1123 date string`() {
        val date = 1536834130581L

        val result = context.parseMillisToRFC1123String(date, TimeZone.getTimeZone("UTC"))
        assertThat(result, iz("Sep 13, 2018, 10:22:10"))
    }
}
