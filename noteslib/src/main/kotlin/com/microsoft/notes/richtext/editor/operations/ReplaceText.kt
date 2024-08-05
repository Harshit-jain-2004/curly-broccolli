package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span

fun EditorState.replaceText(paragraph: Paragraph, start: Int, end: Int, newText: String): EditorState {
    val paraIndex = document.blocks.indexOf(paragraph)

    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, paraIndex) +
                paragraph.replaceText(start, end, newText) +
                document.blocks.subList(paraIndex + 1, document.blocks.size)
        )
    )
}

fun Paragraph.replaceText(start: Int, end: Int, newText: String): Paragraph = when {
    start == end && newText.isEmpty() -> this
    start == 0 && end == 0 && content.text.isEmpty() -> insertIntoEmptyParagraph(newText)
    start == 0 && end == content.text.length && newText.isEmpty() -> deleteContent()
    else ->
        copy(
            content = Content(
                text = content.text.replaceRange(start, end, newText),
                spans = content.spans.adjustSpans(start, end, newText.length)
            )
        )
}

fun Paragraph.insertIntoEmptyParagraph(text: String): Paragraph =
    copy(
        content = Content(
            text = text,
            spans = content.spans.firstOrNull()?.let { listOf(it.copy(end = text.length)) } ?: emptyList()
        )
    )

fun Paragraph.deleteContent(): Paragraph =
    copy(
        content = Content(
            text = "",
            spans = content.spans.firstOrNull()?.takeIf { it.start == 0 }?.let { listOf(it.copy(end = 0)) }
                ?: emptyList()
        )
    )

// TODO: https://github.com/microsoft-notes/notes-android-sdk/issues/885
fun List<Span>.adjustSpans(rangeStart: Int, rangeEnd: Int, newLength: Int): List<Span> {
    val offset = rangeStart - rangeEnd + newLength

    // Empty spans at 0-length selections take precedence and have different adjusting rules
    if (rangeStart == rangeEnd) {
        val emptySpanAtRange = find { it.start == it.end && it.start == rangeStart }
        if (emptySpanAtRange != null) {
            val indexOfEmptySpanAtRange = indexOf(emptySpanAtRange)
            val spansBeforeRange = subList(0, indexOfEmptySpanAtRange)
            val spansAfterRange = subList(indexOfEmptySpanAtRange + 1, size)
            return spansBeforeRange +
                listOfNotNull(emptySpanAtRange).map { it.copy(end = it.end + newLength) } +
                spansAfterRange.map { it.copy(start = it.start + newLength, end = it.end + newLength) }
        }
    }

    return mapNotNull { span ->
        val newStart = when {
            span.start < rangeStart -> span.start
            span.start == rangeStart && (rangeStart != rangeEnd || span.start == 0) -> span.start
            span.start == rangeEnd && span.start == span.end && offset > 0 -> span.start
            span.start < rangeEnd && offset >= 0 -> span.start
            span.start < rangeEnd + offset && offset < 0 -> span.start
            span.start in rangeStart until rangeEnd -> rangeStart + newLength
            else -> span.start + offset
        }

        val newEnd = when {
            span.end < rangeStart -> span.end
            span.end == rangeStart && rangeStart != rangeEnd -> span.end
            span.end in rangeStart..rangeEnd -> rangeStart + newLength
            else -> span.end + offset
        }

        when (newStart) {
            newEnd -> null
            else -> span.copy(start = newStart, end = newEnd)
        }
    }.removeOverlaps()
}

private fun List<Span>.removeOverlaps(): List<Span> {
    var lastStart = Int.MAX_VALUE
    return reversed().mapNotNull { span ->
        val lastStartTemp = lastStart
        lastStart = span.start
        if (span.end > lastStartTemp) span.copy(end = lastStartTemp)
        else span
    }.reversed()
}
