package com.microsoft.notes.richtext.scheme

import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ContentTest {

    @Test(expected = IndexOutOfBoundsException::class)
    fun should_throw_exception_when_start_is_negative() {
        Span(
            style = SpanStyle.BOLD, start = -1, end = 10,
            flag = 0
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun should_throw_exception_when_end_is_negative() {
        Span(
            style = SpanStyle.BOLD, start = 0, end = -1, flag = 0
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun should_throw_exception_when_start_and_end_are_negative() {
        Span(
            style = SpanStyle.BOLD, start = -1, end = -1,
            flag = 0
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun should_throw_exception_when_start_is_greater_than_end() {
        Span(
            style = SpanStyle.BOLD, start = 10, end = 5, flag = 0
        )
    }

    @Test
    fun should_create_correct_span_when_start_and_end_are_the_same() {
        val span = Span(
            style = SpanStyle.BOLD, start = 10, end = 10,
            flag = 0
        )
        assertThat(span, iz(notNullValue()))
    }

    @Test
    fun should_create_correct_span_when_start_is_lower_than_end() {
        val span = Span(
            style = SpanStyle.BOLD, start = 5, end = 10, flag = 0
        )
        assertThat(span, iz(notNullValue()))
    }

    @Test
    fun should_create_content_with_text() {
        val TEXT = "Hello"
        val content = Content(TEXT)

        assertThat(content, iz(notNullValue()))
        assertThat(content.text, iz(TEXT))
        assertThat(content.spans, iz(emptyList()))
    }

    @Test
    fun should_create_Content_with_spans() {
        val TEXT = "Hello"
        val SPAN1 = Span(
            style = SpanStyle.BOLD, start = 5, end = 10, flag = 0
        )
        val SPAN2 = Span(
            style = SpanStyle.UNDERLINE, start = 15, end = 20,
            flag = 0
        )
        val content = Content(TEXT, listOf(SPAN1, SPAN2))

        assertThat(content, iz(notNullValue()))
        assertThat(content.text, iz(TEXT))
        assertThat(content.spans.size, iz(2))
        assertThat(content.spans.component1(), iz(SPAN1))
        assertThat(content.spans.component2(), iz(SPAN2))
    }

    @Test
    fun should_normalize_spans() {
        val validSpan = Span(
            SpanStyle(bold = true), start = 5, end = 10, flag = 0
        )
        val wrongSpan = Span(
            SpanStyle(italic = true), start = 18, end = 30, flag = 0
        )
        val text = "Hello my name is Cesar"
        val normalizedSpans = listOf(validSpan, wrongSpan).normalize(text)

        with(normalizedSpans) {
            assertThat(size, iz(2))
            assertThat(component1(), iz(validSpan))
            assertThat(component2().start, iz(wrongSpan.start))
            assertThat(component2().end, iz(text.length))
        }
    }

    @Test
    fun should_normalize_overlapped_spans() {
        val validSpan = Span(
            SpanStyle(bold = true), start = 5, end = 10, flag = 0
        )
        val wrongSpan = Span(
            SpanStyle(italic = true), start = 8, end = 30, flag = 0
        )
        val text = "Hello my name is Cesar"
        val normalizedSpans = listOf(validSpan, wrongSpan).normalize(text)

        with(normalizedSpans) {
            assertThat(size, iz(2))
            assertThat(component1(), iz(validSpan))
            assertThat(component2().start, iz(wrongSpan.start))
            assertThat(component2().end, iz(text.length))
        }
    }

    @Test
    fun should_normalize_overlapped_and_long_spans() {
        val wrongSpan1 = Span(
            SpanStyle(bold = true), start = 5, end = 35, flag = 0
        )
        val wrongSpan2 = Span(
            SpanStyle(italic = true), start = 8, end = 30, flag = 0
        )
        val text = "Hello my name is Cesar"
        val normalizedSpans = listOf(wrongSpan1, wrongSpan2).normalize(text)

        with(normalizedSpans) {
            assertThat(size, iz(2))
            assertThat(component1().start, iz(wrongSpan1.start))
            assertThat(component1().end, iz(text.length))
            assertThat(component2().start, iz(wrongSpan2.start))
            assertThat(component2().end, iz(text.length))
        }
    }

    @Test
    fun should_create_Content_with_normalized_spans() {
        val validSpan = Span(
            SpanStyle(bold = true), start = 5, end = 10, flag = 0
        )
        val wrongSpan = Span(
            SpanStyle(italic = true), start = 8, end = 30, flag = 0
        )
        val text = "Hello my name is Cesar"
        val content = Content(text = text, spans = emptyList())
        val contentWithNormalizedSpans = content.copyAndNormalizeSpans(text, listOf(validSpan, wrongSpan))
        with(contentWithNormalizedSpans.spans) {
            assertThat(size, iz(2))
            assertThat(component1(), iz(validSpan))
            assertThat(component2().start, iz(wrongSpan.start))
            assertThat(component2().end, iz(text.length))
        }
    }
}
