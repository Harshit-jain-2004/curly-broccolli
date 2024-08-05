package com.microsoft.notes.richtext.editor.operations

import android.text.Spannable
import android.util.Log
import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.FormattingProperty
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asMedia
import com.microsoft.notes.richtext.scheme.asParagraph

fun EditorState.changeFormatting(formattingProperty: FormattingProperty, value: Boolean): EditorState {
    var currentState = this
    (document.range.startBlock..document.range.endBlock).forEach { paraIndex ->
        val para = currentState.document.blocks[paraIndex]
        if (para is Paragraph) {
            currentState = currentState.changeFormatting(
                para,
                if (paraIndex == document.range.startBlock) document.range.startOffset else 0,
                if (paraIndex == document.range.endBlock) document.range.endOffset else para.content.text.length,
                formattingProperty,
                value
            )
        }
    }
    return currentState
}

fun EditorState.changeFormatting(
    paragraph: Paragraph,
    start: Int,
    end: Int,
    property: FormattingProperty,
    value: Boolean
): EditorState {
    val paraIndex = document.blocks.indexOf(paragraph)

    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, paraIndex) +
                paragraph.changeFormatting(start, end, property, value) +
                document.blocks.subList(paraIndex + 1, document.blocks.size)
        )
    )
}

fun Paragraph.changeFormatting(start: Int, end: Int, property: FormattingProperty, value: Boolean): Paragraph =
    copy(content = content.copy(spans = content.spans.changeFormatting(start, end, property, value)))

/**
 * Given a selection, and an incoming style change for that selection, returns the resulting span list describing
 * the resulting styles for the block. This is done in a few steps:
 *
 * 1) Filling in gaps in the styles up to the end of the selection, such that all unstyled text correspond to a
 * default span
 * 2) Partition the resulting spans into (a) spans that are part of the selection (b) 'before' (c) 'after'
 * 3) Given spans that are part of the selection, split them such that for any span that contains either start or
 * end, it is split in two at that point. The resulting span list has spans that are either entirely in the
 * selection or aren't
 * 4) Given the span list produced in the previous step, spans in the selection have the new style applied
 * 5) Combine the resulting list with the 'before' and 'after' lists, and normalize (e.g., compression)
 *
 * @param start start index of the selection
 * @param end end index of the selection (which can be equal to start if the selection is 0-length)
 * @param property the style property to apply the value to (e.g., bold)
 * @param value the value to apply to the style property
 * @return the resulting span list after the style has been applied
 */
fun List<Span>.changeFormatting(start: Int, end: Int, property: FormattingProperty, value: Boolean): List<Span> {
    val (before, withinSelection, after) = fillStyleGaps(end).partitionGivenSelection(start, end)
    val withinSelectionSplit = withinSelection.splitBySelection(start, end)
    val withinSelectionWithNewFormatting = withinSelectionSplit.changeFormattingAssumingSplit(start, end, property, value)
    return (before + withinSelectionWithNewFormatting + after).normalize()
}

internal fun List<Span>.splitBySelection(start: Int, end: Int) =
    map { it.splitBySelection(start, end) }.flatten()

internal fun Span.splitBySelection(start: Int, end: Int): List<Span> {
    val endPartitioned = splitByIndex(end)
    if (start == end) {
        return endPartitioned
    }
    val startPartitioned = endPartitioned[0].splitByIndex(start)
    return startPartitioned + endPartitioned.subList(1, endPartitioned.size)
}

internal fun Span.splitByIndex(index: Int): List<Span> =
    if (index in (start + 1) until end) {
        listOf(copy(end = index), copy(start = index))
    } else {
        listOf(this)
    }

