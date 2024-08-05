package com.microsoft.notes.richtext.render

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
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
class OtherSchemeExtensionsTest {
    lateinit var context: Context

    val SHORT_TEXT = "test"

    val FIRST_SPAN_START = 0
    val FIRST_SPAN_END = 2
    val FIRST_SPAN_FLAG = 10

    // Is the value of TextUtils.STRIKETHROUGH_SPAN but we don't have access to it
    val INTERNAL_STRIKETHROUGH_TYPE_SPAN = 5
    // Is the value of TextUtils.UNDERLINE_SPAN but we don't have access to it
    val INTERNAL_UNDERLINE_TYPE_SPAN = 6

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun should_set_Span_to_SpannableStringBuilder() {
        val spanBuilder = SpannableStringBuilder(SHORT_TEXT)
        val span = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)

        spanBuilder.setSpan(span)

        val resultSpanList = spanBuilder.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD))
    }

    @Test
    fun should_parse_SpanStyle_Bold_to_AndroidSpan() {
        (SpanStyle.BOLD.toAndroidTextStyleSpans().first() as StyleSpan).style.let {
            assertThat(it, iz(Typeface.BOLD))
        }
    }

    @Test
    fun should_parse_SpanStyle_Italic_to_AndroidSpan() {
        (SpanStyle.ITALIC.toAndroidTextStyleSpans().first() as StyleSpan).style.let {
            assertThat(it, iz(Typeface.ITALIC))
        }
    }

    @Test
    fun should_parse_SpanStyle_BoldItalic_to_AndroidSpan() {
        (SpanStyle.BOLD_ITALIC.toAndroidTextStyleSpans().first() as StyleSpan).style.let {
            assertThat(it, iz(Typeface.BOLD_ITALIC))
        }
    }

    @Test
    fun should_parse_SpanStyle_Underline_to_AndroidSpan() {
        (SpanStyle.UNDERLINE.toAndroidTextStyleSpans().first() as UnderlineSpan).spanTypeId.let {
            assertThat(it, iz(INTERNAL_UNDERLINE_TYPE_SPAN))
        }
    }

    @Test
    fun should_parse_SpanStyle_Strikethrough_to_AndroidSpan() {
        (SpanStyle.STRIKETHROUGH.toAndroidTextStyleSpans().first() as StrikethroughSpan).spanTypeId.let {
            assertThat(it, iz(INTERNAL_STRIKETHROUGH_TYPE_SPAN))
        }
    }

    @Test
    fun should_parse_Int_to_android_Text_Spannable_flag() {
        assertThat(
            toAndroidTextSpannableFlag(isAtBeginning = true),
            iz(Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        )
    }

    @Test
    fun should_parse_Int_to_android_Text_Spannable_flag_when_not_at_beginning() {
        assertThat(
            toAndroidTextSpannableFlag(isAtBeginning = false),
            iz(Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        )
    }
}
