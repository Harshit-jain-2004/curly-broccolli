package com.microsoft.notes.richtext.render

import android.text.SpannableStringBuilder
import android.text.Spanned
import com.microsoft.notes.richtext.scheme.Span

fun SpannableStringBuilder.setSpan(span: Span, offset: Int = 0) =
    with(span) {
        style.toAndroidTextStyleSpans().forEach {
            setSpan(
                it, start + offset, end + offset,
                toAndroidTextSpannableFlag(start == 0)
            )
        }
    }

internal fun toAndroidTextSpannableFlag(isAtBeginning: Boolean): Int =
    if (isAtBeginning) Spanned.SPAN_INCLUSIVE_INCLUSIVE else Spanned.SPAN_EXCLUSIVE_INCLUSIVE
