package com.microsoft.notes.richtext.render

import android.content.Context
import android.text.SpannableStringBuilder
import com.microsoft.notes.richtext.scheme.Block
import com.microsoft.notes.richtext.scheme.InlineMedia
import com.microsoft.notes.richtext.scheme.Paragraph

fun Block.parse(context: Context): SpannableStringBuilder =
    when (this) {
        is Paragraph -> this.parse()
        is InlineMedia -> this.parse(context)
    }

fun Block.size(): Int = when (this) {
    is Paragraph -> size()
    is InlineMedia -> size()
}

fun Block.cursorPlaces(): Int = when (this) {
    is Paragraph -> cursorPlaces()
    is InlineMedia -> cursorPlaces()
}
