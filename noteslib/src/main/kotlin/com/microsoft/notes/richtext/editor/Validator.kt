package com.microsoft.notes.richtext.editor

import android.graphics.Typeface
import android.text.ParcelableSpan
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.microsoft.notes.richtext.render.NotesBulletSpan
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.NEW_LINE_CHAR
import com.microsoft.notes.richtext.scheme.Paragraph
import com.microsoft.notes.richtext.scheme.ParagraphStyle
import com.microsoft.notes.richtext.scheme.Span
import com.microsoft.notes.richtext.scheme.SpanStyle
import java.util.SortedMap

private const val TAG = "NotesEditText"

fun validate(spannable: SpannableStringBuilder, editorState: EditorState): Boolean {
    if (spannable.isEmpty() || spannable.last() != NEW_LINE_CHAR) {
        return false
    }

    val document = editorState.document
    val convertedDocument = spannable.toDocument(editorState.trackedSpans)

    val matches = document contentEquals convertedDocument
    return matches
}

infix fun Document.contentEquals(other: Document): Boolean =
    blocks.size == other.blocks.size &&
        (0 until blocks.size).all {
            blocks[it] contentEquals other.blocks[it]
        }

infix fun Block.contentEquals(other: Block): Boolean =
    when (this) {
        is Paragraph -> other is Paragraph && this contentEquals other
        is InlineMedia -> other is InlineMedia && this contentEquals other
    }

// We don't have any way to extract the URL from ImageSpan
@Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant")
infix fun InlineMedia.contentEquals(other: InlineMedia): Boolean = true

infix fun Paragraph.contentEquals(other: Paragraph): Boolean =
    style == other.style && content contentEquals other.content

infix fun Content.contentEquals(other: Content): Boolean =
    text == other.text && spans.size == other.spans.size &&
        (0 until spans.size).all {
            spans[it] contentEquals other.spans[it]
        }

infix fun Span.contentEquals(other: Span): Boolean =
    start == other.start && end == other.end && style == other.style

fun SpannableStringBuilder.toDocument(trackedSpans: Set<Any>): Document {
    val lines = toString().substring(0, length - 1).lines()
    var currentOffset = 0
    return Document(
        blocks = lines.map {
            val start = currentOffset
            val end = start + it.length
            currentOffset = end + 1
            val bulletSpans = getSpans(start, end + 1, NotesBulletSpan::class.java).filterIn(trackedSpans)
            val imageSpans = getSpans(start, end, ImageSpan::class.java).filterIn(trackedSpans)

            when {
                imageSpans.isNotEmpty() -> InlineMedia()
                else -> Paragraph(
                    style = ParagraphStyle(unorderedList = (bulletSpans.isNotEmpty())),
                    content = Content(it, createStyleSpans(start, end, trackedSpans))
                )
            }
        }
    )
}

fun SpannableStringBuilder.createStyleSpans(start: Int, end: Int, trackedSpans: Set<Any>): List<Span> {
    val startIncludingPrevNewline = if (start > 0) start - 1 else start
    val endIncludingNewline = if (end == length) end else end + 1
    val styleSpans = getSpans(startIncludingPrevNewline, endIncludingNewline, StyleSpan::class.java)
        .filterIn(trackedSpans)
    val underlineSpans = getSpans(startIncludingPrevNewline, endIncludingNewline, UnderlineSpan::class.java)
        .filterIn(trackedSpans)
    val strikethroughSpans = getSpans(startIncludingPrevNewline, endIncludingNewline, StrikethroughSpan::class.java)
        .filterIn(trackedSpans)
    val deltas = styleSpans.toDeltas(this, start, end) merge underlineSpans.toDeltas(
        this, start,
        end
    ) merge strikethroughSpans.toDeltas(this, start, end)
    return deltas.toSpans(end == start)
}

infix fun SortedMap<Int, List<FormattingDelta>>.merge(other: SortedMap<Int, List<FormattingDelta>>): SortedMap<Int, List<FormattingDelta>> {
    val result = toMutableMap()
    other.forEach { (key, value) ->
        val current = result[key]
        result[key] = when (current) {
            null -> value
            else -> current + value
        }
    }
    return result.toSortedMap()
}

