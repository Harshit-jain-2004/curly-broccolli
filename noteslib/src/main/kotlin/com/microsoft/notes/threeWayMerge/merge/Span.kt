package com.microsoft.notes.threeWayMerge.merge

import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.threeWayMerge.Diff
import com.microsoft.notes.threeWayMerge.Range
import com.microsoft.notes.threeWayMerge.SpanDeletion
import com.microsoft.notes.threeWayMerge.SpanInsertion

/**
 * Returns a [Set]<[Int]> with the indices that the current [List]<[Span]> contains.
 *
 * ie:
 *      current list of Span: listOf(Span(start = 0, end = 2, style = SpanStyle.BOLD_ITALIC, flag = 0),
 *                                   Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0),
 *                                   Span(start = 7, end = 9, style = SpanStyle.BOLD, flag = 0),
 *                                   Span(start = 11, end = 15, style = SpanStyle.UNDERLINE, flag = 0))
 *      result: setOf(0, 1, 5, 6, 7, 8, 11, 12, 13, 14))
 *
 * @receiver [List]<[Span]> with the current spans.
 * @return [Set]<[Int]> with the indices that are inside of the Spans.
 */
internal fun List<Span>.spanIndices(): Set<Int> {
    val indices = mutableListOf<Int>()
    this.forEachIndexed { _, span ->
        indices.addAll(Range(span.start, span.end).indices())
    }
    return indices.toSet()
}

/**
 * Decrement offset (start) by the number of characters deleted before offset in [previouslyDeletedIndices]
 *
 * ie:
 *      current Span: Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
 *      previouslyDeletedIndices: listOf(1, 2, 3, 7)
 *      result: Span(start = 2, end = 5, style = SpanStyle.BOLD, flag = 0)
 *      So before start(5) we have 1, 2 and 3, so we have to move back the start and end of our Span 3 positions
 *
 * @receiver [Span]
 * @param previouslyDeletedIndices that is a list of the indices that were deleted before
 * @result a new [Span] with its offset (start, and end) updated properly regarding the deleted indices that
 * happened before start index
 */
fun Span.spanModifiedByDeletes(previouslyDeletedIndices: List<Int>): Span {
    val newStart = newOffsetModifiedByDeletes(start, previouslyDeletedIndices)
    return copy(start = newStart, end = newStart + (end - start))
}

/**
 * Increment offset (start) by the number of characters inserted before offset in [previouslyInsertedIndices]
 *
 * ie:
 *      current Span: Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
 *      previouslyInsertedIndices: listOf(1, 2, 3, 7)
 *      result: Span(start = 8, end = 11, style = SpanStyle.BOLD, flag = 0)
 *      So before start(5) we have 1, 2 and 3, so we have to move forward the start and end of our Span 3 positions
 *
 * @receiver [Span]
 * @param previouslyInsertedIndices that is a list of the indices that were inserted before
 * @result a new [Span] with its offset (start, and end) updated properly regarding the inserted indices that
 * happened before start index
 */
fun Span.spanModifiedByInserts(previouslyInsertedIndices: List<Int>): Span {
    val newStart = newOffsetModifiedByInserts(start, previouslyInsertedIndices)
    return copy(start = newStart, end = newStart + (end - start))
}

/**
 * Remove any indexes in the [Span] range that already exist in one of the styles.
 * In practice, this means that spans in the primary document will override spans in the secondary document.
 *
 * ie:
 *      current Span: Span(start = 5, end = 8, style = SpanStyle.BOLD, flag = 0)
 *      existingIndices: setOf(1, 2, 3, 7) --> we create ranges: (1, 4) and (7, 8)
 *      result: Span(start = 5, end = 7, style = SpanStyle.BOLD, flag = 0)
 *      we have deleted the last part of our Span range, since we had one (7, 8) that already existed.
 *
 * @receiver [Span]
 * @param existingIndices a [Set]<[Int]> that contains existing indices
 * @result [Span] with the new start and end offset after applied the modification.
 */
fun Span.spanModifiedByExisting(existingIndices: Set<Int>): Span {
    if (start == end) {
        // 0-length spans can be ignored as they're already as short as possible (being 0-length)
        return this
    }
    val existingRanges = existingIndices.toRanges()
    var copy = Range(start, end)
    existingRanges.forEach { range ->
        copy = copy.remove(range)
    }
    return copy(start = copy.start, end = copy.end)
}

