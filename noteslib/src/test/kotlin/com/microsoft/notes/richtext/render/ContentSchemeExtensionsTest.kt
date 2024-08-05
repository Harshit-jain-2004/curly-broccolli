package com.microsoft.notes.richtext.render

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.NEW_LINE_CHAR
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
class ContentSchemeExtensionsTest {
    lateinit var context: Context

    val TEXT_WITHOUT_NEW_LINE_CHAR = "This text doesn't have new line at the beginning"
    val TEXT_WITH_BLANK_SPACES_AT_THE_BEGINNING = "       This text has empty spaces at the beginning"

    val SHORT_TEXT = "test"
    val LONG_TEXT = "This is a long test and we are going to have fun unit testing it"

    val FIRST_SPAN_START = 0
    val FIRST_SPAN_END = 2
    val FIRST_SPAN_FLAG = 10

    val SECOND_SPAN_START = 10
    val SECOND_SPAN_END = 20
    val SECOND_SPAN_FLAG = 8

    // Is the value of TextUtils.UNDERLINE_SPAN but we don't have access to it
    val INTERNAL_UNDERLINE_TYPE_SPAN = 6

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun should_parse_Content_text_without_new_line_character() {
        val content = Content(TEXT_WITHOUT_NEW_LINE_CHAR)

        val result = content.parseText()
        assertThat(result, iz(TEXT_WITHOUT_NEW_LINE_CHAR + NEW_LINE_CHAR))
    }

    @Test
    fun should_parse_Content_text_with_one_new_line_character() {
        val content = Content(TEXT_WITHOUT_NEW_LINE_CHAR)

        val result = content.parseText()
        assertThat(result, iz(TEXT_WITHOUT_NEW_LINE_CHAR + NEW_LINE_CHAR))
    }

    @Test
    fun should_parse_Content_text_with_blank_spaces_at_the_beginning() {
        val content = Content(TEXT_WITH_BLANK_SPACES_AT_THE_BEGINNING)

        val result = content.parseText()
        assertThat(result, iz(TEXT_WITH_BLANK_SPACES_AT_THE_BEGINNING + NEW_LINE_CHAR))
    }

    @Test
    fun should_parse_Content_text_with_blank_spaces_at_the_beginning_and_new_line_char_at_the_end() {
        val content = Content(TEXT_WITH_BLANK_SPACES_AT_THE_BEGINNING)

        val result = content.parseText()
        assertThat(result, iz(TEXT_WITH_BLANK_SPACES_AT_THE_BEGINNING + NEW_LINE_CHAR))
    }

    @Test
    fun should_parse_Content_with_one_Span() {
        val span = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val content = Content(SHORT_TEXT, listOf(span))

        val result = content.parseSpan()

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))

        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD))
    }

    @Test
    fun should_parse_Content_with_two_Span() {
        val span1 = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val span2 = Span(SpanStyle.ITALIC, SECOND_SPAN_START, SECOND_SPAN_END, SECOND_SPAN_FLAG)
        val content = Content(LONG_TEXT, listOf(span1, span2))

        val result = content.parseSpan()

        val resultSpanList = result.getSpans(FIRST_SPAN_START, SECOND_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(2))

        val resultSpan1 = resultSpanList.component1()
        assertThat(resultSpan1.style, iz(Typeface.BOLD))

        val resultSpan2 = resultSpanList.component2()
        assertThat(resultSpan2.style, iz(Typeface.ITALIC))
    }

    @Test
    fun should_parse_Content_with_two_different_styled_Span() {
        val span1 = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val span2 = Span(SpanStyle.UNDERLINE, SECOND_SPAN_START, SECOND_SPAN_END, SECOND_SPAN_FLAG)
        val content = Content(LONG_TEXT, listOf(span1, span2))

        val result = content.parseSpan()

        val resultSpanList1 = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList1.size, iz(1))
        val resultSpan1 = resultSpanList1.component1()
        assertThat(resultSpan1.style, iz(Typeface.BOLD))

        val resultSpanList2 = result.getSpans(SECOND_SPAN_START, SECOND_SPAN_END, UnderlineSpan::class.java)
        assertThat(resultSpanList2.size, iz(1))
        val resultSpan2 = resultSpanList2.component1()
        assertThat(resultSpan2.spanTypeId, iz(INTERNAL_UNDERLINE_TYPE_SPAN))
    }
}
