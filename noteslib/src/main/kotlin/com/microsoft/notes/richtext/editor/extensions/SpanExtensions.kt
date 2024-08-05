package com.microsoft.notes.richtext.editor.extensions

import com.microsoft.notes.richtext.scheme.Span

fun List<Span>.moveSpans(offset: Int): List<Span> = map { it.move(offset) }

fun Span.move(offset: Int): Span =
    copy(start = start + offset, end = end + offset)

fun List<Span>.getSpanAt(position: Int): Span? =
    reversed().firstOrNull {
        (it.start < position || (it.start == position && (position == 0 || it.start == it.end))) && position <= it.end
    }
