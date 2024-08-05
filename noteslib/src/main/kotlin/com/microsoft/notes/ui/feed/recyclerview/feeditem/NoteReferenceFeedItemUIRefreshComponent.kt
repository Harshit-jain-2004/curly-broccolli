package com.microsoft.notes.ui.feed.recyclerview.feeditem

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.microsoft.notes.noteslib.R

class NoteReferenceFeedItemUIRefreshComponent(context: Context, attrs: AttributeSet) :
    NoteReferenceFeedItemComponent(context, attrs) {
    override fun setSourceIcon() {
        (noteSource?.layoutParams as MarginLayoutParams).topMargin = context.resources.getDimensionPixelSize(R.dimen.note_reference_source_top_margin)
        if (themeOverride != null) {
            noteSourceIcon?.setColorFilter(ContextCompat.getColor(context, R.color.sn_primary_color_dark))
        } else {
            noteSourceIcon?.setColorFilter(ContextCompat.getColor(context, R.color.sn_primary_color))
        }
    }
}
