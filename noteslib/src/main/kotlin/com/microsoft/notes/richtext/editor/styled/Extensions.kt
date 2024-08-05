@file:Suppress("TooManyFunctions")
package com.microsoft.notes.richtext.editor.styled

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.microsoft.notes.models.Color
import com.microsoft.notes.models.FontColor
import com.microsoft.notes.noteslib.NotesThemeOverride
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.editor.extensions.toDarkColorResource
import com.microsoft.notes.richtext.editor.extensions.toDarkHighLightColorResource
import com.microsoft.notes.richtext.editor.extensions.toDarkTextHandleColorResource
import com.microsoft.notes.richtext.editor.extensions.toIconColorNightResource
import com.microsoft.notes.richtext.editor.extensions.toInkColorNightResource
import com.microsoft.notes.richtext.editor.extensions.toInkColorResource
import com.microsoft.notes.richtext.editor.extensions.toLightHighLightColorResource
import com.microsoft.notes.richtext.editor.extensions.toLightTextHandleColorResource
import com.microsoft.notes.richtext.editor.extensions.toLighterColorResource
import com.microsoft.notes.richtext.editor.extensions.toLinkTextColorResource
import com.microsoft.notes.richtext.editor.extensions.toMediumColorResource
import com.microsoft.notes.richtext.editor.extensions.toSNCardColorNightResource
import com.microsoft.notes.richtext.editor.extensions.toSNIconColorResource
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun Context.parseMillisToRFC1123String(
    date: Long,
    timezone: TimeZone = TimeZone.getDefault()
): String {
    val dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
    val timeFormat = DateFormat.getTimeFormat(this)
    dateFormat.timeZone = timezone
    timeFormat.timeZone = timezone

    return try {
        dateFormat.format(date) + ", " + timeFormat.format(date)
    } catch (e: IllegalArgumentException) {
        val currentTime = System.currentTimeMillis()
        dateFormat.format(currentTime) + ", " + timeFormat.format(currentTime)
    }
}

fun Context.parseMillisToShortDateFormat(
    date: Long,
    timezone: TimeZone = TimeZone.getDefault()
): String {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val timeFormat = DateFormat.getTimeFormat(this)
    dateFormat.timeZone = timezone
    timeFormat.timeZone = timezone

    return try {
        val isToday = DateUtils.isToday(date)
        if (isToday) {
            timeFormat.format(date)
        } else {
            dateFormat.format(date)
        }
    } catch (e: IllegalArgumentException) {
        val currentTime = System.currentTimeMillis()
        timeFormat.format(currentTime)
    }
}

fun Color.toMediumContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toMediumColorResource())

fun Color.toLighterContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toLighterColorResource())

fun Color.toTopBarContextColor(context: Context, themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null): Int =
    if (themeOverride != null) {
        ContextCompat.getColor(context, this.toIconColorNightResource())
    } else {
        this.toMediumContextColor(context)
    }

fun Color.toCardColorContextColor(context: Context, themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null): Int =
    if (themeOverride != null) {
        ContextCompat.getColor(context, this.toSNCardColorNightResource())
    } else {
        this.toLighterContextColor(context)
    }

fun Color.toIconColorContextColor(context: Context, themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null): Int =
    if (themeOverride != null) {
        ContextCompat.getColor(context, this.toIconColorNightResource())
    } else {
        ContextCompat.getColor(context, this.toSNIconColorResource())
    }

fun Color.toDarkContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toDarkColorResource())

fun Color.toDarkTextHighLightContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toDarkHighLightColorResource())

fun Color.toLightTextHighLightContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toLightHighLightColorResource())

fun Color.toDarkTextHandleContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toDarkTextHandleColorResource())

fun Color.toLightTextHandleContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toLightTextHandleColorResource())

fun Color.toInkContextColor(context: Context, themeOverride: NotesThemeOverride.StickyNoteCanvasThemeOverride? = null): Int =
    if (themeOverride != null) {
        ContextCompat.getColor(context, this.toInkColorNightResource())
    } else {
        ContextCompat.getColor(context, this.toInkColorResource())
    }

fun FontColor.toContextColor(context: Context): Int =
    when (this) {
        FontColor.LIGHT -> ContextCompat.getColor(context, R.color.sn_font_light)
        else -> ContextCompat.getColor(context, R.color.sn_font_dark)
    }

fun FontColor.toSecondaryTextColor(context: Context): Int =
    ContextCompat.getColor(
        context,
        when (this) {
            FontColor.LIGHT -> R.color.secondary_text_color_light
            else -> R.color.secondary_text_color_dark
        }
    )

fun Color.toDividerContextColor(context: Context): Int =
    when (this) {
        Color.CHARCOAL -> ContextCompat.getColor(context, R.color.sn_timestamp_divider_color_dark)
        else -> ContextCompat.getColor(context, R.color.sn_timestamp_divider_color_light)
    }

fun Color.toMetadataColor(context: Context): Int =
    when (this) {
        Color.CHARCOAL -> ContextCompat.getColor(context, R.color.sn_metadata_color_charcoal)
        else -> ContextCompat.getColor(context, R.color.sn_metadata_color_light)
    }

fun Color.toTimestampDrawable(): Int =
    when (this) {
        Color.CHARCOAL -> R.drawable.sn_timestamp_button_bg_dark
        else -> R.drawable.sn_timestamp_button_bg_light
    }

fun Color.toLinkTextContextColor(context: Context): Int =
    ContextCompat.getColor(context, this.toLinkTextColorResource())

fun ImageView.loadRoundedImageFromUri(uri: String?, color: Int? = null, centerCrop: Boolean = true) {
    // https://github.com/bumptech/glide/issues/803
    context.let {
        if (it is Activity && it.isDestroyed) {
            return
        }
    }

    clearColorFilter()

    val roundedImageViewTarget = object : BitmapImageViewTarget(this) {
        override fun setResource(resource: Bitmap?) {
            val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, resource)
            circularBitmapDrawable.cornerRadius = context.resources.getDimension(R.dimen.sn_image_corner_radius)
            setImageDrawable(circularBitmapDrawable)
            color?.let { setColorFilter(color) }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            // Doing nothing will catch the exception and show the placeholder
        }
    }
    var opts = RequestOptions()
        .fallback(R.drawable.sn_notes_canvas_image_placeholder)
        .placeholder(R.drawable.sn_notes_canvas_image_placeholder)

    if (centerCrop) {
        opts = opts.centerCrop()
    }

    Glide.with(context)
        .asBitmap()
        .apply(opts)
        .load(uri)
        .into(roundedImageViewTarget)
}
