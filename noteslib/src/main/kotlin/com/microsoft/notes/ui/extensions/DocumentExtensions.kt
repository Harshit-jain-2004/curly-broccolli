package com.microsoft.notes.ui.extensions

import android.content.Context
import android.text.SpannableStringBuilder
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.richtext.render.parse
import com.microsoft.notes.richtext.scheme.Document
import com.microsoft.notes.richtext.scheme.asParagraph
import com.microsoft.notes.richtext.scheme.isMedia
import com.microsoft.notes.richtext.scheme.isParagraph

private const val NOTE_PREVIEW_MAX_CHAR_COUNT = 300

private val NOTE_PREVIEW_MAX_PARAGRAPH_COUNT = if (NotesLibrary.getInstance().experimentFeatureFlags.stickyNotesCardImprovementsEnabled) 9 else 5

fun Document.asPreviewSpannable(context: Context): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    var paragraphCounter = 0
    for (block in blocks) {
        if (paragraphCounter <= NOTE_PREVIEW_MAX_PARAGRAPH_COUNT &&
            builder.length < NOTE_PREVIEW_MAX_CHAR_COUNT
        ) {
            if (!block.isMedia()) {
                builder.append(block.parse(context))
                paragraphCounter++
            }
        } else {
            break
        }
    }
    return if (builder.isNotEmpty() && builder[builder.length - 1] == '\n') {
        // Remove extra new line char at the end added while parsing block
        SpannableStringBuilder(builder, 0, builder.length - 1)
    } else {
        builder
    }
}

fun Document.asTextSpannable(context: Context): SpannableStringBuilder {
    var builder = SpannableStringBuilder()
    var isBulletList = false
    blocks.forEachIndexed { _, block ->
        builder.append(block.parse(context))
        if (block.isParagraph()) {
            isBulletList = isBulletList || block.asParagraph().style.unorderedList
        }
    }
    if (builder.toString() == "\n" && !isBulletList) builder = SpannableStringBuilder()
    return builder
}
