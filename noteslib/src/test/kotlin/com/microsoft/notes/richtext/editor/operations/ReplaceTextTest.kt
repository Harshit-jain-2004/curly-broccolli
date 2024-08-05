package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class ReplaceTextTest {

    val FLAG = 0

    @Test
    fun should_adjustSpans_inserting_at_empty_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 3, 3, FLAG))
        val newSpans = spans.adjustSpans(3, 3, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 3, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_between_nonempty_and_empty_span() {
        val spans = listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 3, FLAG))
        val newSpans = spans.adjustSpans(3, 3, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_across_spans_same_length() {
        val spans = listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 6, FLAG))
        val newSpans = spans.adjustSpans(2, 4, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 6, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_across_spans_less_length() {
        val spans = listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 6, FLAG))
        val newSpans = spans.adjustSpans(2, 4, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.DEFAULT, 0, 2, FLAG), Span(SpanStyle.BOLD, 2, 4, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_across_spans_more_length() {
        val spans = listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 6, FLAG))
        val newSpans = spans.adjustSpans(2, 4, 4)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 3, 8, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_across_spans_more_length_with_gap() {
        val spans = listOf(Span(SpanStyle.DEFAULT, 0, 3, FLAG), Span(SpanStyle.BOLD, 4, 6, FLAG))
        val newSpans = spans.adjustSpans(2, 4, 4)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.DEFAULT, 0, 6, FLAG), Span(SpanStyle.BOLD, 6, 8, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_inside_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(2, 2, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 9, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_at_beginning_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 1, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 6, 9, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_at_beginning_with_across_spans() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 5, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_at_beginning_of_paragraph() {
        val spans = listOf(Span(SpanStyle.BOLD, 0, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 0, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 9, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_at_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(4, 4, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 9, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_before_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 0, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 6, 9, FLAG))))
    }

    @Test
    fun should_adjustSpans_inserting_after_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(5, 5, 5)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_inside_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(2, 3, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_at_beginning_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 3, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_at_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(2, 4, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_before_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 1, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 3, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_after_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(4, 5, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_overlaps_beginning_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 2, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_overlaps_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(3, 5, 0)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))))
    }

    @Test
    fun should_adjustSpans_deleting_overlaps_entire_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 5, 0)
        assertThat(newSpans, iz(emptyList()))
    }

    @Test
    fun should_adjustSpans_deleting_span_exactly() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 4, 0)
        assertThat(newSpans, iz(emptyList()))
    }

    @Test
    fun should_adjustSpans_replacing_inside_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(2, 3, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_at_beginning_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 2, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_at_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(3, 4, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_before_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 1, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 2, 5, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_after_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(4, 5, 2)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_overlaps_beginning_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 2, 1)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 3, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_overlaps_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(3, 5, 3)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 6, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_overlaps_entire_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 5, 1)
        assertThat(newSpans, iz(emptyList()))
    }

    @Test
    fun should_adjustSpans_replacing_span_exactly() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 4, 1)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_before_beginning_to_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(0, 4, 1)
        assertThat(newSpans, iz(emptyList()))
    }

    @Test
    fun should_adjustSpans_replacing_from_beginning_to_after_end_of_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 1, 4, FLAG))
        val newSpans = spans.adjustSpans(1, 5, 1)
        assertThat(newSpans, iz(listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
    }

    @Test
    fun should_adjustSpans_replacing_from_start_of_paragraph_with_empty_span() {
        val spans = listOf(Span(SpanStyle.BOLD, 0, 0, FLAG), Span(SpanStyle.UNDERLINE, 0, 3, FLAG))
        val newSpans = spans.adjustSpans(0, 0, 1)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 1, FLAG),
                    Span(SpanStyle.UNDERLINE, 1, 4, FLAG)
                )
            )
        )
    }

    @Test
    fun should_adjustSpans_replacing_from_between_nonempty_spans_with_empty_span() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, FLAG),
            Span(SpanStyle.ITALIC, 3, 3, FLAG),
            Span(SpanStyle.UNDERLINE, 3, 6, FLAG)
        )
        val newSpans = spans.adjustSpans(3, 3, 2)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, FLAG),
                    Span(SpanStyle.ITALIC, 3, 5, FLAG),
                    Span(SpanStyle.UNDERLINE, 5, 8, FLAG)
                )
            )
        )
    }

    @Test
    fun should_adjustSpans_replacing_from_between_nonempty_spans_with_empty_span_with_gaps() {
        val spans = listOf(
            Span(SpanStyle.BOLD, 0, 3, FLAG),
            Span(SpanStyle.ITALIC, 3, 3, FLAG),
            Span(SpanStyle.UNDERLINE, 5, 8, FLAG)
        )
        val newSpans = spans.adjustSpans(3, 3, 2)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 3, FLAG),
                    Span(SpanStyle.ITALIC, 3, 5, FLAG),
                    Span(SpanStyle.UNDERLINE, 7, 10, FLAG)
                )
            )
        )
    }

    @Test
    fun should_adjustSpans_append_character_using_word_replace_with_empty_span_at_end() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 6, FLAG),
            Span(SpanStyle.BOLD, 6, 6, FLAG)
        )
        val newSpans = spans.adjustSpans(0, 6, 7)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 6, FLAG),
                    Span(SpanStyle.BOLD, 6, 7, FLAG)
                )
            )
        )
    }

    @Test
    fun should_adjustSpans_append_character_using_word_replace_with_length_one_span_at_end() {
        val spans = listOf(
            Span(SpanStyle.DEFAULT, 0, 6, FLAG),
            Span(SpanStyle.BOLD, 6, 7, FLAG)
        )
        val newSpans = spans.adjustSpans(0, 7, 8)
        assertThat(
            newSpans,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 6, FLAG),
                    Span(SpanStyle.BOLD, 6, 8, FLAG)
                )
            )
        )
    }

    @Test
    fun should_replaceText_empty_range_with_empty_string_no_ops() {
        val para = Paragraph(content = Content("test", listOf(Span(SpanStyle.BOLD, 0, 4, FLAG))))
        val newPara = para.replaceText(2, 2, "")
        assertThat(newPara, iz(para))
    }

    @Test
    fun should_replaceText_inserting_into_empty_paragraph() {
        val para = Paragraph(content = Content(""))
        val newPara = para.replaceText(0, 0, "test")
        assertThat(newPara.content, iz(Content("test")))
    }

    @Test
    fun should_replaceText_inserting_into_empty_paragraph_with_formatting() {
        val para = Paragraph(content = Content("", listOf(Span(SpanStyle.BOLD, 0, 0, FLAG))))
        val newPara = para.replaceText(0, 0, "test")
        assertThat(newPara.content, iz(Content("test", listOf(Span(SpanStyle.BOLD, 0, 4, FLAG)))))
    }

    @Test
    fun should_replaceText_deleting_all_content() {
        val para = Paragraph(content = Content("test"))
        val newPara = para.replaceText(0, 4, "")
        assertThat(newPara.content, iz(Content()))
    }

    @Test
    fun should_replaceText_deleting_all_content_with_formatting() {
        val para = Paragraph(content = Content("test", listOf(Span(SpanStyle.BOLD, 0, 2, FLAG))))
        val newPara = para.replaceText(0, 4, "")
        assertThat(newPara.content, iz(Content("", listOf(Span(SpanStyle.BOLD, 0, 0, FLAG)))))
    }

    @Test
    fun should_replaceText_deleting_all_content_with_multiple_formatting() {
        val para = Paragraph(
            content = Content(
                "test",
                listOf(
                    Span(SpanStyle.BOLD, 0, 2, FLAG), Span(SpanStyle.ITALIC, 2, 4, FLAG)
                )
            )
        )
        val newPara = para.replaceText(0, 4, "")
        assertThat(newPara.content, iz(Content("", listOf(Span(SpanStyle.BOLD, 0, 0, FLAG)))))
    }

    @Test
    fun should_replaceText_deleting_all_content_with_formatting_not_at_start() {
        val para = Paragraph(content = Content("test", listOf(Span(SpanStyle.BOLD, 1, 2, FLAG))))
        val newPara = para.replaceText(0, 4, "")
        assertThat(newPara.content, iz(Content()))
    }

    @Test
    fun should_replaceText_on_specified_paragraph() {
        val state = EditorState(
            Document(
                listOf(
                    Paragraph(),
                    Paragraph(content = Content("test", listOf(Span(SpanStyle.BOLD, 0, 4, FLAG)))),
                    Paragraph()
                )
            )
        )
        val newState = state.replaceText(state.document.blocks[1].asParagraph(), 1, 2, "oa")
        assertThat(
            newState.document.blocks,
            iz(
                listOf(
                    state.document.blocks[0],
                    Paragraph(
                        localId = state.document.blocks[1].localId,
                        content = Content("toast", listOf(Span(SpanStyle.BOLD, 0, 5, FLAG)))
                    ),
                    state.document.blocks[2]
                )
            )
        )
    }
}
