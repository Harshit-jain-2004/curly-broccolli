package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span

fun EditorState.splitParagraph(paragraph: Paragraph, splitAt: Int): EditorState {
    val paraIndex = document.blocks.indexOf(paragraph)
    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, paraIndex) +
                paragraph.splitBefore(splitAt) +
                paragraph.splitAfter(splitAt) +
                document.blocks.subList(paraIndex + 1, document.blocks.size)
        )
    )
}

fun Paragraph.splitBefore(splitAt: Int): Paragraph =
    copy(
        content = Content(
            text = content.text.substring(0, splitAt),
            spans = content.spans.splitBefore(splitAt)
        )
    )

fun Paragraph.splitAfter(splitAt: Int): Paragraph =
    Paragraph(
        style = style.copy(),
        content = Content(
            text = content.text.substring(splitAt),
            spans = content.spans.splitAfter(splitAt)
        )
    )

fun List<Span>.splitBefore(splitAt: Int): List<Span> =
    filter { it.start < splitAt || it.start == 0 && splitAt == 0 }.map {
        if (it.end > splitAt) it.copy(end = splitAt) else it
    }

fun List<Span>.splitAfter(splitAt: Int): List<Span> {
    if (splitAt <= 0)
        return this
    val startingSpanIndex = indexOf(
        findLast {
            splitAt == it.end || (it.start < splitAt && splitAt < it.end)
        }
    )
    return if (startingSpanIndex >= 0)
        subList(startingSpanIndex, size)
            .map { it.copy(start = maxOf(0, it.start - splitAt), end = it.end - splitAt) }
    else emptyList()
}
