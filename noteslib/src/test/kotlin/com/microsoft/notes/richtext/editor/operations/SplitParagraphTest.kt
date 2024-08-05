package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SplitParagraphTest {

    val FLAG = 0

    @Test
    fun should_splitBefore_keeps_spans_before_split_point() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 1, 2, FLAG),
            Span(SpanStyle.BOLD, 2, 3, FLAG),
            Span(SpanStyle.BOLD, 3, 4, FLAG),
            Span(SpanStyle.BOLD, 4, 5, FLAG)
        )
        val newSpans = spans.splitBefore(3)
        assertThat(newSpans, iz(listOf(spans[0], spans[1])))
    }

    @Test
    fun should_splitBefore_splits_span_overlapping_split_point() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))
        val newSpans = spans.splitBefore(2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
    }

    @Test
    fun should_splitBefore_keeps_empty_span_if_split_at_beginning() {
        val spans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val newSpans = spans.splitBefore(0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
    }

    @Test
    fun should_splitBefore_keeps_empty_span_from_empty_paragraph() {
        val spans = listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))
        val newSpans = spans.splitBefore(0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
    }

    @Test
    fun should_splitAfter_keeps_spans_after_split_point_and_adjusts() {
        val spans = listOf(
            Span(SpanStyle.ITALIC, 1, 2, FLAG),
            Span(SpanStyle.BOLD, 2, 3, FLAG),
            Span(SpanStyle.UNDERLINE, 3, 4, FLAG),
            Span(SpanStyle.BOLD_ITALIC, 4, 5, FLAG)
        )
        val newSpans = spans.splitAfter(3)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 0, FLAG),
                    Span(SpanStyle.UNDERLINE, 0, 1, FLAG),
                    Span(SpanStyle.BOLD_ITALIC, 1, 2, FLAG)
                )
            )
        )
    }

    @Test
    fun should_splitAfter_splits_span_overlapping_split_point() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))
        val newSpans = spans.splitAfter(2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))))
    }

    @Test
    fun should_splitAfter_keeps_empty_span_if_split_at_end() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))
        val newSpans = spans.splitAfter(3)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
    }

    @Test
    fun should_splitAfter_keeps_empty_span_if_split_at_end_with_empty_span() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 1, 3, FLAG),
            Span(SpanStyle.BOLD, 3, 3, FLAG)
        )
        val newSpans = spans.splitAfter(3)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
    }

    @Test
    fun should_splitAfter_keeps_empty_span_when_middle_of_paragraph() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 1, 3, FLAG),
            Span(SpanStyle.UNDERLINE, 3, 3, FLAG),
            Span(SpanStyle.BOLD, 3, 5, FLAG)
        )
        val newSpans = spans.splitAfter(3)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.UNDERLINE, 0, 0, FLAG),
                    Span(SpanStyle.BOLD, 0, 2, FLAG)
                )
            )
        )
    }

    @Test
    fun should_splitAfter_no_empty_span_if_not_adjacent() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))
        val newSpans = spans.splitAfter(4)
        assertThat(newSpans, iz(emptyList()))
    }

    @Test
    fun should_splitAfter_keeps_all_spans_if_split_at_zero_index() {
        val spans = listOf(
            Span(SpanStyle.ITALIC, 1, 2, FLAG),
            Span(SpanStyle.BOLD, 2, 3, FLAG),
            Span(SpanStyle.UNDERLINE, 3, 4, FLAG),
            Span(SpanStyle.BOLD_ITALIC, 4, 5, FLAG)
        )
        val newSpans = spans.splitAfter(0)
        assertThat(newSpans, iz(spans))
    }

    @Test
    fun should_splitAfter_keeps_empty_span_from_empty_paragraph() {
        val spans = listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))
        val newSpans = spans.splitAfter(0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
    }

    @Test
    fun should_splitAfter_keeps_empty_zero_index_span_when_splitting_from_zero_index() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 0, FLAG),
            Span(SpanStyle.UNDERLINE, 0, 3, FLAG)
        )
        val newSpans = spans.splitAfter(0)
        assertThat(newSpans, iz(spans))
    }

    @Test
    fun should_splitParagraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    Paragraph(content = Content("hello world", listOf(Span(SpanStyle.STRIKETHROUGH, 0, 11, FLAG)))),
                    Paragraph()
                )
            )
        )
        val newState = state.splitParagraph(state.document.blocks[1].asParagraph(), 6)
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[3], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("hello ", listOf(Span(SpanStyle.STRIKETHROUGH, 0, 6, FLAG)))))
        }
        with(newState.document.blocks[2].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("world", listOf(Span(SpanStyle.STRIKETHROUGH, 0, 5, FLAG)))))
        }
    }

    @Test
    fun should_splitParagraph_at_beginning() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    Paragraph(content = Content("hello world", listOf(Span(SpanStyle.UNDERLINE, 0, 11, FLAG)))),
                    Paragraph()
                )
            )
        )
        val newState = state.splitParagraph(state.document.blocks[1].asParagraph(), 0)
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[3], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("", listOf(Span(SpanStyle.UNDERLINE, 0, 0, FLAG)))))
        }
        with(newState.document.blocks[2].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("hello world", listOf(Span(SpanStyle.UNDERLINE, 0, 11, FLAG)))))
        }
    }

    @Test
    fun should_splitParagraph_at_end() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    Paragraph(content = Content("hello world", listOf(Span(SpanStyle.UNDERLINE, 0, 11, FLAG)))),
                    Paragraph()
                )
            )
        )
        val newState = state.splitParagraph(state.document.blocks[1].asParagraph(), 11)
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[3], iz(state.document.blocks[2]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("hello world", listOf(Span(SpanStyle.UNDERLINE, 0, 11, FLAG)))))
        }
        with(newState.document.blocks[2].asParagraph()) {
            assertThat(style, iz(state.document.blocks[1].asParagraph().style))
            assertThat(content, iz(Content("", listOf(Span(SpanStyle.UNDERLINE, 0, 0, FLAG)))))
        }
    }

    @Test
    fun should_splitParagraph_on_empty_paragraph() {
        val state = EditorState(Document(listOf(Paragraph())))
        val newState = state.splitParagraph(state.document.blocks[0].asParagraph(), 0)
        with(newState.document.blocks[0].asParagraph()) {
            assertThat(content, iz(Content()))
        }
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(content, iz(Content()))
        }
    }

    @Test
    fun should_splitParagraph_copies_paragraph_style() {
        val state = EditorState(Document(listOf(Paragraph(style = ParagraphStyle(unorderedList = true)))))
        val newState = state.splitParagraph(state.document.blocks[0].asParagraph(), 0)
        with(newState.document.blocks[0].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(style, iz(ParagraphStyle(unorderedList = true)))
        }
    }
}
