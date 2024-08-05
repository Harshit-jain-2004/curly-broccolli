package com.microsoft.notes.ui.extensions

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.R

/**
 * Set the given {@link content} after truncation into the {@link textView} when is not null or not empty, and set
 * this view to
 * {@link VISIBLE}, otherwise the view visibility will set to {@link GONE}
 */
fun TextView.setPreviewContentAndVisibility(content: SpannableStringBuilder?) {
    if (content != null && content.isNotEmpty()) {
        this.text = content
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}

/**
 * Sets the as font color a contrast value of the given {@link color} passed as a paramether
 *
 */
fun TextView.setContrastColor(color: Color) {
    val fontColor = if (color == Color.CHARCOAL) {
        ContextCompat.getColor(context, R.color.sn_font_light)
    } else {
        ContextCompat.getColor(context, R.color.feed_item_ui_refresh_text_color_light)
    }
    this.setTextColor(fontColor)
}

fun TextView.setDateTimeContrastColor(color: Color) {
    val fontColor = if (color == Color.CHARCOAL) {
        ContextCompat.getColor(context, R.color.sn_date_time_dark)
    } else {
        ContextCompat.getColor(context, R.color.sn_date_time_light)
    }
    this.setTextColor(fontColor)
}

fun TextView.setDateTimeContrastColorForFeedUIRefresh(color: Color) {
    val fontColor = if (color == Color.CHARCOAL) {
        ContextCompat.getColor(context, R.color.sn_date_time_dark)
    } else {
        ContextCompat.getColor(context, R.color.note_reference_timestamp_color_light)
    }
    this.setTextColor(fontColor)
}

fun View?.show() {
    this?.visibility = View.VISIBLE
}

fun View?.hide() {
    this?.visibility = View.GONE
}
