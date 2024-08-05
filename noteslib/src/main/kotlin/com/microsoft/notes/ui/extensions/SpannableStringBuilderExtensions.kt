package com.microsoft.notes.ui.extensions

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.microsoft.notes.models.Color
import com.microsoft.notes.noteslib.NotesLibrary

/**
 * Highlights the occurrences of keywords in the SpannableString
 * Highlighting Logic:
 *    substring of the textBlock with smallest start index, matching any of the search keywords is
 *    highlighted first. Then, the next substring not overlapping with previous highlight is
 *    highlighted and so on...
 *    In case multiple keywords appear at same start position in textBlock, the keyword that appears
 *    first in the keyword list is highlighted.
 *
 * Highlighting Algorithm:
 *     Maintain an array: highlightSpanLengths,
 *         highlightSpanLengths[i] : length of keyword that appears in textBlock at position 'i',
 *         if multiple keywords start at 'i', then the one to appear first in list would be considered.
 *     List of keywords is traversed in reverse, for every keyword, all its occurrences in the textblock
 *     are found and highlightSpanLengths is updated.
 *     Finally, highlightSpanLengths is traversed and all its finite non-overlapping spans are
 *     highlighted.
 *         span defined by highlightedSpanLengths[i] = [i, i + highlightedSpanLengths[i] )
 */
fun SpannableStringBuilder.highlightKeywords(context: Context, keywordsToHighlight: List<String>, color: Color) {
    val foregroundHighlightColor = ContextCompat.getColor(context, color.toSearchHighlightForegroundColorResource())
    val backgroundHighlightColor = ContextCompat.getColor(context, color.toSearchHighlightBackgroundColorResource())

    highlightKeywords(keywordsToHighlight, foregroundHighlightColor, backgroundHighlightColor)
}

fun SpannableStringBuilder.getHighlightedText(context: Context, keywordsToHighlight: List<String>?): SpannableStringBuilder {
    if (keywordsToHighlight != null) {
        highlightKeywords(
            keywordsToHighlight,
            ContextCompat.getColor(context, NotesLibrary.getInstance().theme.searchHighlightForeground),
            ContextCompat.getColor(context, NotesLibrary.getInstance().theme.searchHighlightBackground)
        )
    }
    return this
}

fun SpannableStringBuilder.highlightKeywords(
    keywordsToHighlight: List<String>,
    foregroundHighlightColor: Int,
    backgroundHighlightColor: Int
) {
    val filteredKeywordsToHighlight = keywordsToHighlight.filter { !it.isBlank() }
    if (filteredKeywordsToHighlight.isEmpty()) {
        return
    }
    val highlightSpanLengths = IntArray(this.length) { 0 }

    for (keyword in filteredKeywordsToHighlight.asReversed()) {
        var startIndex = 0
        while (startIndex < this.length) {
            startIndex = this.indexOf(keyword, startIndex, true)
            if (startIndex == -1) {
                break
            }
            highlightSpanLengths[startIndex++] = keyword.length
        }
    }

    var curIndex = 0
    while (curIndex < highlightSpanLengths.size) {
        if (highlightSpanLengths[curIndex] == 0) {
            curIndex++
        } else {
            setSpan(
                ForegroundColorSpan(foregroundHighlightColor), curIndex,
                curIndex + highlightSpanLengths[curIndex], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                BackgroundColorSpan(backgroundHighlightColor), curIndex,
                curIndex + highlightSpanLengths[curIndex], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            curIndex += highlightSpanLengths[curIndex]
        }
    }
}
