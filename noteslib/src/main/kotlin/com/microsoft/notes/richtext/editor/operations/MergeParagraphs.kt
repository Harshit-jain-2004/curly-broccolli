package com.microsoft.notes.richtext.editor.operations

import com.microsoft.notes.richtext.editor.EditorState
import com.microsoft.notes.richtext.editor.extensions.moveSpans
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.Span

fun EditorState.mergeParagraphIntoPrevious(paragraphLocalId: String): EditorState =
    mergeParagraphIntoPrevious(document.blocks.indexOfFirst { it.localId == paragraphLocalId })

fun EditorState.mergeParagraphIntoPrevious(paragraph: Paragraph): EditorState =
    mergeParagraphIntoPrevious(document.blocks.indexOf(paragraph))

fun EditorState.mergeParagraphIntoPrevious(paraIndex: Int): EditorState {
    val paragraph = document.blocks[paraIndex] as Paragraph
    val prevBlock = document.blocks[paraIndex - 1]
    return copy(
        document = document.copy(
            blocks = document.blocks.subList(0, paraIndex - 1) +
                when (prevBlock) {
                    is Paragraph -> paragraph.mergeInto(prevBlock)
                    is InlineMedia -> paragraph
                } + document.blocks.subList(paraIndex + 1, document.blocks.size)
        )
    )
}

fun Paragraph.mergeInto(targetParagraph: Paragraph): Paragraph =
    targetParagraph.copy(content = content.mergeInto(targetParagraph.content))

fun Content.mergeInto(targetContent: Content): Content = when {
    targetContent.text.isEmpty() -> this
    text.isEmpty() -> targetContent
    else -> Content(
        text = targetContent.text + this.text,
        spans = spans.mergeInto(targetContent.spans, targetContent.text.length)
    )
}

fun List<Span>.mergeInto(target: List<Span>, targetLength: Int): List<Span> = when {
    shouldMergeSpanWith(target, targetLength) -> target.dropLast(1) +
        target.last().copy(end = targetLength + first().end) +
        drop(1).moveSpans(targetLength)
    else -> target + moveSpans(targetLength)
}

fun List<Span>.shouldMergeSpanWith(target: List<Span>, targetLength: Int): Boolean =
    isNotEmpty() && target.isNotEmpty() && first().start == 0 && target.last().end == targetLength &&
        first().style == target.last().style
