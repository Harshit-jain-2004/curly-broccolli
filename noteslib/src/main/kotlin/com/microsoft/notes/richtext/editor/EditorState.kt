package com.microsoft.notes.richtext.editor

import android.text.Spanned
import android.text.SpannedString
import com.microsoft.notes.richtext.editor.operations.fillStyleGaps
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Range
import com.microsoft.notes.richtext.scheme.SpanStyle
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.getSpannedFromHtml
import com.microsoft.notes.richtext.scheme.isParagraph

data class EditorState(
    val document: Document = Document(listOf(Paragraph())),
    val spannedText: Spanned = SpannedString(""),
    val renderFlags: Int = RenderFlags.NONE,
    val trackedSpans: Set<Any> = emptySet(),
    val readOnlyMode: Boolean = false
) {
    val needsRender: Boolean
        get() = renderFlags has RenderFlags.RENDER_NEEDED
}

fun reduceEditorState(document: Document, currentEditorState: EditorState): EditorState =
    if (document.isSamsungNoteDocument) {
        currentEditorState.copy(
            document = document,
            spannedText = getSpannedFromHtml(document.body).trim() as Spanned,
            readOnlyMode = document.readOnly
        )
    } else {
        currentEditorState.copy(
            document = document.addParagraphIfEmpty(),
            readOnlyMode = document.readOnly
        )
    }

fun Document.addParagraphIfEmpty(): Document =
    if (blocks.isEmpty()) copy(blocks = listOf(Paragraph())) else this

fun EditorState.updateRange(range: Range): EditorState =
    copy(document = document.copy(range = range))

fun EditorState.forceRender(flags: Int = 0): EditorState =
    copy(renderFlags = renderFlags or RenderFlags.RENDER_NEEDED or flags)

fun EditorState.renderCompleted(): EditorState =
    copy(renderFlags = RenderFlags.NONE)

fun EditorState.trackSpans(spans: Set<Any>): EditorState =
    copy(trackedSpans = spans)

fun EditorState.isParagraph(range: Range = this.document.range): Boolean {
    if (document.blocks.isEmpty()) {
        return false
    }
    val block = document.blocks[range.startBlock]
    return when (block) {
        is Paragraph -> true
        is InlineMedia -> false
    }
}

fun EditorState.getFormatting(range: Range = this.document.range): SpanStyle {
    val block = document.blocks.getOrNull(range.startBlock)
    if (block == null || !block.isParagraph()) {
        return SpanStyle.DEFAULT
    }
    val spans = block.asParagraph().content.spans.fillStyleGaps(range.startOffset)
    if (range.isCollapsed) {
        val emptySpanAtCollapsedRange = spans.find { it.start == it.end && it.start == range.startOffset }
        if (emptySpanAtCollapsedRange != null) {
            return emptySpanAtCollapsedRange.style
        }
        val spanAtCollapsedRangeStart = spans.find { range.startOffset in it.start..it.end }
        return spanAtCollapsedRangeStart?.style ?: SpanStyle.DEFAULT
    }

    val spanAtRangeStart = spans.reversed().find { range.startOffset in it.start until it.end }
    return spanAtRangeStart?.style ?: SpanStyle.DEFAULT
}

fun EditorState.getParagraphFormatting(range: Range = this.document.range): ParagraphStyle =
    if (isParagraph(range)) {
        document.blocks[range.startBlock].asParagraph().style
    } else {
        ParagraphStyle()
    }

enum class FormattingProperty {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH
}

enum class ParagraphFormattingProperty {
    BULLETS
}

object RenderFlags {
    const val NONE = 0
    const val RENDER_NEEDED = 1
    const val DELAY_RENDER = 2
    const val SET_SELECTION = 4
}

infix fun Int.has(other: Int): Boolean = (this and other) != 0
