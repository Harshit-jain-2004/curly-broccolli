package com.microsoft.notes.richtext.render

import android.graphics.Typeface
import android.text.ParcelableSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.microsoft.notes.richtext.scheme.SpanStyle

fun SpanStyle.toAndroidTextStyleSpans(): List<ParcelableSpan> {
    val spans = mutableListOf<ParcelableSpan>()
    when {
        bold && italic -> spans += StyleSpan(Typeface.BOLD_ITALIC)
        bold -> spans += StyleSpan(Typeface.BOLD)
        italic -> spans += StyleSpan(Typeface.ITALIC)
    }
    if (underline) spans += UnderlineSpan()
    if (strikethrough) spans += StrikethroughSpan()
    return spans
}
