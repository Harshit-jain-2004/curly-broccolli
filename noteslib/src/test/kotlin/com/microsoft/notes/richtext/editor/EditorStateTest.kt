package com.microsoft.notes.richtext.editor

import android.text.Spannable
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class EditorStateTest {

    @Test
    fun should_getFormatting_empty_selection_within_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 2, 0, 2))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_empty_selection_between_spans() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(
                                Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                                Span(SpanStyle.ITALIC, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            )
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 3, 0, 3))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_empty_selection_start_of_block() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 0, 0, 0))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_empty_selection_start_of_block_on_empty_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(
                                Span(SpanStyle.DEFAULT, 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE),
                                Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            )
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 0, 0, 0))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_empty_selection_not_touching_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 0, 5))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_empty_selection_end_of_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 3, 0, 3))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_empty_selection_beginning_of_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 1, 0, 1))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_selection_contained_in_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 2, 0, 3))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_selection_span_equality() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 1, 0, 5))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_selection_extending_beyond_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 3, 0, 7))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_selection_extending_beyond_span_at_span_start() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 1, 0, 7))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_selection_not_touching_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 0, 7))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_selection_end_of_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 1, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 3, 0, 7))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_selection_beginning_of_span() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 3, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 2, 0, 3))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_multi_paragraph_selection() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_multi_paragraph_selection_cover_all_content() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 0, 1, 10))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_multi_paragraph_selection_different_style_in_next_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            listOf(Span(SpanStyle.ITALIC, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 0, 1, 10))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_multi_paragraph_no_spans_in_range() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            emptyList()
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            emptyList()
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_multi_paragraph_spans_only_in_start_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            emptyList()
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_multi_paragraph_spans_only_in_end_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            emptyList()
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            listOf(Span(SpanStyle.BOLD, 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }

    @Test
    fun should_getFormatting_multi_paragraph_start_at_span_start() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 5, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            emptyList()
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_getFormatting_multi_paragraph_start_at_span_end() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(
                        content = Content(
                            "hello world",
                            listOf(Span(SpanStyle.BOLD, 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
                        )
                    ),
                    Paragraph(
                        content = Content(
                            "foo bar baz",
                            emptyList()
                        )
                    )
                )
            )
        )
        val formatting = state.getFormatting(Range(0, 5, 1, 5))
        assertThat(formatting, iz(SpanStyle.DEFAULT))
    }
}