/**
 * Given a list of [diffs] and a [List]<[Span]> it returns a new [List]<[Span]> where spans found in the
 * diff as [SpanDeletion] diffs are deleted from the given list.
 *
 * ie:
 *      diffs:  mutableListOf<Diff>(
 *                  SpanDeletion(
 *                          blockId = "blockId",
 *                          span = Span(start = 1, end = 11, style = SpanStyle.BOLD_ITALIC, flag = 0)))
 *      spans: listOf(Span(start = 1, end = 11, style = SpanStyle.BOLD_ITALIC, flag = 0),
 *                    Span(start = 11, end = 13, style = SpanStyle.UNDERLINE, flag = 0))
 *      result: listOf(Span(start = 11, end = 13, style = SpanStyle.UNDERLINE, flag = 0)
 *      We have removed the first Span that was given by the [SpanDeletion] diff and was contained in the
 *      spans list.
 *
 * @param diffs can contain or not [SpanDeletion] diffs that will be used to delete [Span] in the [spans] list.
 * @param spans were will be deleting possible [SpanDeletion] diffs from [diffs]
 * @return a new [List]<[Span]> without [Span] that were pointed by the [diffs] list.
 */
fun applySpanDeletes(diffs: MutableList<Diff>, spans: List<Span>): List<Span> {
    val newSpans = spans.toMutableList()
    val diffIndicesToDelete = mutableListOf<Int>()
    diffs.forEachIndexed loop@{ i, diff ->
        if (diff !is SpanDeletion) return@loop

        newSpans.remove(diff.span)
        diffIndicesToDelete.add(i)
    }
    diffs.removeAt(diffIndicesToDelete)
    return newSpans
}

/**
 * Check if the current [Span] is valid or not in terms of how its offset (start and end) are.
 * The rule here is that end hast to be greater than or equal to start. On Android we support empty spans (for
 * now). Once we change out canvas design, we will not support 0 length spans.
 *
 * ie:
 *      current Span = Span(start = 1, end = 11, style = SpanStyle.BOLD_ITALIC, flag = 0)
 *      result: true --> is valid
 *
 *      current: Span(start = 5, end = 1, style = SpanStyle.BOLD_ITALIC, flag = 0)
 *      result: false --> is not valid
 *
 * @receiver [Span]
 * @return [Boolean] where true says the [Span] is valid, otherwise false.
 */
internal fun Span.isValid(): Boolean = end >= start

/**
 * Returns a new [List]<[Span]> given [SpanInsertion] diffs. The [Span]s' start and end can be modified too since
 * [previouslyDeletedIndices] and [previouslyInsertedIndices] could have indices that affect them.
 *
 * ie:
 *      diffs: listOf(SpanInsertion(
 *                          blockId = "blockId",
 *                          span = Span(start = 1, end = 3, style = SpanStyle.UNDERLINE, flag = 0)))
 *      spans: emptyList()
 *      previouslyDeletedIndices = emptyList()
 *      previouslyInsertedIndices = emptyList()
 *      result: listOf(Span(start = 1, end = 3, style = SpanStyle.UNDERLINE, flag = 0))
 *      We have inserted a Span given by [SpanInsertion] diff.
 *
 *      diffs: listOf(SpanInsertion(
 *                      blockId = "blockId",
 *                      span = Span(start = 5, end = 7, style = SpanStyle.UNDERLINE, flag = 0)))
 *      spans: emptyList()
 *      previouslyDeletedIndices = emptyList()
 *      previouslyInsertedIndices = listOf(1, 3, 4, 10, 11, 12)
 *      result: listOf(Span(start = 8, end = 10, style = SpanStyle.UNDERLINE, flag = 0))
 *      we have 3 indices (1, 3, 4) that are before our Span start so we have to increase its offset.
 *
 * @param diffs can contain [SpanInsertion] that will be the only ones that will processed by this function.
 * @param spans where we will apply [diffs]
 * @param previouslyDeletedIndices we can have indices that were deleted previously.
 * @param previouslyInsertedIndices we can have indices that were inserted previously.
 * @return [List]<[Span]> with the result of apply the previous parameters to [spans].
 */
fun applySpanInserts(
    diffs: MutableList<Diff>,
    spans: List<Span>,
    previouslyDeletedIndices: List<Int> = emptyList(),
    previouslyInsertedIndices: List<Int> = emptyList()
): List<Span> {
    val newSpans = spans.toMutableList()
    val diffIndicesToDelete = mutableListOf<Int>()
    var spanIndices = spans.spanIndices()

    diffs.forEachIndexed loop@{ i, diff ->
        if (diff !is SpanInsertion) return@loop

        var span = diff.span
        span = span.spanModifiedByDeletes(previouslyDeletedIndices)
        span = span.spanModifiedByInserts(previouslyInsertedIndices)
        span = span.spanModifiedByExisting(spanIndices)

        if (span.isValid()) {
            newSpans.add(span)
            spanIndices += Range(span.start, span.end).indices().toSet()
        }
        diffIndicesToDelete.add(i)
    }

    newSpans.sortWith(
        Comparator { a, b ->
            when {
                a.start > b.start -> 1
                b.start > a.start -> -1
                else -> when {
                    a.end > b.end -> 1
                    b.end > a.end -> -1
                    else -> 0
                }
            }
        }
    )

    diffs.removeAt(diffIndicesToDelete)
    return newSpans
}