fun <T : ParcelableSpan> List<T>.toDeltas(spannable: SpannableStringBuilder, start: Int, end: Int): SortedMap<Int, List<FormattingDelta>> {
    val deltas = mutableMapOf<Int, List<FormattingDelta>>()
    this.forEach {
        val style = it.toStyle()
        val spanStart = maxOf(spannable.getSpanStart(it) - start, 0)
        val spanEnd = minOf(spannable.getSpanEnd(it) - start, end - start)
        if (spanStart != spanEnd || start == end) {
            val startDeltas = deltas[spanStart]
            deltas[spanStart] = when (startDeltas) {
                null -> SpanStyle.DEFAULT.diff(style)
                else -> startDeltas.merge(SpanStyle.DEFAULT.diff(style))
            }
            if (spanEnd > spanStart || spanStart > start) {
                val endDeltas = deltas[spanEnd]
                deltas[spanEnd] = when (endDeltas) {
                    null -> style.diff(SpanStyle.DEFAULT)
                    else -> endDeltas.merge(style.diff(SpanStyle.DEFAULT))
                }
            }
        }
    }
    return deltas.filterValues { it.isNotEmpty() }.toSortedMap()
}

fun List<FormattingDelta>.merge(other: List<FormattingDelta>): List<FormattingDelta> {
    val deltas = toMutableList()
    other.forEach { delta ->
        val currentDelta = deltas.firstOrNull { it.formattingProperty == delta.formattingProperty }
        when {
            currentDelta == null -> deltas.add(delta)
            currentDelta.value != delta.value -> deltas.remove(currentDelta)
        }
    }
    return deltas.toList()
}

fun SpanStyle.diff(other: SpanStyle): List<FormattingDelta> {
    val deltas = mutableListOf<FormattingDelta>()
    if (bold != other.bold) deltas.add(FormattingDelta(FormattingProperty.BOLD, other.bold))
    if (italic != other.italic) deltas.add(FormattingDelta(FormattingProperty.ITALIC, other.italic))
    if (underline != other.underline) deltas.add(FormattingDelta(FormattingProperty.UNDERLINE, other.underline))
    if (strikethrough != other.strikethrough) deltas.add(
        FormattingDelta(
            FormattingProperty.STRIKETHROUGH,
            other
                .strikethrough
        )
    )
    return deltas.toList()
}

fun ParcelableSpan.toStyle(): SpanStyle = when (this) {
    is StyleSpan -> when (style) {
        Typeface.BOLD -> SpanStyle.BOLD
        Typeface.BOLD_ITALIC -> SpanStyle.BOLD_ITALIC
        Typeface.ITALIC -> SpanStyle.ITALIC
        else -> SpanStyle.DEFAULT
    }
    is UnderlineSpan -> SpanStyle.UNDERLINE
    is StrikethroughSpan -> SpanStyle.STRIKETHROUGH
    else -> SpanStyle.DEFAULT
}

fun SortedMap<Int, List<FormattingDelta>>.toSpans(isEmpty: Boolean): List<Span> {
    if (isEmpty) {
        return this[0]?.let {
            listOf(Span(SpanStyle.DEFAULT.applyDiff(it), 0, 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE))
        }
            ?: emptyList()
    }

    var currentStyle = SpanStyle.DEFAULT
    var currentIndex = 0
    return mapNotNull { (index, deltas) ->
        val oldStyle = currentStyle
        val oldIndex = currentIndex
        currentStyle = currentStyle.applyDiff(deltas)
        currentIndex = index
        if (index == oldIndex || oldStyle == SpanStyle.DEFAULT)
            null
        else
            Span(oldStyle, oldIndex, index, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun SpanStyle.applyDiff(deltas: List<FormattingDelta>): SpanStyle = copy(
    bold = deltas.firstOrNull { it.formattingProperty == FormattingProperty.BOLD }?.value ?: bold,
    italic = deltas.firstOrNull { it.formattingProperty == FormattingProperty.ITALIC }?.value ?: italic,
    underline = deltas.firstOrNull { it.formattingProperty == FormattingProperty.UNDERLINE }?.value
        ?: underline,
    strikethrough = deltas.firstOrNull { it.formattingProperty == FormattingProperty.STRIKETHROUGH }?.value
        ?: strikethrough
)

data class FormattingDelta(val formattingProperty: FormattingProperty, val value: Boolean)

fun <T : Any> Array<out T>.filterIn(set: Set<Any>): List<T> = filter { set.contains(it) }
