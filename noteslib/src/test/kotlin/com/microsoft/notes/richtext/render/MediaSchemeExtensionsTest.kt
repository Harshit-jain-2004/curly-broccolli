package com.microsoft.notes.richtext.render

import android.content.Context
import android.os.Build
import android.text.style.ImageSpan
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.scheme.InlineMedia
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP),
    application = RoboTestApplication::class
)
class MediaSchemeExtensionsTest {
    lateinit var context: Context
    val LOCAL_ID = "localId"
    lateinit var localUrl: String
    lateinit var remoteUrl: String

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
        localUrl = context.getUriToResource(android.R.drawable.star_on).toString()
        remoteUrl = context.getUriToResource(android.R.drawable.star_off).toString()
    }

    @Test
    fun should_use_localUrl_when_no_remoteUrl_in_Media() {
        val media = InlineMedia(LOCAL_ID, localUrl = localUrl)

        val resultUrl = media.localOrRemoteUrl()
        assertThat(resultUrl, iz(localUrl))
    }

    @Test
    fun should_use_remoteUrl_when_no_localUrl_in_Media() {
        val media = InlineMedia(LOCAL_ID, remoteUrl = remoteUrl)

        val resultUrl = media.localOrRemoteUrl()
        assertThat(resultUrl, iz(remoteUrl))
    }

    @Test
    fun should_use_localUrl_when_have_also_remoteUrl_in_Media() {
        val media = InlineMedia(LOCAL_ID, localUrl = localUrl, remoteUrl = remoteUrl)

        val resultUrl = media.localOrRemoteUrl()
        assertThat(resultUrl, iz(localUrl))
    }

    @Test
    fun should_return_null_as_url_when_no_localUrl_not_remoteUrl_in_Media() {
        val media = InlineMedia(LOCAL_ID)

        val resultUrl = media.localOrRemoteUrl()
        assertThat(resultUrl, iz(nullValue()))
    }

    @Test
    fun should_create_a_final_string_from_Media_with_LocalUrl() {
        val block = InlineMedia(LOCAL_ID, localUrl = localUrl)

        val result = block.parse(createContextWithMockedString())
        val resultSpanList = result.getSpans(IMAGE_SPAN_START, IMAGE_SPAN_END, ImageSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.drawable, iz(not(nullValue())))
    }

    private fun createContextWithMockedString(): Context {
        return mock {
            on { getString(any()) } doReturn "Image"
        }
    }

    @Test
    fun should_create_a_final_string_from_Media_with_RemoteUrl() {
        val block = InlineMedia(LOCAL_ID, remoteUrl = remoteUrl)

        val result = block.parse(createContextWithMockedString())
        val resultSpanList = result.getSpans(IMAGE_SPAN_START, IMAGE_SPAN_END, ImageSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.drawable, iz(not(nullValue())))
    }

    @Test
    fun should_create_a_final_string_from_Media_with_LocalUrl_having_RemoteUrl_too() {
        val block = InlineMedia(LOCAL_ID, localUrl = localUrl, remoteUrl = remoteUrl)

        val result = block.parse(createContextWithMockedString())
        val resultSpanList = result.getSpans(IMAGE_SPAN_START, IMAGE_SPAN_END, ImageSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.drawable, iz(not(nullValue())))
    }

    @Test
    fun should_create_a_final_string_from_Media_without_LocalUrl_nor_RemoteUrl() {
        val block = InlineMedia(LOCAL_ID)

        val result = block.parse(createContextWithMockedString())
        assertThat(result.toString(), iz(NEW_LINE_CHAR_WITH_SPACE))
    }
}
