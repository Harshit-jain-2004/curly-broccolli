package com.microsoft.notes.richtext.render

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TtsSpan
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.richtext.scheme.InlineMedia

const val NEW_LINE_CHAR_WITH_SPACE = " \n"
const val IMAGE_SPAN_START = 0
const val IMAGE_SPAN_END = 1

@Suppress("FunctionOnlyReturningConstant")
fun InlineMedia.size(): Int = 1

fun InlineMedia.cursorPlaces(): Int =
    size() + 1

@SuppressLint("NewApi")
fun InlineMedia.parse(context: Context): SpannableStringBuilder {
    val spanBuilder = SpannableStringBuilder(NEW_LINE_CHAR_WITH_SPACE)
    val imageSpan = PendingImageSpan(this)
    spanBuilder.setSpan(imageSpan, IMAGE_SPAN_START, IMAGE_SPAN_END, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val ttsSpan = TtsSpan.TextBuilder().setText(context.getString(R.string.sn_tts_image_span_text)).build()
    spanBuilder.setSpan(ttsSpan, IMAGE_SPAN_START, IMAGE_SPAN_END, Spannable.SPAN_COMPOSING)
    return spanBuilder
}

fun InlineMedia.localOrRemoteUrl(): String? =
    localUrl ?: remoteUrl