internal fun List<Span>.changeFormattingAssumingSplit(start: Int, end: Int, property: FormattingProperty, value: Boolean): List<Span> {
    if (start == end) {
        return changeFormattingAssumingSplit(start, property, value)
    }
    return map {
        if (start <= it.start && it.end <= end)
            Span(it.style.setProperty(property, value), it.start, it.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else
            it
    }
}

internal fun List<Span>.changeFormattingAssumingSplit(index: Int, property: FormattingProperty, value: Boolean): List<Span> {
    val existingEmptySpan = find { it.start == it.end && it.start == index }
    if (existingEmptySpan != null) {
        return map {
            if (it == existingEmptySpan)
                Span(it.style.setProperty(property, value), index, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                it
        }
    }

    var baseForNewSpan = find { it.start == index || it.end == index }
    if (baseForNewSpan == null) {
        // This should never happen given splitBySelection is called first, meaning there must be a span where
        // either its start or end is equal to the 0-length selection index, but we handle this case anyway
        Log.d("changeFormatting", "changeFormattingAssumingSplit: unable to find base for new span in the input list")
        baseForNewSpan = Span(SpanStyle.DEFAULT, index, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    val partitioned = partition { it.end <= index }
    return partitioned.first +
        Span(baseForNewSpan.style.setProperty(property, value), index, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) +
        partitioned.second
}

internal fun List<Span>.fillStyleGaps(end: Int): List<Span> {
    var currentStart = 0
    val newSpans = flatMap {
        val result = when {
            currentStart < it.start -> listOf(
                Span(
                    SpanStyle(),
                    currentStart, it.start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                ),
                it
            )
            else -> listOf(it)
        }
        currentStart = it.end
        result
    }
    return when {
        currentStart < end || (currentStart == end && newSpans.isEmpty() /* we don't have any spans and end is 0 */) -> newSpans + Span(
            SpanStyle(), currentStart, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        else -> newSpans
    }
}

internal fun List<Span>.partitionGivenSelection(start: Int, end: Int): Triple<List<Span>, List<Span>, List<Span>> {
    val before = mutableListOf<Span>()
    val after = mutableListOf<Span>()
    val withinSelection = mutableListOf<Span>()
    forEach {
        when {
            it.end < start -> before.add(it)
            end < it.start -> after.add(it)
            else -> withinSelection.add(it)
        }
    }
    return Triple(before.toList(), withinSelection.toList(), after.toList())
}

fun List<Span>.normalize(): List<Span> = removeConsecutiveZeroLengthStyles().mergeEqualAdjacentSpans()

internal fun List<Span>.removeConsecutiveZeroLengthStyles(): List<Span> {
    val ret = ArrayList<Span>()
    forEachIndexed { index, span ->
        run {
            if (span.start != span.end || (index == size - 1 || get(index + 1).start != get(index + 1).end)) {
                ret.add(span.copy())
            }
        }
    }
    return ret
}

internal fun List<Span>.mergeEqualAdjacentSpans(): List<Span> =
    fold(mutableListOf<Span>()) { acc, cur ->
        mergeSpanIntoMutableList(acc, cur)
        acc
    }.toList()

private fun mergeSpanIntoMutableList(list: MutableList<Span>, span: Span) {
    val prev = list.lastOrNull()
    if (prev != null &&
        (prev.start != prev.end || prev.start == 0) &&
        (prev.style == span.style && prev.flag == span.flag) &&
        (prev.start <= span.start && span.start <= prev.end)
    ) {
        list.removeAt(list.size - 1)
        list.add(prev.copy(end = span.end))
        return
    }
    list.add(span)
}

// TODO: Call this when going to view mode from edit mode
fun EditorState.removeEmptySpans(): EditorState {
    return copy(
        document = document.copy(
            blocks = document.blocks.map { block ->
                when (block) {
                    is InlineMedia -> block.asMedia()
                    is Paragraph -> with(block.asParagraph()) {
                        copy(content = content.copy(spans = content.spans.filter { span -> span.start != span.end }))
                    }
                }
            }
        )
    )
}

fun SpanStyle.setProperty(property: FormattingProperty, value: Boolean): SpanStyle =
    when (property) {
        FormattingProperty.BOLD -> copy(bold = value)
        FormattingProperty.ITALIC -> copy(italic = value)
        FormattingProperty.UNDERLINE -> copy(underline = value)
        FormattingProperty.STRIKETHROUGH -> copy(strikethrough = value)
    }
