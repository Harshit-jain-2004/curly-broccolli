package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class MergeParagraphsTest {

    val FLAG = 0

    @Test
    fun should_shouldMergeSpanWith_merges_adjacent_spans_with_same_style() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 2)
        assertThat(shouldMerge, iz(true))
    }

    @Test
    fun should_shouldMergeSpanWith_merges_adjacent_spans_when_multiple_spans_present() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG), Span(SpanStyle.ITALIC, 1, 2, FLAG))
        val targetSpans = listOf(Span(SpanStyle.UNDERLINE, 1, 2, FLAG), Span(SpanStyle.BOLD, 2, 4, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 4)
        assertThat(shouldMerge, iz(true))
    }

    @Test
    fun should_shouldMergeSpanWith_doesnt_merge_if_source_empty() {
        val spansToMerge = emptyList<Span>()
        val targetSpans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 2)
        assertThat(shouldMerge, iz(false))
    }

    @Test
    fun should_shouldMergeSpanWith_doesnt_merge_if_target_empty() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = emptyList<Span>()
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 2)
        assertThat(shouldMerge, iz(false))
    }

    @Test
    fun should_shouldMergeSpanWith_doesnt_merge_if_first_span_not_at_beginning() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))
        val targetSpans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 2)
        assertThat(shouldMerge, iz(false))
    }

    @Test
    fun should_shouldMergeSpanWith_doesnt_merge_if_last_span_not_at_end() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 3)
        assertThat(shouldMerge, iz(false))
    }

    @Test
    fun should_shouldMergeSpanWith_doesnt_merge_if_style_is_different() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = listOf(Span(SpanStyle.ITALIC, 0, 2, FLAG))
        val shouldMerge = spansToMerge.shouldMergeSpanWith(targetSpans, 2)
        assertThat(shouldMerge, iz(false))
    }

    @Test
    fun should_shouldInto_merges_adjacent_spans_with_same_style() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))
        val merged = spansToMerge.mergeInto(targetSpans, 2)
        assertThat(merged, iz(listOf(Span(SpanStyle.BOLD, 0, 3, FLAG))))
    }

    @Test
    fun should_mergeInto_merges_adjacent_spans_when_multiple_spans_present() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG), Span(SpanStyle.ITALIC, 1, 2, FLAG))
        val targetSpans = listOf(Span(SpanStyle.UNDERLINE, 1, 2, FLAG), Span(SpanStyle.BOLD, 2, 4, FLAG))
        val merged = spansToMerge.mergeInto(targetSpans, 4)
        assertThat(
            merged,
            iz(
                listOf(
                    Span(SpanStyle.UNDERLINE, 1, 2, FLAG), Span(SpanStyle.BOLD, 2, 5, FLAG),
                    Span(SpanStyle.ITALIC, 5, 6, FLAG)
                )
            )
        )
    }

    @Test
    fun should_mergeInto_moves_spans_when_not_merging() {
        val spansToMerge = listOf(Span(SpanStyle.BOLD, 0, 1, FLAG))
        val targetSpans = listOf(Span(SpanStyle.ITALIC, 0, 2, FLAG))
        val merged = spansToMerge.mergeInto(targetSpans, 2)
        assertThat(merged, iz(listOf(Span(SpanStyle.ITALIC, 0, 2, FLAG), Span(SpanStyle.BOLD, 2, 3, FLAG))))
    }

    @Test
    fun should_mergeInto_combines_text_and_spans() {
        val paragraph1 = Paragraph(content = Content("one", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val paragraph2 = Paragraph(content = Content("two", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(
            merged.content,
            iz(
                Content(
                    "onetwo",
                    listOf(
                        Span(SpanStyle.BOLD, 1, 2, FLAG),
                        Span(SpanStyle.BOLD, 4, 5, FLAG)
                    )
                )
            )
        )
    }

    @Test
    fun should_mergeInto_with_empty_target_is_source() {
        val paragraph1 = Paragraph()
        val paragraph2 = Paragraph(content = Content("two", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(merged.content, iz(paragraph2.content))
    }

    @Test
    fun should_mergeInto_with_empty_source_is_target() {
        val paragraph1 = Paragraph(content = Content("one", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val paragraph2 = Paragraph()
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(merged.content, iz(paragraph1.content))
    }

    @Test
    fun should_mergeInto_with_empty_source_ignores_span() {
        val paragraph1 = Paragraph(content = Content("one", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val paragraph2 = Paragraph(content = Content("", listOf(Span(SpanStyle.ITALIC, 0, 0, FLAG))))
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(merged.content, iz(paragraph1.content))
    }

    @Test
    fun should_mergeInto_keeps_source_formatting_when_both_paragraphs_empty() {
        val paragraph1 = Paragraph(content = Content("", listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
        val paragraph2 = Paragraph(content = Content("", listOf(Span(SpanStyle.ITALIC, 0, 0, FLAG))))
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(merged.content, iz(paragraph2.content))
    }

    @Test
    fun should_mergeInto_keeps_target_paragraph_style() {
        val paragraph1 = Paragraph(style = ParagraphStyle(unorderedList = true))
        val paragraph2 = Paragraph(style = ParagraphStyle(unorderedList = false))
        val merged = paragraph2.mergeInto(paragraph1)
        assertThat(merged.style, iz(ParagraphStyle(unorderedList = true)))
    }

    @Test
    fun should_mergeParagraphIntoPrevious_with_two_paragraphs() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    Paragraph(content = Content("one")),
                    Paragraph(content = Content("two")),
                    Paragraph()
                )
            )
        )
        val newState = state.mergeParagraphIntoPrevious(state.document.blocks[2].asParagraph())
        assertThat(newState.document.blocks[0], iz(state.document.blocks[0]))
        assertThat(newState.document.blocks[2], iz(state.document.blocks[3]))
        with(newState.document.blocks[1].asParagraph()) {
            assertThat(content, iz(Content("onetwo")))
        }
    }

    @Test
    fun should_mergeParagraphIntoPrevious_when_previous_is_media() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    InlineMedia(),
                    Paragraph(content = Content("two")),
                    Paragraph()
                )
            )
        )
        val newState = state.mergeParagraphIntoPrevious(state.document.blocks[2].asParagraph())
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0], state.document.blocks[2],
                    state.document.blocks[3]
                )
            )
        )
    }
}
