package com.microsoft.notes.richtext.render

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.microsoft.notes.noteslib.BuildConfig
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.NEW_LINE_CHAR_AS_STR
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
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
class ParagraphSchemeExtensionsTest {
    lateinit var context: Context
    val LOCAL_ID = "localId"
    val SHORT_TEXT = "test"
    val LONG_TEXT = "This is a long test and we are going to have fun unit testing it"

    val FIRST_SPAN_START = 0
    val FIRST_SPAN_END = 2
    val FIRST_SPAN_FLAG = 10

    val SECOND_SPAN_START = 10
    val SECOND_SPAN_END = 20
    val SECOND_SPAN_FLAG = 8

    // Is the value of TextUtils.STRIKETHROUGH_SPAN but we don't have access to it
    val INTERNAL_STRIKETHROUGH_TYPE_SPAN = 5
    // Is the value of TextUtils.UNDERLINE_SPAN but we don't have access to it
    val INTERNAL_UNDERLINE_TYPE_SPAN = 6

    @Before
    fun setup() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_without_spans() {
        val style = ParagraphStyle(false)
        val content = Content(SHORT_TEXT)
        val block = Paragraph(LOCAL_ID, style = style, content = content)

        val result = block.parse()
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_with_one_BOLD_span() {
        val result = parseDefinedParagraph(false, SpanStyle.BOLD)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD))
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_with_one_ITALIC_span() {
        val result = parseDefinedParagraph(false, SpanStyle.ITALIC)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.ITALIC))
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_with_one_BOLD_ITALIC_span() {
        val result = parseDefinedParagraph(false, SpanStyle.BOLD_ITALIC)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD_ITALIC))
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_with_one_UNDERLINE_span() {
        val result = parseDefinedParagraph(false, SpanStyle.UNDERLINE)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, UnderlineSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.spanTypeId, iz(INTERNAL_UNDERLINE_TYPE_SPAN))
    }

    @Test
    fun should_create_a_final_string_given_a_normal_Paragraph_with_one_STRIKETHROUGH_span() {
        val result = parseDefinedParagraph(false, SpanStyle.STRIKETHROUGH)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StrikethroughSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.spanTypeId, iz(INTERNAL_STRIKETHROUGH_TYPE_SPAN))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_without_spans() {
        val result = parseDefinedParagraph(true)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_one_BOLD_Span() {
        val result = parseDefinedParagraph(true, SpanStyle.BOLD)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_one_ITALIC_Span() {
        val result = parseDefinedParagraph(true, SpanStyle.ITALIC)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.ITALIC))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_one_BOLD_ITALIC_Span() {
        val result = parseDefinedParagraph(true, SpanStyle.BOLD_ITALIC)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.style, iz(Typeface.BOLD_ITALIC))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_one_UNDERLINE_Span() {
        val result = parseDefinedParagraph(true, SpanStyle.UNDERLINE)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, UnderlineSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.spanTypeId, iz(INTERNAL_UNDERLINE_TYPE_SPAN))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_one_STRIKETHROUGH_Span() {
        val result = parseDefinedParagraph(true, SpanStyle.STRIKETHROUGH)
        assertThat(result.toString(), iz(SHORT_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StrikethroughSpan::class.java)
        assertThat(resultSpanList.size, iz(1))
        val resultSpan = resultSpanList.component1()
        assertThat(resultSpan.spanTypeId, iz(INTERNAL_STRIKETHROUGH_TYPE_SPAN))
    }

    @Test
    fun should_create_a_final_string_given_an_unordered_Paragraph_with_two_Span() {
        val style = ParagraphStyle(true)
        val span1 = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val span2 = Span(SpanStyle.ITALIC, SECOND_SPAN_START, SECOND_SPAN_END, SECOND_SPAN_FLAG)
        val content = Content(LONG_TEXT, listOf(span1, span2))
        val block = Paragraph(LOCAL_ID, style = style, content = content)

        val result = block.parse()
        assertThat(result.toString(), iz(LONG_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList = result.getSpans(FIRST_SPAN_START, SECOND_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList.size, iz(2))
        val resultSpan1 = resultSpanList.component1()
        assertThat(resultSpan1.style, iz(Typeface.BOLD))
        val resultSpan2 = resultSpanList.component2()
        assertThat(resultSpan2.style, iz(Typeface.ITALIC))
    }

    @Test
    fun should_create_a_final_string_given_a_Paragraph_with_two_different_styled_Span() {
        val style = ParagraphStyle(false)
        val span1 = Span(SpanStyle.BOLD, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        val span2 = Span(SpanStyle.UNDERLINE, SECOND_SPAN_START, SECOND_SPAN_END, SECOND_SPAN_FLAG)
        val content = Content(LONG_TEXT, listOf(span1, span2))
        val block = Paragraph(LOCAL_ID, style = style, content = content)

        val result = block.parse()
        assertThat(result.toString(), iz(LONG_TEXT + NEW_LINE_CHAR_AS_STR))

        val resultSpanList1 = result.getSpans(FIRST_SPAN_START, FIRST_SPAN_END, StyleSpan::class.java)
        assertThat(resultSpanList1.size, iz(1))
        val resultSpan1 = resultSpanList1.component1()
        assertThat(resultSpan1.style, iz(Typeface.BOLD))

        val resultSpanList2 = result.getSpans(SECOND_SPAN_START, SECOND_SPAN_END, UnderlineSpan::class.java)
        assertThat(resultSpanList2.size, iz(1))
        val resultSpan2 = resultSpanList2.component1()
        assertThat(resultSpan2.spanTypeId, iz(INTERNAL_UNDERLINE_TYPE_SPAN))
    }

    // --------------------------------------------------------------------------------------------//

    private fun parseDefinedParagraph(unorderedList: Boolean, spanStyle: SpanStyle?): SpannableStringBuilder {
        val span = if (spanStyle != null) {
            Span(spanStyle, FIRST_SPAN_START, FIRST_SPAN_END, FIRST_SPAN_FLAG)
        } else {
            null
        }
        return parseDefinedParagraph(unorderedList, span)
    }

    private fun parseDefinedParagraph(unorderedList: Boolean, span: Span? = null): SpannableStringBuilder {
        val style = ParagraphStyle(unorderedList)
        val content = if (span != null) {
            Content(SHORT_TEXT, listOf(span))
        } else {
            Content(SHORT_TEXT, emptyList())
        }
        val block = Paragraph(LOCAL_ID, style = style, content = content)

        return block.parse()
    }
}
