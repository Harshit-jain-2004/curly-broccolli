package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.SpanDeletion
import com.microsoft.notes.threeWayMerge.SpanInsertion
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as iz

class SpanTest {

    @Test
    fun should_get_span_indices_from_a_given_list_of_spans() {
        val spans = listOf(
            Span(start = 0, end = 2, style = SpanStyle.BOLD_ITALIC, flag = 0),
            Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0),
            Span(start = 7, end = 9, style = SpanStyle.BOLD, flag = 0),
            Span(start = 11, end = 15, style = SpanStyle.UNDERLINE, flag = 0)
        )
        val result = spans.spanIndices()
        assertThat(result, iz(setOf(0, 1, 5, 6, 7, 8, 11, 12, 13, 14)))
    }

    @Test
    fun should_get_new_span_given_previously_deleted_indices() {
        val span = Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
        val previouslyDeletedIndices = listOf(1, 2, 3, 7)
        val result = span.spanModifiedByDeletes(previouslyDeletedIndices)
        assertThat(result, iz(Span(start = 2, end = 5, style = SpanStyle.BOLD, flag = 0)))
    }

    @Test
    fun should_get_new_span_given_previously_inserted_indices() {
        val span = Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
        val previouslyInsertedIndices = listOf(1, 2, 3, 7)
        val result = span.spanModifiedByInserts(previouslyInsertedIndices)
        assertThat(result, iz(Span(start = 8, end = 11, style = SpanStyle.BOLD, flag = 0)))
    }

    @Test
    fun should_get_new_span_given_existing_indices() {
        val span = Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
        val existingIndices = setOf(1, 2, 3, 7)
        val result = span.spanModifiedByExisting(existingIndices)
        assertThat(result, iz(Span(start = 5, end = 7, style = SpanStyle.BOLD, flag = 0)))
    }

    @Test
    fun should_not_modify_empty_span_given_existing_indices_overlap() {
        val span = Span(start = 5, end = 5, style = SpanStyle.BOLD, flag = 0)
        val existingIndices = setOf(4, 5, 6)
        val result = span.spanModifiedByExisting(existingIndices)
        assertThat(result, iz(span))
    }

    @Test
    fun should_not_modify_empty_span_given_existing_indices_do_not_overlap() {
        val span = Span(start = 5, end = 5, style = SpanStyle.BOLD, flag = 0)
        val existingIndices = setOf(6, 7, 8)
        val result = span.spanModifiedByExisting(existingIndices)
        assertThat(result, iz(span))
    }

    @Test
    fun should_not_modify_empty_span_given_no_existing_indices() {
        val span = Span(start = 5, end = 5, style = SpanStyle.BOLD, flag = 0)
        val existingIndices = emptySet<Int>()
        val result = span.spanModifiedByExisting(existingIndices)
        assertThat(result, iz(span))
    }

    @Test
    fun should_apply_span_deletes() {
        val SPAN_1 = Span(start = 1, end = 11, style = SpanStyle.BOLD_ITALIC, flag = 0)
        val SPAN_2 = Span(start = 11, end = 13, style = SpanStyle.UNDERLINE, flag = 0)

        val diff = SpanDeletion(blockId = "blockId", span = SPAN_1)
        val diffs = mutableListOf<Diff>(diff)
        val result = applySpanDeletes(diffs, mutableListOf(SPAN_1, SPAN_2))

        // Should remove the style
        assertThat(result, iz(listOf(SPAN_2)))
        // Should remove the processed diff from the diffs array
        assertThat(diffs.isEmpty(), iz(true))
    }

    @Test
    fun is_span_valid() {
        val SPAN_1 = Span(start = 1, end = 11, style = SpanStyle.BOLD_ITALIC, flag = 0)
        val result = SPAN_1.isValid()

        assertThat(result, iz(true))
    }

    @Test
    fun should_apply_span_inserts() {
        val SPAN_1 = Span(start = 1, end = 3, style = SpanStyle.UNDERLINE, flag = 0)

        val diff = SpanInsertion(blockId = "blockId", span = SPAN_1)
        val diffs = mutableListOf<Diff>(diff)
        val result = applySpanInserts(diffs = diffs, spans = mutableListOf())

        // Should insert the span
        assertThat(result.size, iz(1))
        assertThat(result.component1(), iz(SPAN_1))

        // Should remove the processed diff from the diffs array
        assertThat(diffs.isEmpty(), iz(true))
    }

    @Test
    fun should_increment_the_offset_by_the_number_of_chars_inserted() {
        val SPAN_1 = Span(start = 5, end = 7, style = SpanStyle.UNDERLINE, flag = 0)
        val insertedIndices = listOf(1, 3, 4, 10, 11, 12)
        val diff = SpanInsertion(blockId = "blockId", span = SPAN_1)
        val diffs = mutableListOf<Diff>(diff)

        val result = applySpanInserts(diffs, spans = mutableListOf(), previouslyInsertedIndices = insertedIndices)
        assertThat(result.component1().start, iz(8))
        assertThat(result.component1().end, iz(10))
    }

    @Test
    fun should_modify_the_intersecting_style_to_not_intersect() {
        val EXISTING_SPAN = Span(start = 10, end = 20, style = SpanStyle.ITALIC, flag = 0)
        val NEW_SPAN = Span(start = 5, end = 15, style = SpanStyle.BOLD, flag = 0)
        val diff = SpanInsertion(span = NEW_SPAN, blockId = "blockId")
        val diffs = mutableListOf<Diff>(diff)
        val spans = mutableListOf(EXISTING_SPAN)

        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(result.size, iz(2))
        assertThat(result.component2(), iz(EXISTING_SPAN))
        assertThat(result.component1().start, iz(5))
        assertThat(result.component1().end, iz(10))
        assertThat(result.component1().style, iz(SpanStyle.BOLD))
    }

    @Test
    fun should_simply_insert_empty_span_insertion() {
        val diffs = mutableListOf<Diff>(
            SpanInsertion(span = Span(SpanStyle.BOLD, 5, 5, 0), blockId = "blockId")
        )
        val spans = mutableListOf(
            Span(SpanStyle.DEFAULT, 0, 5, 0)
        )
        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(
            result,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 5, 0),
                    Span(SpanStyle.BOLD, 5, 5, 0)
                )
            )
        )
    }

    @Test
    fun should_simply_insert_empty_span_insertion_between_spans() {
        val diffs = mutableListOf<Diff>(
            SpanInsertion(span = Span(SpanStyle.BOLD, 5, 5, 0), blockId = "blockId")
        )
        val spans = mutableListOf(
            Span(SpanStyle.DEFAULT, 0, 5, 0),
            Span(SpanStyle.ITALIC, 5, 10, 0)
        )
        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(
            result,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 5, 0),
                    Span(SpanStyle.BOLD, 5, 5, 0),
                    Span(SpanStyle.ITALIC, 5, 10, 0)
                )
            )
        )
    }

    @Test
    fun should_simply_insert_empty_span_insertion_at_start_of_block() {
        val diffs = mutableListOf<Diff>(
            SpanInsertion(span = Span(SpanStyle.BOLD, 0, 0, 0), blockId = "blockId")
        )
        val spans = mutableListOf(
            Span(SpanStyle.DEFAULT, 0, 5, 0),
            Span(SpanStyle.ITALIC, 5, 10, 0)
        )
        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(
            result,
            iz(
                listOf(
                    Span(SpanStyle.BOLD, 0, 0, 0),
                    Span(SpanStyle.DEFAULT, 0, 5, 0),
                    Span(SpanStyle.ITALIC, 5, 10, 0)
                )
            )
        )
    }

    @Test
    fun should_simply_insert_empty_span_insertion_after_deletion_of_empty_span() {
        val diffs = mutableListOf<Diff>(
            SpanInsertion(span = Span(SpanStyle(bold = true, italic = true), 5, 5, 0), blockId = "blockId")
        )
        val spans = mutableListOf(
            Span(SpanStyle.DEFAULT, 0, 5, 0)
        )
        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(
            result,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 5, 0),
                    Span(SpanStyle(bold = true, italic = true), 5, 5, 0)
                )
            )
        )
    }

    @Test
    fun should_handle_multiple_insertions() {
        val diffs = mutableListOf<Diff>(
            SpanInsertion(span = Span(SpanStyle(bold = true, italic = true), 5, 10, 0), blockId = "blockId"),
            SpanInsertion(span = Span(SpanStyle.BOLD, 10, 15, 0), blockId = "blockId")
        )
        val spans = mutableListOf(
            Span(SpanStyle.DEFAULT, 0, 5, 0)
        )
        val result = applySpanInserts(diffs = diffs, spans = spans)
        assertThat(
            result,
            iz(
                listOf(
                    Span(SpanStyle.DEFAULT, 0, 5, 0),
                    Span(SpanStyle(bold = true, italic = true), 5, 10, 0),
                    Span(SpanStyle.BOLD, 10, 15, 0)
                )
            )
        )
    }
}
