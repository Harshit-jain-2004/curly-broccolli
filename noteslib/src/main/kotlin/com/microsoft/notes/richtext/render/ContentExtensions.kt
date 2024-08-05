package com.microsoft.notes.richtext.render

import android.text.SpannableStringBuilder
import com.microsoft.notes.richtext.scheme.Content
import com.microsoft.notes.richtext.scheme.NEW_LINE_CHAR_AS_STR

fun Content.parseSpan(): SpannableStringBuilder {
    val spanBuilder = SpannableStringBuilder(text)
    spans.forEach { spanBuilder.setSpan(it) }
    return spanBuilder
}

fun Content.parseText(): String =
    text + NEW_LINE_CHAR_AS_STR
