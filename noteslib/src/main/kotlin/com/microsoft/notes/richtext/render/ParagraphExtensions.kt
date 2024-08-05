package com.microsoft.notes.richtext.render

import android.text.Spannable
import android.text.SpannableStringBuilder
import com.microsoft.notes.richtext.scheme.Paragraph

fun Paragraph.parse(): SpannableStringBuilder =
    if (style.unorderedList) {
        parseUnorderedList()
    } else {
        parseNormalStyledParagraph()
    }

fun Paragraph.parseUnorderedList(): SpannableStringBuilder {
    val contentWithParsedText = content.copy(text = content.parseText())
    val spannableStringBuilder = contentWithParsedText.parseSpan()
    spannableStringBuilder.setSpan(
        NotesBulletSpan(), 0, spannableStringBuilder.length,
        Spannable
            .SPAN_INCLUSIVE_EXCLUSIVE
    )
    return spannableStringBuilder
}

fun Paragraph.parseNormalStyledParagraph(): SpannableStringBuilder {
    val contentWithParsedText = content.copy(text = content.parseText())
    return contentWithParsedText.parseSpan()
}

fun Paragraph.size(): Int =
    content.text.length

fun Paragraph.cursorPlaces(): Int =
    size() + 1
