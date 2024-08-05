package com.microsoft.notes.richtext.render

import android.content.Context
import android.net.Uri
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
import org.hamcrest.CoreMatchers.`is` as iz

@Ignore("Ignore this test.We will enable it again once we move to androidX and update robolectric version")
@RunWith(RobolectricTestRunner::class)
@Config(
    constants = BuildConfig::class,
    sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP),
    application = RoboTestApplication::class
)
class FrameworkExtensionsTest {
    lateinit var context: Context

    val DRAWABLE_NAME = "star_on"
    val ANDROID_DRAWABLE_PATH = "android.resource://android/drawable/"

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun should_get_correct_uri_from_resource() {
        val uri = context.getUriToResource(android.R.drawable.star_on)
        val expectedUri = Uri.parse(ANDROID_DRAWABLE_PATH + DRAWABLE_NAME)

        assertThat(uri, iz(expectedUri))
    }
}
