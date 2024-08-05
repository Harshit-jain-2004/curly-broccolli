package com.microsoft.notes.ui.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R

fun Color.toSearchHighlightBackgroundColorResource() = when (this) {
    Color.CHARCOAL -> R.color.sn_search_highlight_background_dark
    else -> NotesLibrary.getInstance().theme.searchHighlightBackground
}

fun Color.toSearchHighlightForegroundColorResource() = when (this) {
    Color.CHARCOAL -> R.color.sn_search_highlight_foreground_dark
    else -> NotesLibrary.getInstance().theme.searchHighlightForeground
}

fun Color.toColorNameResource() = when (this) {
    Color.YELLOW -> R.string.sn_color_name_yellow
    Color.GREEN -> R.string.sn_color_name_green
    Color.PINK -> R.string.sn_color_name_pink
    Color.PURPLE -> R.string.sn_color_name_purple
    Color.BLUE -> R.string.sn_color_name_blue
    Color.GREY -> R.string.sn_color_name_grey
    Color.CHARCOAL -> R.string.sn_color_name_charcoal
}

fun Color.toImageCountBackgroundColorResource() = when (this) {
    Color.CHARCOAL -> R.color.sn_note_preview_image_tint_dark
    else -> R.color.sn_note_preview_image_tint_light
}

fun Color.toFeedImageCountBackgroundContextColor(context: Context) =
    ContextCompat.getColor(context, R.color.feed_sn_note_preview_image_tint_light)

fun Color.toImageCountBackgroundContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toImageCountBackgroundColorResource())
